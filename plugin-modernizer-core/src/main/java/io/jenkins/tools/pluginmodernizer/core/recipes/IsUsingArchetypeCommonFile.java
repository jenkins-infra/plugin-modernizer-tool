package io.jenkins.tools.pluginmodernizer.core.recipes;

import io.jenkins.tools.pluginmodernizer.core.extractor.ArchetypeCommonFile;
import io.jenkins.tools.pluginmodernizer.core.extractor.ArchetypeCommonFileVisitor;
import io.jenkins.tools.pluginmodernizer.core.extractor.PluginMetadata;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.SourceFile;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.marker.SearchResult;

/**
 * Determine if the plugin is using a common file.
 */
public class IsUsingArchetypeCommonFile extends Recipe {

    /**
     * The archetype common file to check for.
     */
    private final ArchetypeCommonFile archetypeCommonFile;

    /**
     * Constructor.
     * @param archetypeCommonFile the archetype common file to check for
     */
    public IsUsingArchetypeCommonFile(ArchetypeCommonFile archetypeCommonFile) {
        this.archetypeCommonFile = archetypeCommonFile;
    }

    @Override
    public String getDisplayName() {
        return "Check if the project is using a common file";
    }

    @Override
    public String getDescription() {
        return "Checks if the project is using a common file.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public Tree visit(Tree tree, ExecutionContext ctx) {

                PluginMetadata pluginMetadata = new ArchetypeCommonFileVisitor().reduce(tree, new PluginMetadata());
                if (pluginMetadata.hasCommonFile(archetypeCommonFile)) {
                    SourceFile sourceFile = (SourceFile) tree;
                    if (sourceFile.getSourcePath().equals(archetypeCommonFile.getPath())) {
                        return SearchResult.found(tree, "Project is using " + archetypeCommonFile.getPath());
                    }
                }

                return tree;
            }
        };
    }
}
