package io.jenkins.tools.pluginmodernizer.core.visitors;

import io.jenkins.tools.pluginmodernizer.core.extractor.ArchetypeCommonFile;
import io.jenkins.tools.pluginmodernizer.core.recipes.AddProperty;
import io.jenkins.tools.pluginmodernizer.core.recipes.EnableCD;
import java.util.Optional;
import java.util.regex.Pattern;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.maven.MavenIsoVisitor;
import org.openrewrite.text.PlainText;
import org.openrewrite.xml.ChangeTagValueVisitor;
import org.openrewrite.xml.tree.Xml;
import org.openrewrite.yaml.tree.Yaml;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Visitor to modify POM and configuration files for CD (JEP-229) support.
 * Transforms version structure, updates Maven config, and handles workflow files.
 */
public class EnableCDVisitor extends TreeVisitor<Tree, ExecutionContext> {

    private static final Logger LOG = LoggerFactory.getLogger(EnableCDVisitor.class);
    private static final Pattern VERSION_PATTERN = Pattern.compile("^(\\d+(?:\\.\\d+)*)(.*|$)");

    private final EnableCD.ConfigState state;

    public EnableCDVisitor(EnableCD.ConfigState state) {
        this.state = state;
    }

    @Override
    public Tree visit(Tree tree, ExecutionContext ctx) {
        // Handle POM transformations
        if (tree instanceof Xml.Document) {
            Xml.Document doc = (Xml.Document) tree;
            if (doc.getSourcePath().endsWith("pom.xml")) {
                return new PomVisitor(state).visitNonNull(tree, ctx);
            }
        }

        // Handle maven.config updates
        if (tree instanceof PlainText) {
            PlainText plainText = (PlainText) tree;
            if (ArchetypeCommonFile.MAVEN_CONFIG.same(plainText.getSourcePath())) {
                return updateMavenConfig(plainText);
            }
        }

        // Handle dependabot.yml updates
        if (tree instanceof Yaml.Documents) {
            Yaml.Documents yamlDocs = (Yaml.Documents) tree;
            if (ArchetypeCommonFile.DEPENDABOT.same(yamlDocs.getSourcePath())) {
                return updateDependabot(yamlDocs, ctx);
            }
            // Handle release-drafter.yml updates
            if (ArchetypeCommonFile.RELEASE_DRAFTER.same(yamlDocs.getSourcePath())) {
                return updateReleaseDrafter(yamlDocs, ctx);
            }
        }

        // Delete release drafter workflow if it exists
        if (tree instanceof org.openrewrite.SourceFile) {
            org.openrewrite.SourceFile sourceFile = (org.openrewrite.SourceFile) tree;
            if (ArchetypeCommonFile.RELEASE_DRAFTER_WORKFLOW.same(sourceFile.getSourcePath())
                    && state.isReleaseDrafterWorkflowExists()) {
                LOG.info("Deleting release-drafter workflow file");
                return null;
            }
        }

        return tree;
    }

    /**
     * Update maven.config to add CD format
     */
    private PlainText updateMavenConfig(PlainText mavenConfig) {
        // Don't update if POM already uses CD format and workflow exists
        if (state.isPomAlreadyUsesCDFormat() && state.isCdWorkflowExists()) {
            LOG.debug("POM already uses CD format and workflow exists. Skipping maven.config update.");
            return mavenConfig;
        }

        String content = mavenConfig.getText();

        // Check if changelist.format is already present
        if (content.contains("-Dchangelist.format")) {
            LOG.debug("maven.config already contains changelist.format, skipping update");
            return mavenConfig;
        }

        // Add the changelist.format line
        // If content ends with newline, just append; otherwise add newline first
        String updatedContent;
        if (content.endsWith("\n")) {
            updatedContent = content + "-Dchangelist.format=%d.v%s";
        } else {
            updatedContent = content + "\n-Dchangelist.format=%d.v%s";
        }
        LOG.debug("Adding -Dchangelist.format=%d.v%s to maven.config");

        return mavenConfig.withText(updatedContent);
    }

    /**
     * Update dependabot.yml to add github-actions ecosystem if not present
     */
    private Tree updateDependabot(Yaml.Documents dependabot, ExecutionContext ctx) {
        String content = dependabot.printAll();

        // Check if github-actions is already configured
        if (content.contains("package-ecosystem: github-actions")
                || content.contains("package-ecosystem: \"github-actions\"")) {
            LOG.debug("dependabot.yml already contains github-actions, skipping update");
            return dependabot;
        }

        LOG.debug("Adding github-actions section to dependabot.yml");

        // Append the github-actions template to the content
        String updatedContent = content.trim() + "\n" + EnableCD.DEPENDABOT_UPDATE_TEMPLATE.trim();

        // Convert to PlainText instead of re-parsing as YAML to ensure the change is recognized
        return PlainText.builder()
                .sourcePath(dependabot.getSourcePath())
                .text(updatedContent)
                .build();
    }

    /**
     * Update release-drafter.yml to remove custom templates (for CD compatibility)
     */
    private Tree updateReleaseDrafter(Yaml.Documents releaseDrafter, ExecutionContext ctx) {
        String content = releaseDrafter.printAll();

        // Check if templates are present
        if (!content.contains("name-template:")
                && !content.contains("tag-template:")
                && !content.contains("version-template:")) {
            LOG.debug("release-drafter.yml already cleaned, skipping update");
            return releaseDrafter;
        }

        LOG.debug("Removing custom templates from release-drafter.yml for CD compatibility");

        // Remove the template lines - handle both Unix (\n) and Windows (\r\n) line endings
        String updatedContent = content.replaceAll("(?m)^\\s*name-template:.*\\R?", "")
                .replaceAll("(?m)^\\s*tag-template:.*\\R?", "")
                .replaceAll("(?m)^\\s*version-template:.*\\R?", "");

        // Convert to PlainText to ensure the change is recognized
        return PlainText.builder()
                .sourcePath(releaseDrafter.getSourcePath())
                .text(updatedContent)
                .build();
    }

    /**
     * Inner visitor for POM transformations
     */
    private static class PomVisitor extends MavenIsoVisitor<ExecutionContext> {

        private final EnableCD.ConfigState state;

        public PomVisitor(EnableCD.ConfigState state) {
            this.state = state;
        }

        @Override
        public Xml.Document visitDocument(Xml.Document document, ExecutionContext ctx) {
            document = super.visitDocument(document, ctx);

            Xml.Tag root = document.getRoot();

            // Check if properties section exists
            Optional<Xml.Tag> propertiesTag = root.getChild("properties");
            if (propertiesTag.isEmpty()) {
                LOG.warn("POM lacks a properties section. Cannot add CD properties. Skipping transformation.");
                state.setPomHasProperties(false);
                return document;
            }

            state.setPomHasProperties(true);

            // Check if already using CD format
            Optional<Xml.Tag> versionTag = root.getChild("version");
            if (versionTag.isPresent()) {
                String currentVersion = versionTag.get().getValue().orElse("");

                // If already using ${changelist} format, skip
                if (currentVersion.equals("${changelist}")) {
                    // Check if changelist property already has the correct value
                    Optional<Xml.Tag> changelistProp = propertiesTag.get().getChildren("changelist").stream()
                            .findFirst();
                    if (changelistProp.isPresent()) {
                        String changelistValue = changelistProp.get().getValue().orElse("");
                        if (changelistValue.equals("999999-SNAPSHOT")) {
                            LOG.info("POM already uses correct CD version format. Skipping transformation.");
                            state.setPomAlreadyUsesCDFormat(true);
                            return document;
                        }
                    }
                }

                // Always use just ${changelist} format for CD (JEP-229)
                LOG.debug("Transforming version from {} to ${{changelist}}", currentVersion);
                document = (Xml.Document)
                        new ChangeTagValueVisitor<>(versionTag.get(), "${changelist}").visitNonNull(document, ctx);

                // Remove revision property if it exists
                Optional<Xml.Tag> revisionProperty =
                        propertiesTag.get().getChildren("revision").stream().findFirst();
                if (revisionProperty.isPresent()) {
                    LOG.debug("Removing revision property from POM");
                    doAfterVisit(new org.openrewrite.xml.RemoveContentVisitor<>(revisionProperty.get(), true, true));
                }

                // Update changelist property to 999999-SNAPSHOT
                Optional<Xml.Tag> existingChangelist =
                        propertiesTag.get().getChildren("changelist").stream().findFirst();
                if (existingChangelist.isPresent()) {
                    LOG.debug("Updating existing changelist property to 999999-SNAPSHOT");
                    document = (Xml.Document) new ChangeTagValueVisitor<>(existingChangelist.get(), "999999-SNAPSHOT")
                            .visitNonNull(document, ctx);
                } else {
                    LOG.debug("Adding changelist property with value 999999-SNAPSHOT");
                    document = (Xml.Document) new AddProperty("changelist", "999999-SNAPSHOT")
                            .getVisitor()
                            .visitNonNull(document, ctx);
                }
            }

            return document;
        }
    }
}
