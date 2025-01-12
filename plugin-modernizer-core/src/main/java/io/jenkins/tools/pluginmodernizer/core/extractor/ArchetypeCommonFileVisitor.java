package io.jenkins.tools.pluginmodernizer.core.extractor;

import org.openrewrite.SourceFile;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A tree visitor that visit a tree and populate the metadata with common files.
 */
public class ArchetypeCommonFileVisitor extends TreeVisitor<Tree, PluginMetadata> {

    /**
     * LOGGER.
     */
    private static final Logger LOG = LoggerFactory.getLogger(ArchetypeCommonFileVisitor.class);

    @Override
    public Tree visit(Tree tree, PluginMetadata pluginMetadata) {

        SourceFile sourceFile = (SourceFile) tree;

        ArchetypeCommonFile commonFile = ArchetypeCommonFile.fromPath(sourceFile.getSourcePath());

        // Store common files into metadata
        if (commonFile != null) {
            if (pluginMetadata.hasCommonFile(commonFile)) {
                LOG.debug("File {} is already a common file", sourceFile.getSourcePath());
            } else {
                LOG.debug("File {} is a common file", sourceFile.getSourcePath());
                pluginMetadata.addCommonFile(commonFile);
            }
        } else {
            LOG.debug("File {} is not a common file", sourceFile.getSourcePath());
        }
        return tree;
    }
}
