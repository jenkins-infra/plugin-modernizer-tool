package io.jenkins.tools.pluginmodernizer.core.recipes;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.openrewrite.ExecutionContext;
import org.openrewrite.ScanningRecipe;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.AnnotationMatcher;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;
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
                            acc.add(classDecl.getSimpleName());
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

            if (acc.contains(classDecl.getSimpleName())) {
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
            }
            return super.visitClassDeclaration(classDecl, ctx);
        }
    }
    ;
}
