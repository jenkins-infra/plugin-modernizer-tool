package io.jenkins.tools.pluginmodernizer.core.recipes;

import org.apache.maven.shared.invoker.*;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.maven.MavenIsoVisitor;
import org.openrewrite.xml.tree.Xml;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Collections;

public class IncrementalifyRecipe extends Recipe {

    private static final Logger LOG = LoggerFactory.getLogger(IncrementalifyRecipe.class);

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
                try {
                    InvocationRequest request = new DefaultInvocationRequest();
                    // Use the actual path of the document being processed
                    request.setPomFile(document.getSourcePath().toFile());
                    request.setGoals(Collections.singletonList("incrementals:incrementalify"));

                    Invoker invoker = new DefaultInvoker();
                    String m2Home = System.getenv("M2_HOME");
                    if (m2Home == null || m2Home.isEmpty()) {
                        LOG.error("M2_HOME environment variable is not set. Unable to execute Maven command.");
                        return document;
                    }
                    invoker.setMavenHome(new File(m2Home));
                    InvocationResult result = invoker.execute(request);

                    if (result.getExitCode() != 0) {
                        throw new IllegalStateException("Build failed.");
                    }
                } catch (MavenInvocationException e) {
                    LOG.error("Error executing mvn incrementals:incrementalify", e);
                }

                return super.visitDocument(document, ctx);
            }
        };
    }
}
