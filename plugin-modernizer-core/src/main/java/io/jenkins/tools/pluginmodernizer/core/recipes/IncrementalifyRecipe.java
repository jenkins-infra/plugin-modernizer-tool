package io.jenkins.tools.pluginmodernizer.core.recipes;

import java.io.File;
import java.util.Collections;
import org.apache.maven.shared.invoker.*;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.maven.MavenIsoVisitor;
import org.openrewrite.xml.tree.Xml;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IncrementalifyRecipe extends Recipe {

    private static final Logger LOG = LoggerFactory.getLogger(IncrementalifyRecipe.class);
    private Invoker invoker;
    private boolean skipM2HomeCheck = false;

    public IncrementalifyRecipe() {
        this.invoker = new DefaultInvoker();
    }

    // For testing purposes only
    void setInvokerForTesting(Invoker invoker) {
        this.invoker = invoker;
    }

    // For testing purposes only
    void setSkipM2HomeCheckForTesting(boolean skipM2HomeCheck) {
        this.skipM2HomeCheck = skipM2HomeCheck;
    }

    // For testing purposes only - directly execute Maven command on a file
    public void executeOnPomFile(File pomFile) throws MavenInvocationException {
        InvocationRequest request = new DefaultInvocationRequest();
        request.setPomFile(pomFile);
        request.setGoals(Collections.singletonList("incrementals:incrementalify"));
        StringBuilderOutputHandler outputHandler = new StringBuilderOutputHandler();
        request.setOutputHandler(outputHandler);

        if (this.invoker instanceof DefaultInvoker && !skipM2HomeCheck) {
            String m2Home = System.getenv("M2_HOME");
            if (m2Home == null || m2Home.isEmpty()) {
                LOG.error("M2_HOME environment variable is not set. Unable to execute Maven command.");
                return;
            }
            ((DefaultInvoker) this.invoker).setMavenHome(new File(m2Home));
        }

        InvocationResult result = invoker.execute(request);

        if (result.getExitCode() != 0) {
            LOG.error(
                    "Maven build failed with exit code {}: {}",
                    result.getExitCode(),
                    outputHandler.getOutput());
            throw new IllegalStateException("Maven build failed with exit code " + result.getExitCode()
                    + ". See logs for details.");
        }
        LOG.debug("Maven output: {}", outputHandler.getOutput());
    }

    @Override
    public String getDisplayName() {
        return "Incrementalify Recipe";
    }

    @Override
    public String getDescription() {
        return "Runs the `mvn incrementals:incrementalify` command to enable incrementals.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new MavenIsoVisitor<ExecutionContext>() {
            @Override
            public Xml.Document visitDocument(Xml.Document document, ExecutionContext ctx) {
                LOG.info("Visiting document with path: {}", document.getSourcePath());
                try {
                    InvocationRequest request = new DefaultInvocationRequest();
                    // Use the actual path of the document being processed
                    request.setPomFile(document.getSourcePath().toFile());
                    request.setGoals(Collections.singletonList("incrementals:incrementalify"));
                    // Capture output for logging
                    StringBuilderOutputHandler outputHandler = new StringBuilderOutputHandler();
                    request.setOutputHandler(outputHandler);
                    if (IncrementalifyRecipe.this.invoker instanceof DefaultInvoker && !skipM2HomeCheck) {
                        String m2Home = System.getenv("M2_HOME");
                        if (m2Home == null || m2Home.isEmpty()) {
                            LOG.error("M2_HOME environment variable is not set. Unable to execute Maven command.");
                            return document;
                        }
                        ((DefaultInvoker) IncrementalifyRecipe.this.invoker).setMavenHome(new File(m2Home));
                    }
                    InvocationResult result = invoker.execute(request);

                    if (result.getExitCode() != 0) {
                        LOG.error(
                                "Maven build failed with exit code {}: {}",
                                result.getExitCode(),
                                outputHandler.getOutput());
                        throw new IllegalStateException("Maven build failed with exit code " + result.getExitCode()
                                + ". See logs for details.");
                    }
                    LOG.debug("Maven output: {}", outputHandler.getOutput());
                } catch (MavenInvocationException e) {
                    LOG.error("Error executing mvn incrementals:incrementalify", e);
                    return document; // Explicitly return the original document
                }

                return super.visitDocument(document, ctx);
            }
        };
    }

    /**
     * Simple output handler implementation to capture Maven output
     */
    private static class StringBuilderOutputHandler implements InvocationOutputHandler {
        private final StringBuilder output = new StringBuilder();

        @Override
        public void consumeLine(String line) {
            output.append(line).append(System.lineSeparator());
        }

        public String getOutput() {
            return output.toString();
        }
    }
}
