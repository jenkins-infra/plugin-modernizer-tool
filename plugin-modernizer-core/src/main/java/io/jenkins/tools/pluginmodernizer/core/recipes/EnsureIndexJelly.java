package io.jenkins.tools.pluginmodernizer.core.recipes;

import io.jenkins.tools.pluginmodernizer.core.extractor.ArchetypeCommonFile;
import io.jenkins.tools.pluginmodernizer.core.extractor.PluginMetadata;
import io.jenkins.tools.pluginmodernizer.core.extractor.PomResolutionVisitor;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.intellij.lang.annotations.Language;
import org.openrewrite.Cursor;
import org.openrewrite.ExecutionContext;
import org.openrewrite.ScanningRecipe;
import org.openrewrite.SourceFile;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.maven.MavenIsoVisitor;
import org.openrewrite.text.PlainText;
import org.openrewrite.xml.tree.Xml;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Ensure `index.jelly` exists
 */
public class EnsureIndexJelly extends ScanningRecipe<EnsureIndexJelly.ShouldCreate> {

    /**
     * Jelly file
     */
    @Language("xml")
    public static final String JELLY_FILE =
            """
        <?jelly escape-by-default='true'?>
        <div>
           DESCRIPTION
        </div>
        """;

    /**
     * LOGGER.
     */
    private static final Logger LOG = LoggerFactory.getLogger(EnsureIndexJelly.class);

    @Override
    public String getDisplayName() {
        return "Create `index.jelly` if it doesn't exist";
    }

    @Override
    public String getDescription() {
        return "Jenkins tooling [requires](https://github.com/jenkinsci/maven-hpi-plugin/pull/302) "
                + "`src/main/resources/index.jelly` exists with a description.";
    }

    @Override
    public ShouldCreate getInitialValue(ExecutionContext ctx) {
        return new ShouldCreate();
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(ShouldCreate shouldCreate) {
        PluginMetadata metadata = new PluginMetadata();
        return new TreeVisitor<>() {
            @Override
            public Tree visit(Tree tree, ExecutionContext ctx) {
                SourceFile sourceFile = (SourceFile) tree;
                // We visit a jelly
                if (sourceFile.getSourcePath().endsWith(ArchetypeCommonFile.INDEX_JELLY.getPath())) {
                    LOG.info("Found 1 index.jelly a Will not replace it");
                    shouldCreate.jelliesPath.add(sourceFile.getSourcePath());
                    return tree;
                }
                // We visit a pom
                if (sourceFile.getSourcePath().endsWith(ArchetypeCommonFile.POM.getPath())) {
                    new PomResolutionVisitor().reduce(sourceFile, metadata);
                    if (metadata.getJenkinsVersion() == null) {
                        LOG.info("Skipping pom {} as it is not a Jenkins plugin", sourceFile.getSourcePath());
                        return tree;
                    }
                    Path jellyPath = sourceFile
                            .getSourcePath()
                            .resolve("..")
                            .resolve(ArchetypeCommonFile.INDEX_JELLY.getPath())
                            .normalize();
                    Xml.Document pom = (Xml.Document) sourceFile;
                    DescriptionVisitor descriptionVisitor = new DescriptionVisitor();
                    descriptionVisitor.visitNonNull(pom, ctx);
                    if (!descriptionVisitor.description.isEmpty()) {
                        shouldCreate.plugins.put(jellyPath, descriptionVisitor.description);
                    } else if (!descriptionVisitor.artifactId.isEmpty()) {
                        shouldCreate.plugins.put(jellyPath, descriptionVisitor.artifactId);
                    }
                    LOG.debug(shouldCreate.toString());
                }
                return tree;
            }
        };
    }

    /**
     * Visitor to extract the metadata from the POM file.
     */
    private static class DescriptionVisitor extends MavenIsoVisitor<ExecutionContext> {
        private String artifactId = "";
        private String description = "";

        @Override
        public Xml.Tag visitTag(Xml.Tag tag, ExecutionContext ctx) {
            Cursor parent = getCursor().getParentOrThrow();
            if (!(parent.getValue() instanceof Xml.Tag)) {
                return super.visitTag(tag, ctx);
            }
            Xml.Tag parentTag = parent.getValue();
            if (!parentTag.getName().equals("project")) {
                return super.visitTag(tag, ctx);
            }
            if ("description".equals(tag.getName())) {
                description = tag.getValue().orElse("");
            } else if ("artifactId".equals(tag.getName()) && !isManagedDependencyTag() && !isDependencyTag()) {
                artifactId =
                        tag.getValue().orElseThrow(() -> new IllegalStateException("Expected to find an artifact id"));
            }
            return super.visitTag(tag, ctx);
        }
    }

    /**
     * Accumulator to know if the file should be created or not and with which description.
     */
    public static class ShouldCreate {
        private Map<Path, String> plugins = new HashMap<>();
        private List<Path> jelliesPath = new LinkedList<>();
    }

    @Override
    public Collection<SourceFile> generate(ShouldCreate shouldCreate, ExecutionContext ctx) {
        if (shouldCreate.plugins.isEmpty()) {
            return Collections.emptyList();
        }
        List<SourceFile> generated = new LinkedList<>();
        for (Map.Entry<Path, String> plugin : shouldCreate.plugins.entrySet()) {
            if (shouldCreate.jelliesPath.contains(plugin.getKey())) {
                continue;
            }
            LOG.info("Creating index.jelly at " + plugin.getKey() + " with description " + plugin.getValue());
            generated.add(PlainText.builder()
                    .sourcePath(plugin.getKey())
                    .text(JELLY_FILE.replace("DESCRIPTION", plugin.getValue()))
                    .build());
        }
        return generated;
    }
}
