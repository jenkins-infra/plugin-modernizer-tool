package io.jenkins.tools.pluginmodernizer.core.extractor;

import io.jenkins.tools.pluginmodernizer.core.utils.JsonUtils;
import org.openrewrite.ExecutionContext;
import org.openrewrite.PathUtils;
import org.openrewrite.SourceFile;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Visitor to extract metadata from source files.
 */
public class MetadataVisitor extends TreeVisitor<Tree, ExecutionContext> {

    /**
     * LOGGER.
     */
    private static final Logger LOG = LoggerFactory.getLogger(MetadataVisitor.class);

    final PluginMetadata pluginMetadata;

    /**
     * Constructor.
     * @param pluginMetadata the plugin metadata
     */
    public MetadataVisitor(PluginMetadata pluginMetadata) {
        this.pluginMetadata = pluginMetadata;
    }

    @Override
    public Tree visit(Tree tree, ExecutionContext executionContext) {

        SourceFile sourceFile = (SourceFile) tree;

        // Common metadata
        PluginMetadata commonMetadata = new ArchetypeCommonFileVisitor().reduce(tree, pluginMetadata);

        // Extract metadata from Jenkinsfile
        if (PathUtils.matchesGlob(sourceFile.getSourcePath(), "**/Jenkinsfile")) {
            LOG.debug("Visiting Jenkinsfile {}", sourceFile.getSourcePath());
            PluginMetadata jenkinsFileMetadata = new JenkinsfileVisitor().reduce(tree, commonMetadata);
            LOG.debug("Jenkinsfile metadata: {}", JsonUtils.toJson(jenkinsFileMetadata));
            executionContext.putMessage(
                    "jenkinsFileMetadata", jenkinsFileMetadata); // Is there better than context messaging ?
            return tree;
        }

        // Extract metadata from POM
        else if (PathUtils.matchesGlob(sourceFile.getSourcePath(), "**/pom.xml")) {
            LOG.debug("Visiting POM {}", sourceFile.getSourcePath());
            PluginMetadata pomMetadata = new PomResolutionVisitor().reduce(tree, commonMetadata);
            LOG.debug("POM metadata: {}", JsonUtils.toJson(pomMetadata));
            executionContext.putMessage("pomMetadata", pomMetadata); // Is there better than context messaging ?
            return tree;
        }
        // Extract metadata from java file
        else if (PathUtils.matchesGlob(sourceFile.getSourcePath(), "**/*.java")) {
            LOG.debug("Visiting Java file {}", sourceFile.getSourcePath());
            PluginMetadata javaMetadata = new JavaFileVisitor().reduce(tree, commonMetadata);
            LOG.debug("Java metadata: {}", JsonUtils.toJson(javaMetadata));
            executionContext.putMessage("javaMetadata", javaMetadata); // Is there better than context messaging ?
            return tree;
        }

        // Just add the common
        else {
            executionContext.putMessage("commonMetadata", commonMetadata); // Is there better than context messaging ?
        }

        return tree;
    }
}
