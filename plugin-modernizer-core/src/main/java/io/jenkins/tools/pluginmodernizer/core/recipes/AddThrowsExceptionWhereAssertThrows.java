package io.jenkins.tools.pluginmodernizer.core.recipes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JRightPadded;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.NameTree;
import org.openrewrite.java.tree.Space;
import org.openrewrite.marker.Markers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AddThrowsExceptionWhereAssertThrows extends Recipe {

    /**
     * Logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(AddThrowsExceptionWhereAssertThrows.class);

    @Override
    public String getDisplayName() {
        return "Add throws exception to methods using assertThrows";
    }

    @Override
    public String getDescription() {
        return "Adds 'throws Exception' to test methods that contain assertThrows calls.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<>() {
            // find the test method using assertThrows
            // firstly check if assertThrows is imported, if not skip that file
            @Override
            public J.CompilationUnit visitCompilationUnit(J.CompilationUnit cu, ExecutionContext ctx) {
                // Check for relevant imports
                boolean hasAssertThrowsImport = cu.getImports().stream()
                        .anyMatch(imp -> imp.getPackageName().equals("org.junit.jupiter.api")
                                && (imp.getClassName().equals("Assertions")
                                        || imp.getClassName().equals("*")));
                if (!hasAssertThrowsImport) {
                    return cu; // Skip files without relevant imports
                }

                return super.visitCompilationUnit(cu, ctx);
            }

            @Override
            public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext ctx) {
                method = super.visitMethodDeclaration(method, ctx);
                // Check if method is a test method (has @Test annotation)
                if (method.getLeadingAnnotations().stream()
                        .anyMatch(annotation -> annotation.getType() != null
                                && annotation.getSimpleName().equals("Test"))) {
                    // Check if method contains assertThrows
                    if (containsAssertThrows(method)) {
                        LOG.info(
                                "Found assertThrows for method {} in class {}",
                                method.getSimpleName(),
                                method.getMethodType().getDeclaringType().getFullyQualifiedName());
                        // Check if 'throws Exception' is already present
                        if (method.getThrows() == null
                                || method.getThrows().stream()
                                        .noneMatch(t -> t.getType() != null
                                                && t.getType().toString().equals("java.lang.Exception"))) {
                            // Add 'throws Exception'
                            List<NameTree> newThrows = new ArrayList<>();
                            newThrows.add(new JRightPadded<>(
                                            new J.Identifier(
                                                    Tree.randomId(),
                                                    Space.SINGLE_SPACE,
                                                    Markers.EMPTY,
                                                    Collections.emptyList(),
                                                    "Exception",
                                                    JavaType.buildType("java.lang.Exception"),
                                                    null),
                                            Space.SINGLE_SPACE,
                                            Markers.EMPTY)
                                    .getElement());
                            J.MethodDeclaration withThrows = method.withThrows(newThrows);
                            LOG.info(
                                    "Added 'throws Exception' to method {} in class {}",
                                    method.getSimpleName(),
                                    method.getMethodType().getDeclaringType().getFullyQualifiedName());
                            return withThrows;
                        }
                    }
                }
                return method;
            }

            private boolean containsAssertThrows(J.MethodDeclaration method) {
                AtomicBoolean found = new AtomicBoolean(false);
                new JavaIsoVisitor<AtomicBoolean>() {
                    @Override
                    public J.MethodInvocation visitMethodInvocation(
                            J.MethodInvocation methodInvocation, AtomicBoolean found) {
                        if (found.get()) return methodInvocation; // Short-circuit if already found
                        if ("assertThrows".equals(methodInvocation.getSimpleName())) {
                            if (methodInvocation.getSelect() == null
                                    || (methodInvocation.getSelect() instanceof J.Identifier
                                            && "Assertions"
                                                    .equals(((J.Identifier) methodInvocation.getSelect())
                                                            .getSimpleName()))) {
                                found.set(true);
                            }
                        }
                        return super.visitMethodInvocation(methodInvocation, found);
                    }
                }.visit(method.getBody(), found);
                return found.get();
            }
        };
    }
}
