package io.jenkins.tools.pluginmodernizer.core.extractor;

import static io.jenkins.tools.pluginmodernizer.core.utils.JsonUtils.fromJson;
import static io.jenkins.tools.pluginmodernizer.core.utils.JsonUtils.merge;
import static io.jenkins.tools.pluginmodernizer.core.utils.JsonUtils.toJson;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Visitor to finalize metadata from source files
 */
public class MetadataFinalizerVisitor extends TreeVisitor<Tree, ExecutionContext> {

    /**
     * LOGGER.
     */
    private static final Logger LOG = LoggerFactory.getLogger(MetadataFinalizerVisitor.class);

    @Override
    public Tree visit(Tree tree, ExecutionContext ctx) {

        PluginMetadata mergedMetadata = ctx.getMessage("mergedMetadata", new PluginMetadata());
        PluginMetadata commonMetadata = ctx.getMessage("commonMetadata", new PluginMetadata());
        PluginMetadata pomMetadata = ctx.getMessage("pomMetadata", new PluginMetadata());
        PluginMetadata javaMetadata = ctx.getMessage("javaMetadata", new PluginMetadata());
        PluginMetadata jenkinsFileMetadata = ctx.getMessage("jenkinsFileMetadata", new PluginMetadata());

        // Merge the metadata
        PluginMetadata merged =
                fromJson(merge(pomMetadata.toJson(), jenkinsFileMetadata.toJson()), PluginMetadata.class);
        merged = fromJson(merge(commonMetadata.toJson(), merged.toJson()), PluginMetadata.class);
        merged = fromJson(merge(javaMetadata.toJson(), merged.toJson()), PluginMetadata.class);
        merged = fromJson(merge(mergedMetadata.toJson(), merged.toJson()), PluginMetadata.class);

        LOG.debug("Merged metadata: {}", toJson(merged));

        // Write the metadata to a file for later use by the plugin modernizer.
        merged.save();
        LOG.debug("Plugin metadata written to {}", merged.getRelativePath());
        ctx.putMessage("mergedMetadata", merged);
        LOG.debug(toJson(merged));

        return tree;
    }
}
