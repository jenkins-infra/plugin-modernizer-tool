package io.jenkins.tools.pluginmodernizer.core.recipes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
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

public class AnnotateWithJenkins extends ScanningRecipe<Map<String, String>> {

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
    public Map<String, String> getInitialValue(ExecutionContext ctx) {
        return new HashMap<>();
    }

    /**
     * Detects classes that are using @Rule annotation with JenkinsRule.
     */
    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(Map<String, String> acc) {
        return new RuleAnnotationScanner(acc);
    }

    /**
     * Annotate the classes with @WithJenkins if they were detected.
     */
    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(Map<String, String> acc) {
        return new WithJenkinsAnnotationVisitor(acc);
    }

    public static class RuleAnnotationScanner extends JavaIsoVisitor<ExecutionContext> {
        private final Map<String, String> acc;

        public RuleAnnotationScanner(Map<String, String> acc) {
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
                    String fieldName =
                            variableDeclarations.getVariables().get(0).getSimpleName();
                    if (fieldNameType.equals("org.jvnet.hudson.test.JenkinsRule")) {
                        J.ClassDeclaration classDecl = getCursor().firstEnclosing(J.ClassDeclaration.class);
                        if (classDecl != null) {
                            assert classDecl.getType() != null : "classDecl.getType() is null";
                            acc.put(classDecl.getType().getFullyQualifiedName(), fieldName);
                            LOG.info(
                                    "Found @Rule JenkinsRule in class: {} with field name : {}",
                                    classDecl.getType().getFullyQualifiedName(),
                                    fieldName);
                        }
                    }
                }
            }
            return super.visitAnnotation(annotation, ctx);
        }
    }
    ;

    public static class WithJenkinsAnnotationVisitor extends JavaIsoVisitor<ExecutionContext> {
        private final Map<String, String> acc;

        public WithJenkinsAnnotationVisitor(Map<String, String> acc) {
            this.acc = acc;
        }

        @Override
        public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext ctx) {

            assert classDecl.getType() != null;
            String fullyQualifiedClassName = classDecl.getType().getFullyQualifiedName();
            // if it's a class in accumulator then do these 3 things
            // 1- add @WithJenkins annotation
            // 2- remove @Rule JenkinsRule field
            // 3- add parameter JenkinsRule <variableName> to all the methods that uses <variableName>.something
            if (acc.containsKey(fullyQualifiedClassName)) {
                if (classDecl.getLeadingAnnotations().stream()
                        .noneMatch(new AnnotationMatcher("@WithJenkins")::matches)) {

                    maybeAddImport("org.jvnet.hudson.test.junit.jupiter.WithJenkins");

                    ArrayList<J.Annotation> leading = new ArrayList<>(classDecl.getLeadingAnnotations());
                    leading.add(new J.Annotation(
                            UUID.randomUUID(),
                            Space.EMPTY,
                            Markers.EMPTY,
                            new J.Identifier(
                                    Tree.randomId(),
                                    Space.EMPTY,
                                    Markers.EMPTY,
                                    Collections.emptyList(),
                                    "WithJenkins\n",
                                    JavaType.buildType("org.jvnet.hudson.test.junit.jupiter.WithJenkins"),
                                    null),
                            null));
                    classDecl = classDecl.withLeadingAnnotations(leading);

                    LOG.info("Annotated class with @WithJenkins: {}", classDecl.getSimpleName());
                }
                // Remove the @Rule JenkinsRule field
                classDecl = classDecl.withBody(classDecl
                        .getBody()
                        .withStatements(classDecl.getBody().getStatements().stream()
                                .filter(statement -> {
                                    if (statement instanceof J.VariableDeclarations) {
                                        J.VariableDeclarations varDecl = (J.VariableDeclarations) statement;
                                        for (J.VariableDeclarations.NamedVariable var : varDecl.getVariables()) {
                                            JavaType.FullyQualified type = (JavaType.FullyQualified) var.getType();
                                            if (type != null
                                                    && type.getFullyQualifiedName()
                                                            .equals("org.jvnet.hudson.test.JenkinsRule")
                                                    && varDecl.getLeadingAnnotations().stream()
                                                            .anyMatch(ann -> ann.getSimpleName()
                                                                    .equals("Rule"))) {
                                                return false; // remove this field
                                            }
                                        }
                                    }
                                    return true;
                                })
                                .collect(Collectors.toList())));

                // Add parameter JenkinsRule <variableName> to all the methods that uses <variableName>.something
                String jenkinsRuleFieldName = acc.get(fullyQualifiedClassName);
                classDecl = addParamJenkinsRule(classDecl, jenkinsRuleFieldName);
                return super.visitClassDeclaration(classDecl, ctx);
            }

            String superClassFullyQualifiedName = null;
            if (classDecl.getExtends() != null) {
                JavaType.FullyQualified superClassType =
                        (JavaType.FullyQualified) classDecl.getExtends().getType();
                if (superClassType != null) {
                    superClassFullyQualifiedName = superClassType.getFullyQualifiedName();
                }
            }
            // else check if the class is a sub class of any of the classes in the accumulator
            // if yes, then check all the methods of this class as it maybe using JenkinsRule field
            // as it inherited @Rule JenkinsRule from it's parent which we already removed
            // so now add JenkinsRule field as a parameter to the methods using it
            if (acc.containsKey(superClassFullyQualifiedName)) {
                String jenkinsRuleFieldName = acc.get(superClassFullyQualifiedName);
                maybeAddImport("org.jvnet.hudson.test.JenkinsRule");
                classDecl = addParamJenkinsRule(classDecl, jenkinsRuleFieldName);
            }
            return super.visitClassDeclaration(classDecl, ctx);
        }

        private J.ClassDeclaration addParamJenkinsRule(J.ClassDeclaration classDecl, String jenkinsRuleFieldName) {
            classDecl = classDecl.withBody(classDecl
                    .getBody()
                    .withStatements(classDecl.getBody().getStatements().stream()
                            .map(statement -> {
                                if (statement instanceof J.MethodDeclaration) {
                                    J.MethodDeclaration methodDecl = (J.MethodDeclaration) statement;
                                    if (methodDecl.getBody() != null
                                            && methodDecl.getBody().getStatements().stream()
                                                    .anyMatch(stmt ->
                                                            stmt.print().contains(jenkinsRuleFieldName + "."))) {
                                        LOG.info(
                                                "JenkinsRule {} parameter added to: {}",
                                                jenkinsRuleFieldName,
                                                methodDecl.getSimpleName());
                                        J.MethodDeclaration finalMethodDecl = methodDecl;
                                        boolean emptyParams = finalMethodDecl.getParameters().stream()
                                                .anyMatch(param -> param instanceof J.Empty);
                                        java.util.List<org.openrewrite.java.tree.Statement> newParams =
                                                new java.util.ArrayList<>();
                                        if (!emptyParams) {
                                            newParams.addAll(finalMethodDecl.getParameters());
                                        }
                                        newParams.add(new J.VariableDeclarations(
                                                Tree.randomId(),
                                                emptyParams ? Space.EMPTY : Space.SINGLE_SPACE,
                                                Markers.EMPTY,
                                                Collections.emptyList(),
                                                Collections.emptyList(),
                                                new J.Identifier(
                                                        Tree.randomId(),
                                                        Space.EMPTY,
                                                        Markers.EMPTY,
                                                        Collections.emptyList(),
                                                        "JenkinsRule",
                                                        JavaType.buildType("org.jvnet.hudson.test.JenkinsRule"),
                                                        null),
                                                null,
                                                Collections.emptyList(),
                                                java.util.Collections.singletonList(new JRightPadded<>(
                                                        new J.VariableDeclarations.NamedVariable(
                                                                Tree.randomId(),
                                                                Space.SINGLE_SPACE,
                                                                Markers.EMPTY,
                                                                new J.Identifier(
                                                                        Tree.randomId(),
                                                                        Space.EMPTY,
                                                                        Markers.EMPTY,
                                                                        java.util.Collections.emptyList(),
                                                                        jenkinsRuleFieldName,
                                                                        JavaType.buildType(
                                                                                "org.jvnet.hudson.test.JenkinsRule"),
                                                                        null),
                                                                java.util.Collections.emptyList(),
                                                                null,
                                                                null),
                                                        Space.EMPTY,
                                                        Markers.EMPTY))));
                                        methodDecl = methodDecl.withParameters(newParams);
                                    }

                                    return methodDecl;
                                }

                                return statement;
                            })
                            .collect(Collectors.toList())));
            return classDecl;
        }
    }
    ;
}
