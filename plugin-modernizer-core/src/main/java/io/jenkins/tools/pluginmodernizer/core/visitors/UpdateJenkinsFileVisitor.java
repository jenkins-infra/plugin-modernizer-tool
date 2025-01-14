package io.jenkins.tools.pluginmodernizer.core.visitors;

import java.util.List;
import org.openrewrite.ExecutionContext;
import org.openrewrite.groovy.GroovyIsoVisitor;
import org.openrewrite.groovy.tree.G;
import org.openrewrite.java.TreeVisitingPrinter;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.TextComment;
import org.openrewrite.marker.Markers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A visitor to update the Jenkinsfile with recommended configuration
 */
public class UpdateJenkinsFileVisitor extends GroovyIsoVisitor<ExecutionContext> {

    /**
     * The method comment
     */
    public static final String METHOD_COMMENT =
            """

             See the documentation for more options:
             https://github.com/jenkins-infra/pipeline-library/
            """;

    /**
     * LOGGER.
     */
    private static final Logger LOG = LoggerFactory.getLogger(UpdateJenkinsFileVisitor.class);

    @Override
    public G.CompilationUnit visitCompilationUnit(G.CompilationUnit cu, ExecutionContext ctx) {
        // Debug purpose
        LOG.debug(TreeVisitingPrinter.printTree(cu));
        return super.visitCompilationUnit(cu, ctx);
    }

    @Override
    public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
        method = super.visitMethodInvocation(method, ctx);
        LOG.debug("Visiting method invocation {}", method);
        if (!"buildPlugin".equals(method.getSimpleName())) {
            return method;
        }
        // Add or replace a comment before the method
        TextComment existingComment = method.getComments().isEmpty()
                ? null
                : (TextComment) method.getComments().get(0);
        if (method.getComments().isEmpty() || !existingComment.getText().equals(METHOD_COMMENT)) {
            return method.withComments(List.of(new TextComment(true, METHOD_COMMENT, "\n", Markers.EMPTY)));
        }
        return method;
    }
}
