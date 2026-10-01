package io.jenkins.tools.pluginmodernizer.core.recipes;

import io.jenkins.tools.pluginmodernizer.core.extractor.PluginMetadata;
import io.jenkins.tools.pluginmodernizer.core.extractor.PomResolutionVisitor;
import io.jenkins.tools.pluginmodernizer.core.visitors.UpdateBomVersionVisitor;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.marker.SearchResult;
import org.openrewrite.maven.MavenIsoVisitor;
import org.openrewrite.maven.table.MavenMetadataFailures;
import org.openrewrite.xml.tree.Xml;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Determines if this project is using a BOM in it's bom file
 */
public class IsUsingBom extends Recipe {

    /**
     * LOGGER.
     */
    private static final Logger LOG = LoggerFactory.getLogger(IsUsingBom.class);

    /**
     * Indicate if a pom must be present on the current document or also on parent
     */
    private boolean onDocument = false;

    /**
     * Check if using bom on current pom and parent
     */
    public IsUsingBom() {
        onDocument = false;
    }

    /**
     * Check if using bom on current pom or parent
     * @param onDocument if the bom is on the current document
     */
    public IsUsingBom(boolean onDocument) {
        this.onDocument = onDocument;
    }

    @Override
    public String getDisplayName() {
        return "Is the project using a Jenkins BOM?";
    }

    @Override
    public String getDescription() {
        return "Checks if the project is using a Jenkins BOM.";
    }

    /**
     * A visitor that checks if the project is using a BOM
     */
    private static class IsUsingBomVisitor extends MavenIsoVisitor<ExecutionContext> {

        /**
         * The metadata failures from recipe
         */
        private final transient MavenMetadataFailures metadataFailures;

        /**
         * Indicate if a pom must be present on the current document or also on parent is accepted
         */
        private final boolean onDocument;

        /**
         * Constructor
         * @param onDocument if the bom is on the current document
         * @param metadataFailures the metadata failures
         */
        public IsUsingBomVisitor(boolean onDocument, MavenMetadataFailures metadataFailures) {
            this.onDocument = onDocument;
            this.metadataFailures = metadataFailures;
        }

        @Override
        public Xml.Document visitDocument(Xml.Document document, ExecutionContext ctx) {

            PluginMetadata pluginMetadata = new PomResolutionVisitor().reduce(document, new PluginMetadata());

            // Check if the project is using a bom on current pom
            if (onDocument) {
                Xml.Tag bom = new UpdateBomVersionVisitor(metadataFailures).getBomTag(document);
                if (bom == null) {
                    return document;
                }
            }

            if (pluginMetadata.getBomVersion() != null) {
                LOG.info("Project is using Jenkins bom at version {}", pluginMetadata.getBomVersion());
                return SearchResult.found(document, "Project is using Jenkins bom");
            }

            return document;
        }
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new IsUsingBomVisitor(onDocument, new MavenMetadataFailures(this));
    }
}
