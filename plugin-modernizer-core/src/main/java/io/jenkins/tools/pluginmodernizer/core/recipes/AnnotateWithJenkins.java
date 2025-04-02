package io.jenkins.tools.pluginmodernizer.core.recipes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.openrewrite.ExecutionContext;
import org.openrewrite.ScanningRecipe;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.AnnotationMatcher;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JRightPadded;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.Space;
import org.openrewrite.marker.Markers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AnnotateWithJenkins extends ScanningRecipe<Set<String>> {

    /**
     * Logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(AnnotateWithJenkins.class);

    @Override
    public String getDisplayName() {
        return "Annotate classes with @WithJenkins";
    }

    @Override
    public String getDescription() {
        return "Detects classes using @Rule with JenkinsRule and annotates them with @WithJenkins.";
    }

    @Override
    public Set<String> getInitialValue(ExecutionContext ctx) {
        return new HashSet<>();
    }

    /**
     * Detects classes that are using @Rule annotation with JenkinsRule.
     */
    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(Set<String> acc) {
        return new RuleAnnotationScanner(acc);
    }

    /**
     * Annotate the classes with @WithJenkins if they were detected.
     */
    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(Set<String> acc) {
        return new WithJenkinsAnnotationVisitor(acc);
    }

    public static class RuleAnnotationScanner extends JavaIsoVisitor<ExecutionContext> {
        private final Set<String> acc;

        public RuleAnnotationScanner(Set<String> acc) {
            this.acc = acc;
        }

        @Override
        public J.Annotation visitAnnotation(J.Annotation annotation, ExecutionContext ctx) {

            // Look for @Rule annotation and the field of type JenkinsRule
            if (annotation.getSimpleName().equals("Rule")) {
                // Check if the field annotated with @Rule is of type JenkinsRule
                J.VariableDeclarations variableDeclarations = getCursor().firstEnclosing(J.VariableDeclarations.class);
                if (variableDeclarations != null) {
                    String fieldNameType = Objects.requireNonNull(
                                    variableDeclarations.getVariables().get(0).getType())
                            .toString();
                    if (fieldNameType.equals("org.jvnet.hudson.test.JenkinsRule")) {
                        LOG.info("Field annotated with @Rule is of type : {}", fieldNameType);
                        J.ClassDeclaration classDecl = getCursor().firstEnclosing(J.ClassDeclaration.class);
                        if (classDecl != null) {
                            assert classDecl.getType() != null : "classDecl.getType() is null";
                            acc.add(classDecl.getType().getFullyQualifiedName());
                        }
                    }
                }
            }
            return super.visitAnnotation(annotation, ctx);
        }
    }
    ;

    public static class WithJenkinsAnnotationVisitor extends JavaIsoVisitor<ExecutionContext> {
        private final Set<String> acc;

        public WithJenkinsAnnotationVisitor(Set<String> acc) {
            this.acc = acc;
        }

        @Override
        public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext ctx) {

            assert classDecl.getType() != null;
            if (acc.contains(classDecl.getType().getFullyQualifiedName())) {
                if (classDecl.getLeadingAnnotations().stream()
                        .noneMatch(new AnnotationMatcher("@WithJenkins")::matches)) {

                    maybeAddImport("org.jvnet.hudson.test.junit.jupiter.WithJenkins");

                    classDecl = classDecl.withLeadingAnnotations(new ArrayList<>(classDecl.getLeadingAnnotations()) {
                        {
                            add(new J.Annotation(
                                    UUID.randomUUID(),
                                    Space.EMPTY,
                                    Markers.EMPTY,
                                    new J.Identifier(
                                            Tree.randomId(),
                                            Space.EMPTY,
                                            Markers.EMPTY,
                                            "WithJenkins\n",
                                            JavaType.buildType("org.jvnet.hudson.test.junit.jupiter.WithJenkins"),
                                            null),
                                    null));
                        }
                    });

                    LOG.info("Annotated class with @WithJenkins: {}", classDecl.getSimpleName());
                }
                // Remove the @Rule JenkinsRule field
                classDecl = classDecl.withBody(classDecl
                        .getBody()
                        .withStatements(classDecl.getBody().getStatements().stream()
                                .filter(statement -> {
                                    if (statement instanceof J.VariableDeclarations) {
                                        J.VariableDeclarations varDecl = (J.VariableDeclarations) statement;
                                        return varDecl.getVariables().stream().noneMatch(variable -> {
                                            JavaType.FullyQualified type = (JavaType.FullyQualified) variable.getType();
                                            return type != null
                                                    && type.getFullyQualifiedName()
                                                            .equals("org.jvnet.hudson.test.JenkinsRule")
                                                    && varDecl.getLeadingAnnotations().stream()
                                                            .anyMatch(ann -> ann.getSimpleName()
                                                                    .equals("Rule"));
                                        });
                                    }
                                    return true;
                                })
                                .collect(Collectors.toList())));

                // Add parameter JenkinsRule j to all the methods that uses j.something
                classDecl = classDecl.withBody(classDecl
                        .getBody()
                        .withStatements(classDecl.getBody().getStatements().stream()
                                .map(statement -> {
                                    if (statement instanceof J.MethodDeclaration) {
                                        J.MethodDeclaration methodDecl = (J.MethodDeclaration) statement;
                                        if (methodDecl.getBody() != null
                                                && methodDecl.getBody().getStatements().stream()
                                                        .anyMatch(stmt ->
                                                                stmt.print().contains("j."))) {
                                            LOG.info(
                                                    "JenkinsRule j parameter added to: {}", methodDecl.getSimpleName());
                                            J.MethodDeclaration finalMethodDecl = methodDecl;
                                            methodDecl = methodDecl.withParameters(new ArrayList<>() {

                                                {
                                                    boolean emptyParams = finalMethodDecl.getParameters().stream()
                                                            .anyMatch(param -> param instanceof J.Empty);
                                                    if (!emptyParams) {
                                                        addAll(finalMethodDecl.getParameters());
                                                    }

                                                    add(new J.VariableDeclarations(
                                                            Tree.randomId(),
                                                            emptyParams ? Space.EMPTY : Space.SINGLE_SPACE,
                                                            Markers.EMPTY,
                                                            Collections.emptyList(),
                                                            Collections.emptyList(),
                                                            new J.Identifier(
                                                                    Tree.randomId(),
                                                                    Space.EMPTY,
                                                                    Markers.EMPTY,
                                                                    "JenkinsRule",
                                                                    JavaType.buildType(
                                                                            "org.jvnet.hudson.test.JenkinsRule"),
                                                                    null),
                                                            null,
                                                            Collections.emptyList(),
                                                            Collections.singletonList(new JRightPadded<>(
                                                                    new J.VariableDeclarations.NamedVariable(
                                                                            Tree.randomId(),
                                                                            Space.SINGLE_SPACE,
                                                                            Markers.EMPTY,
                                                                            new J.Identifier(
                                                                                    Tree.randomId(),
                                                                                    Space.EMPTY,
                                                                                    Markers.EMPTY,
                                                                                    "j",
                                                                                    JavaType.buildType(
                                                                                            "org.jvnet.hudson.test.JenkinsRule"),
                                                                                    null),
                                                                            Collections.emptyList(),
                                                                            null,
                                                                            null),
                                                                    Space.EMPTY,
                                                                    Markers.EMPTY))));
                                                }
                                            });
                                        }

                                        return methodDecl;
                                    }

                                    return statement;
                                })
                                .collect(Collectors.toList())));
            }

            return super.visitClassDeclaration(classDecl, ctx);
        }
    }
    ;
}
