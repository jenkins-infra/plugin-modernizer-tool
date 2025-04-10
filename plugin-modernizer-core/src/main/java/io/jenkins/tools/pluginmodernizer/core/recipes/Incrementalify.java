package io.jenkins.tools.pluginmodernizer.core.recipes;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
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
                if (document == null) {
                    LOG.warn("Document is null, skipping incrementalify");
                    return document;
                }

                // Get the source path from the document
                Path sourcePath = document.getSourcePath();
                if (sourcePath == null) {
                    LOG.warn("Source path is null, skipping incrementalify");
                    return document;
                }

                LOG.info("Visiting document with path: {}", sourcePath);

                // Get the parent path or use the current directory if parent is null
                Path workingPath;
                Path parentPath = sourcePath.getParent();

                if (parentPath == null) {
                    // If the parent path is null, use the current directory
                    LOG.info("Parent path is null for document: {}, using current directory", sourcePath);
                    workingPath = Path.of(".");
                } else {
                    workingPath = parentPath;
                }

                try {
                    // Run the incrementals:incrementalify goal using ProcessBuilder
                    LOG.info("Running incrementals:incrementalify goal...");

                    // Create the process builder for running the Maven command
                    ProcessBuilder processBuilder = new ProcessBuilder("mvn", "incrementals:incrementalify");

                    // Set the working directory
                    processBuilder.directory(workingPath.toFile());

                    // Redirect error stream to output stream
                    processBuilder.redirectErrorStream(true);

                    // Start the process
                    Process process = processBuilder.start();

                    // Read the output
                    try (BufferedReader reader = new BufferedReader(
                            new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                        String line;
                        StringBuilder output = new StringBuilder();
                        while ((line = reader.readLine()) != null) {
                            // Collect the output but don't log each line individually
                            output.append(line).append(System.lineSeparator());
                        }
                        // Log a message about the process completion instead
                        LOG.info("Maven process output collected");
                    }

                    // Wait for the process to complete
                    int exitCode = process.waitFor();

                    if (exitCode == 0) {
                        LOG.info("Successfully enabled incrementals for plugin at {}", workingPath.toAbsolutePath());
                    } else {
                        LOG.error("Failed to enable incrementals, exit code: {}", exitCode);
                        LOG.info("To enable incrementals, run: mvn incrementals:incrementalify");
                    }
                } catch (IOException | InterruptedException e) {
                    LOG.error("Error enabling incrementals", e);
                    LOG.info("To enable incrementals, run: mvn incrementals:incrementalify");
                    // Return the original document if there was an error
                    return document;
                }

                return super.visitDocument(document, ctx);
            }
        };
    }
}
