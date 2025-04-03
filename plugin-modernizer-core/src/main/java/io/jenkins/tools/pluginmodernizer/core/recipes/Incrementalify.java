package io.jenkins.tools.pluginmodernizer.core.recipes;

import io.jenkins.tools.pluginmodernizer.core.impl.MavenInvoker;
import io.jenkins.tools.pluginmodernizer.core.model.Plugin;
import jakarta.inject.Inject;
import java.nio.file.Path;
import org.jetbrains.annotations.NotNull;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.maven.MavenIsoVisitor;
import org.openrewrite.xml.tree.Xml;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Recipe to enable incrementals in a Jenkins plugin.
 * This recipe runs the incrementals:incrementalify goal which:
 * 1. Adds the incrementals-maven-plugin to the pom.xml if needed
 * 2. Creates the .mvn directory and necessary files
 * 3. Configures the plugin for incremental builds
 */
public class Incrementalify extends Recipe {

    private static final Logger LOG = LoggerFactory.getLogger(Incrementalify.class);

    @Inject
    private MavenInvoker mavenInvoker;

    @Override
    public @NotNull String getDisplayName() {
        return "Incrementalify recipe";
    }

    @Override
    public @NotNull String getDescription() {
        return "Enables incrementals in a Jenkins plugin by running the incrementals:incrementalify goal.";
    }

    @Override
    public @NotNull TreeVisitor<?, ExecutionContext> getVisitor() {
        return new MavenIsoVisitor<ExecutionContext>() {
            @Override
            public Xml.Document visitDocument(Xml.Document document, ExecutionContext ctx) {
                if (document == null || document.getSourcePath() == null) {
                    LOG.warn("Document or source path is null, skipping incrementalify");
                    return document;
                }

                LOG.info("Visiting document with path: {}", document.getSourcePath());

                // Get the parent path or use the current directory if parent is null
                Path workingPath;
                Path sourcePath = document.getSourcePath();
                Path parentPath = sourcePath.getParent();

                if (parentPath == null) {
                    // If the parent path is null, use the current directory
                    LOG.info("Parent path is null for document: {}, using current directory", sourcePath);
                    workingPath = Path.of(".");
                } else {
                    workingPath = parentPath;
                }

                try {
                    // Create a temporary Plugin object to use with MavenInvoker
                    Plugin plugin = Plugin.build("temp-plugin", workingPath);

                    // Run the incrementals:incrementalify goal which will:
                    // 1. Add the incrementals-maven-plugin to the pom.xml if needed
                    // 2. Create the .mvn directory and necessary files
                    // 3. Configure the plugin for incremental builds
                    LOG.info("Running incrementals:incrementalify goal...");
                    mavenInvoker.invokeGoal(plugin, "incrementals:incrementalify");

                    LOG.info("Successfully enabled incrementals for plugin at {}", workingPath.toAbsolutePath());
                } catch (Exception e) {
                    LOG.error("Error enabling incrementals", e);
                    // Return the original document if there was an error
                    return document;
                }

                return super.visitDocument(document, ctx);
            }
        };
    }
}
