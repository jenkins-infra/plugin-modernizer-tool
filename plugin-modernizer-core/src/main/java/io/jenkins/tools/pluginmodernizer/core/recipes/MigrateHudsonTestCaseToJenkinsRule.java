package io.jenkins.tools.pluginmodernizer.core.recipes;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.openrewrite.ExecutionContext;
import org.openrewrite.ScanningRecipe;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.Space;
import org.openrewrite.java.tree.Statement;
import org.openrewrite.marker.Markers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Migrates JUnit 3-style Jenkins test classes that extend {@code HudsonTestCase}
 * to the modern JUnit 4 {@code @Rule JenkinsRule} pattern.
 *
 * <p>Transformations applied:
 * <ul>
 *   <li>Removes {@code extends HudsonTestCase}</li>
 *   <li>Inserts {@code @Rule public JenkinsRule j = new JenkinsRule();} field</li>
 *   <li>Converts {@code setUp()} to {@code @Before public void setUp()}, removing {@code super.setUp()}</li>
 *   <li>Converts {@code tearDown()} to {@code @After public void tearDown()}, removing {@code super.tearDown()}</li>
 *   <li>Annotates JUnit 3-style {@code public void test*()} methods with {@code @Test}</li>
 *   <li>Prefixes known delegated {@code HudsonTestCase} method calls with {@code j.}</li>
 * </ul>
 */
public class MigrateHudsonTestCaseToJenkinsRule extends ScanningRecipe<Set<String>> {

    private static final Logger LOG = LoggerFactory.getLogger(MigrateHudsonTestCaseToJenkinsRule.class);

    static final String HUDSON_TEST_CASE_FQN = "org.jvnet.hudson.test.HudsonTestCase";
    static final String JENKINS_RULE_FQN = "org.jvnet.hudson.test.JenkinsRule";

    /**
     * Known methods inherited from HudsonTestCase that must be prefixed with {@code j.}
     * after the migration to a JenkinsRule field.
     */
    static final Set<String> DELEGATED_METHODS = Set.of(
            "assertBuildStatus",
            "assertBuildStatusSuccess",
            "assertLogContains",
            "assertLogNotContains",
            "buildAndAssertStatus",
            "buildAndAssertSuccess",
            "configRoundtrip",
            "createComputerLauncher",
            "createDumbSlave",
            "createFreeStyleProject",
            "createOnlineSlave",
            "createSlave",
            "getURL",
            "waitUntilNoActivity");

    @Override
    public String getDisplayName() {
        return "Migrate HudsonTestCase to JenkinsRule";
    }

    @Override
    public String getDescription() {
        return "Migrates JUnit 3-style Jenkins test classes that extend HudsonTestCase "
                + "to use the modern JUnit 4 @Rule JenkinsRule pattern.";
    }

    @Override
    public Set<String> getInitialValue(ExecutionContext ctx) {
        return new HashSet<>();
    }

    private static Path findJar(String mavenRelativePath, String filenamePrefix) {
        // Try target/openrewrite-jars first (populated during test builds)
        try {
            Path openrewriteJars = Path.of("target/openrewrite-jars");
            if (Files.isDirectory(openrewriteJars)) {
                Optional<Path> found = Files.list(openrewriteJars)
                        .filter(p -> p.getFileName().toString().startsWith(filenamePrefix))
                        .findFirst();
                if (found.isPresent()) {
                    return found.get();
                }
            }
        } catch (IOException ignored) {
        }
        // Fall back to Maven local repository (available during production rewrite:run)
        Path m2Path =
                Path.of(System.getProperty("user.home"), ".m2", "repository").resolve(mavenRelativePath);
        try {
            Optional<Path> found = Files.walk(m2Path)
                    .filter(p -> {
                        String name = p.getFileName().toString();
                        return name.startsWith(filenamePrefix)
                                && name.endsWith(".jar")
                                && !name.contains("sources")
                                && !name.contains("javadoc");
                    })
                    .findFirst();
            if (found.isPresent()) {
                return found.get();
            }
        } catch (IOException ignored) {
        }
        throw new IllegalStateException(
                "Cannot find JAR with prefix '" + filenamePrefix + "' in target/openrewrite-jars or ~/.m2/repository");
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(Set<String> acc) {
        return new JavaIsoVisitor<>() {
            @Override
            public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext ctx) {
                if (classDecl.getExtends() != null && classDecl.getType() != null) {
                    JavaType extendsType = classDecl.getExtends().getType();
                    if (extendsType instanceof JavaType.FullyQualified fq
                            && HUDSON_TEST_CASE_FQN.equals(fq.getFullyQualifiedName())) {
                        acc.add(classDecl.getType().getFullyQualifiedName());
                        LOG.info(
                                "Found HudsonTestCase subclass: {}",
                                classDecl.getType().getFullyQualifiedName());
                    }
                }
                return super.visitClassDeclaration(classDecl, ctx);
            }
        };
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(Set<String> acc) {
        // Template to insert "@Rule public JenkinsRule j = new JenkinsRule();" as the
        // first class member.  Using JavaTemplate guarantees full type attribution on
        // the generated NewClass node, which OpenRewrite's LST validator requires.
        final JavaTemplate jenkinsRuleFieldTemplate = JavaTemplate.builder(
                        "@Rule\npublic JenkinsRule j = new JenkinsRule();")
                .imports("org.junit.Rule", JENKINS_RULE_FQN)
                .javaParser(JavaParser.fromJavaVersion()
                        .addClasspathEntry(findJar("org/jenkins-ci/main/jenkins-test-harness", "jenkins-test-harness"))
                        .addClasspathEntry(findJar("junit/junit", "junit-4")))
                .build();

        return new JavaIsoVisitor<>() {

            @Override
            public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext ctx) {
                if (classDecl.getType() == null
                        || !acc.contains(classDecl.getType().getFullyQualifiedName())) {
                    return super.visitClassDeclaration(classDecl, ctx);
                }

                // Remove extends HudsonTestCase
                classDecl = classDecl.withExtends(null);
                maybeRemoveImport(HUDSON_TEST_CASE_FQN);
                maybeAddImport(JENKINS_RULE_FQN);
                maybeAddImport("org.junit.Rule");

                // Add @Rule public JenkinsRule j = new JenkinsRule() if not already present
                if (!hasJenkinsRuleField(classDecl)) {
                    classDecl = jenkinsRuleFieldTemplate.apply(
                            updateCursor(classDecl),
                            classDecl.getBody().getCoordinates().firstStatement());
                    LOG.info("Added @Rule JenkinsRule j field to {}", classDecl.getSimpleName());
                }

                return super.visitClassDeclaration(classDecl, ctx);
            }

            @Override
            public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext ctx) {
                J.ClassDeclaration enclosing = getCursor().firstEnclosing(J.ClassDeclaration.class);
                if (enclosing == null
                        || enclosing.getType() == null
                        || !acc.contains(enclosing.getType().getFullyQualifiedName())) {
                    return super.visitMethodDeclaration(method, ctx);
                }

                if ("setUp".equals(method.getSimpleName()) && isNoArgMethod(method)) {
                    method = migrateLifecycleMethod(method, "Before", "org.junit.Before", "setUp");
                } else if ("tearDown".equals(method.getSimpleName()) && isNoArgMethod(method)) {
                    method = migrateLifecycleMethod(method, "After", "org.junit.After", "tearDown");
                } else if (isJUnit3TestMethod(method)) {
                    method = addLeadingAnnotation(method, "Test", "org.junit.Test");
                    maybeAddImport("org.junit.Test");
                }

                return super.visitMethodDeclaration(method, ctx);
            }

            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                J.ClassDeclaration enclosing = getCursor().firstEnclosing(J.ClassDeclaration.class);
                if (enclosing == null
                        || enclosing.getType() == null
                        || !acc.contains(enclosing.getType().getFullyQualifiedName())) {
                    return super.visitMethodInvocation(method, ctx);
                }

                // Prefix unqualified calls to known HudsonTestCase methods with j.
                if (method.getSelect() == null && DELEGATED_METHODS.contains(method.getSimpleName())) {
                    J.Identifier jRef = new J.Identifier(
                            Tree.randomId(),
                            Space.EMPTY,
                            Markers.EMPTY,
                            Collections.emptyList(),
                            "j",
                            JavaType.buildType(JENKINS_RULE_FQN),
                            null);
                    method = method.withSelect(jRef);
                    LOG.debug("Prefixed {} with j.", method.getSimpleName());
                }

                return super.visitMethodInvocation(method, ctx);
            }

            // -----------------------------------------------------------------------
            // Helpers
            // -----------------------------------------------------------------------

            private boolean hasJenkinsRuleField(J.ClassDeclaration classDecl) {
                return classDecl.getBody().getStatements().stream()
                        .anyMatch(stmt -> stmt instanceof J.VariableDeclarations vd
                                && vd.getTypeExpression() != null
                                && vd.getTypeExpression().getType() instanceof JavaType.FullyQualified fq
                                && JENKINS_RULE_FQN.equals(fq.getFullyQualifiedName()));
            }

            private boolean isNoArgMethod(J.MethodDeclaration method) {
                return method.getParameters().stream().allMatch(p -> p instanceof J.Empty);
            }

            private boolean isJUnit3TestMethod(J.MethodDeclaration method) {
                return method.getSimpleName().startsWith("test")
                        && isVoidReturn(method)
                        && method.getLeadingAnnotations().stream().noneMatch(a -> "Test".equals(a.getSimpleName()));
            }

            private boolean isVoidReturn(J.MethodDeclaration method) {
                return method.getReturnTypeExpression() instanceof J.Primitive p
                        && p.getType() == JavaType.Primitive.Void;
            }

            private boolean isSuperLifecycleCall(Statement stmt, String name) {
                return stmt instanceof J.MethodInvocation mi
                        && mi.getSelect() instanceof J.Identifier sel
                        && "super".equals(sel.getSimpleName())
                        && name.equals(mi.getSimpleName());
            }

            /**
             * Converts a JUnit 3 lifecycle method (setUp/tearDown) to its JUnit 4 equivalent.
             * Removes {@code @Override}, promotes visibility to {@code public}, adds the
             * lifecycle annotation, and strips the {@code super.methodName()} call.
             */
            private J.MethodDeclaration migrateLifecycleMethod(
                    J.MethodDeclaration method, String annotationName, String annotationFqn, String superMethodName) {

                // Remove @Override
                method = method.withLeadingAnnotations(method.getLeadingAnnotations().stream()
                        .filter(a -> !"Override".equals(a.getSimpleName()))
                        .collect(Collectors.toList()));

                // Promote protected → public
                boolean hasPublic = method.getModifiers().stream().anyMatch(m -> m.getType() == J.Modifier.Type.Public);
                if (!hasPublic) {
                    List<J.Modifier> mods = method.getModifiers().stream()
                            .map(m -> m.getType() == J.Modifier.Type.Protected ? m.withType(J.Modifier.Type.Public) : m)
                            .collect(Collectors.toList());
                    if (mods.stream().noneMatch(m -> m.getType() == J.Modifier.Type.Public)) {
                        mods = new ArrayList<>(mods);
                        mods.add(
                                0,
                                new J.Modifier(
                                        Tree.randomId(),
                                        Space.EMPTY,
                                        Markers.EMPTY,
                                        null,
                                        J.Modifier.Type.Public,
                                        Collections.emptyList()));
                    }
                    method = method.withModifiers(mods);
                }

                // Add @Before / @After annotation
                maybeAddImport(annotationFqn);
                method = addLeadingAnnotation(method, annotationName, annotationFqn);

                // Remove super.setUp() / super.tearDown() from body
                if (method.getBody() != null) {
                    List<Statement> filtered = method.getBody().getStatements().stream()
                            .filter(stmt -> !isSuperLifecycleCall(stmt, superMethodName))
                            .collect(Collectors.toList());
                    method = method.withBody(method.getBody().withStatements(filtered));
                }

                LOG.info("Migrated {}() to @{}", superMethodName, annotationName);
                return method;
            }

            private J.MethodDeclaration addLeadingAnnotation(J.MethodDeclaration method, String name, String fqn) {
                boolean hadNoAnnotations = method.getLeadingAnnotations().isEmpty();
                J.Annotation annotation = buildSimpleAnnotation(name, fqn, Space.EMPTY);
                List<J.Annotation> annotations = new ArrayList<>(method.getLeadingAnnotations());
                annotations.add(annotation);
                method = method.withLeadingAnnotations(annotations);
                // When the method had no prior annotations, the first modifier's prefix was
                // relative to the method's own prefix (typically Space.EMPTY).  After an
                // annotation is prepended the modifier must start on a new line with the same
                // indentation as the method itself.
                if (hadNoAnnotations && !method.getModifiers().isEmpty()) {
                    String indent = extractIndentFromPrefix(method.getPrefix().getWhitespace());
                    List<J.Modifier> mods = new ArrayList<>(method.getModifiers());
                    mods.set(0, mods.get(0).withPrefix(Space.build("\n" + indent, Collections.emptyList())));
                    method = method.withModifiers(mods);
                }
                return method;
            }

            private static String extractIndentFromPrefix(String whitespace) {
                int lastNewline = whitespace.lastIndexOf('\n');
                return lastNewline >= 0 ? whitespace.substring(lastNewline + 1) : "    ";
            }

            private J.Annotation buildSimpleAnnotation(String simpleName, String fqn, Space prefix) {
                return new J.Annotation(
                        Tree.randomId(),
                        prefix,
                        Markers.EMPTY,
                        new J.Identifier(
                                Tree.randomId(),
                                Space.EMPTY,
                                Markers.EMPTY,
                                Collections.emptyList(),
                                simpleName,
                                JavaType.buildType(fqn),
                                null),
                        null);
            }
        };
    }
}
