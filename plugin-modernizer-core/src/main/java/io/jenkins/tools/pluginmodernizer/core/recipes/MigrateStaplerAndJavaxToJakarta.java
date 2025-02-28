package io.jenkins.tools.pluginmodernizer.core.recipes;

import java.util.HashSet;
import java.util.Set;
import org.openrewrite.ExecutionContext;
import org.openrewrite.ScanningRecipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.ChangeMethodName;
import org.openrewrite.java.ChangePackage;
import org.openrewrite.java.ChangeType;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MigrateStaplerAndJavaxToJakarta extends ScanningRecipe<Set<String>> {

    /**
     * Logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(MigrateStaplerAndJavaxToJakarta.class);

    @Override
    public String getDisplayName() {
        return "Migrate Stapler types and methods and javax to jakarta";
    }

    @Override
    public String getDescription() {
        return "Migrate Stapler types and methods and javax to jakarta.";
    }

    @Override
    public Set<String> getInitialValue(ExecutionContext ctx) {
        return new HashSet<>();
    }

    /**
     *  Checks if ChartUtil is used or not, if used add it in accumulator.
     */
    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(Set<String> acc) {
        return new JavaIsoVisitor<>() {
            @Override
            public J.Import visitImport(J.Import importStmt, ExecutionContext ctx) {
                String importedClass = importStmt.getTypeName();

                if (importedClass.equals("hudson.util.ChartUtil")) {
                    LOG.info("Found usage of ChartUtil, skipping migration of stapler2 and jakarta");
                    acc.add(importedClass);
                }
                return super.visitImport(importStmt, ctx);
            }
        };
    }

    /**
     * Migrate To Stapler2 and javax to jakarta if accumulator is empty
     */
    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(Set<String> acc) {

        return new JavaIsoVisitor<>() {

            @Override
            public J.CompilationUnit visitCompilationUnit(J.CompilationUnit cu, ExecutionContext ctx) {
                if (acc.isEmpty()) {
                    cu = (J.CompilationUnit) new ChangePackage("javax.servlet", "jakarta.servlet", true)
                            .getVisitor()
                            .visitNonNull(cu, ctx);
                }
                return super.visitCompilationUnit(cu, ctx);
            }

            @Override
            public J.Identifier visitIdentifier(J.Identifier ident, ExecutionContext ctx) {
                if (acc.isEmpty()) {
                    ident = (J.Identifier) new ChangeType(
                                    "org.kohsuke.stapler.StaplerRequest", "org.kohsuke.stapler.StaplerRequest2", null)
                            .getVisitor()
                            .visitNonNull(ident, ctx);
                    ident = (J.Identifier) new ChangeType(
                                    "org.kohsuke.stapler.StaplerResponse", "org.kohsuke.stapler.StaplerResponse2", null)
                            .getVisitor()
                            .visitNonNull(ident, ctx);
                }
                return super.visitIdentifier(ident, ctx);
            }

            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                if (acc.isEmpty()) {
                    method = (J.MethodInvocation) new ChangeMethodName(
                                    "org.kohsuke.stapler.Stapler getCurrentRequest()", "getCurrentRequest2", null, null)
                            .getVisitor()
                            .visitNonNull(method, ctx);
                    method = (J.MethodInvocation) new ChangeMethodName(
                                    "org.kohsuke.stapler.Stapler getCurrentResponse()",
                                    "getCurrentResponse2",
                                    null,
                                    null)
                            .getVisitor()
                            .visitNonNull(method, ctx);
                }
                return super.visitMethodInvocation(method, ctx);
            }
        };
    }
}
