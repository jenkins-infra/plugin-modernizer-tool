package io.jenkins.tools.pluginmodernizer.core.recipes;

import io.jenkins.tools.pluginmodernizer.core.extractor.ArchetypeCommonFile;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.openrewrite.ExecutionContext;
import org.openrewrite.ScanningRecipe;
import org.openrewrite.SourceFile;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.maven.MavenIsoVisitor;
import org.openrewrite.xml.tree.Content;
import org.openrewrite.xml.tree.Xml;

/**
 * Ensure the parent pom has a relativePath set to disable local resolution.
 */
public class EnsureRelativePath extends ScanningRecipe<StringBuilder> {
    @Override
    public String getDisplayName() {
        return "Ensure the parent pom has a relativePath set to disable local resolution";
    }

    @Override
    public StringBuilder getInitialValue(ExecutionContext ctx) {
        return new StringBuilder("<relativePath />");
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(StringBuilder tag) {
        return new TreeVisitor<>() {

            @Override
            public Tree visit(Tree tree, ExecutionContext ctx) {
                SourceFile sourceFile = (SourceFile) tree;
                if (sourceFile.getSourcePath().equals(ArchetypeCommonFile.WORKFLOW_CD.getPath())) {
                    tag.replace(0, tag.length(), "<relativePath/>");
                }
                return tree;
            }
        };
    }

    @Override
    public String getDescription() {
        return "Ensure the parent pom has a relativePath set to disable local resolution.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(StringBuilder expectedTag) {
        return new MavenIsoVisitor<>() {
            @Override
            public Xml.Tag visitTag(Xml.Tag tag, ExecutionContext ctx) {
                if (isParentTag()) {

                    if (tag.getContent() == null) {
                        return tag;
                    }
                    List<Content> contents = new ArrayList<>(tag.getContent());
                    Optional<Xml.Tag> maybeChild = tag.getChild("artifactId");
                    if (maybeChild.isEmpty()) {
                        return tag;
                    }
                    // Replace correct relative path if present
                    Optional<Xml.Tag> maybeRelativePath = tag.getChild("relativePath");
                    Xml.Tag relativePathTag = Xml.Tag.build(expectedTag.toString())
                            .withPrefix(maybeChild.get().getPrefix());
                    if (maybeRelativePath.isPresent()) {
                        Xml.Tag existingRelativePath = maybeRelativePath.get();
                        // Skip if already correct
                        if (existingRelativePath
                                .getBeforeTagDelimiterPrefix()
                                .equals(relativePathTag.getBeforeTagDelimiterPrefix())) {
                            return tag;
                        }
                        contents.remove(maybeRelativePath.get());
                        contents.add(relativePathTag);
                        return tag.withContent(contents);
                    }

                    // Add relative path
                    else {
                        contents.add(relativePathTag);
                        return tag.withContent(contents);
                    }
                }
                return super.visitTag(tag, ctx);
            }
        };
    }
}
