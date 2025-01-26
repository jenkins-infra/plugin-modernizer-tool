package io.jenkins.tools.pluginmodernizer.core.recipes;

import io.jenkins.tools.pluginmodernizer.core.extractor.MetadataExecutionContext;
import io.jenkins.tools.pluginmodernizer.core.extractor.MetadataVisitor;
import io.jenkins.tools.pluginmodernizer.core.extractor.PluginMetadata;
import io.jenkins.tools.pluginmodernizer.core.impl.CacheManager;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.RecipeList;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;

/**
 * Recipe to fetch metadata from source files and store it in the target directory.
 * See {@link MetadataVisitor} for more details.
 * See {@link MetadataFinalizer} for the finalization of the metadata and merging of metadata from different sources.
 * See {@link PluginMetadata} for the metadata structure.
 */
public class FetchMetadata extends Recipe {

    @Override
    public String getDisplayName() {
        return "Fetch metadata";
    }

    @Override
    public String getDescription() {
        return "Fetch metadata from source files.";
    }

    /**
     * Metadata file name. Default is plugin-metadata.
     */
    @Option(displayName = "File name", description = "The plugin metadata file name", example = "plugin-metadata")
    private final String fileName;

    /**
     * Default constructor with the default metadata file name.
     */
    public FetchMetadata() {
        this.fileName = CacheManager.PLUGIN_METADATA_CACHE_KEY;
        metadataContext = new MetadataExecutionContext();
    }

    /**
     * Constructor with the metadata file name.
     * @param fileName metadata file name
     */
    public FetchMetadata(String fileName) {
        this.fileName = fileName;
        metadataContext = new MetadataExecutionContext(fileName);
    }

    /**
     * Metadata context to store the metadata extracted from different sources.
     */
    private final MetadataExecutionContext metadataContext;

    @Override
    public void buildRecipeList(RecipeList list) {
        super.buildRecipeList(list);
        list.recipe(new MetadataFinalizer(metadataContext));
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new TreeVisitor<>() {
            @Override
            public Tree visit(Tree tree, ExecutionContext ctx) {
                return new MetadataVisitor(new PluginMetadata()).visit(tree, metadataContext);
            }
        };
    }
}
