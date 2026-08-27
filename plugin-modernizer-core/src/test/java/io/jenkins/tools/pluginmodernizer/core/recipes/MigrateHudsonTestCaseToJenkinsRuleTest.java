package io.jenkins.tools.pluginmodernizer.core.recipes;

import static io.jenkins.tools.pluginmodernizer.core.recipes.DeclarativeRecipesTest.collectRewriteTestDependencies;
import static org.openrewrite.java.Assertions.java;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RewriteTest;

/**
 * Tests for {@link MigrateHudsonTestCaseToJenkinsRule}.
 */
@Execution(ExecutionMode.CONCURRENT)
class MigrateHudsonTestCaseToJenkinsRuleTest implements RewriteTest {

    @Test
    void shouldRemoveExtendsAndAddRuleField() {
        rewriteRun(
                spec -> {
                    var parser = JavaParser.fromJavaVersion().logCompilationWarningsAndErrors(true);
                    collectRewriteTestDependencies().forEach(parser::addClasspathEntry);
                    spec.recipe(new MigrateHudsonTestCaseToJenkinsRule())
                            .parser(parser)
                            .expectedCyclesThatMakeChanges(1)
                            .cycles(1);
                },
                // language=java
                java("""
                        import org.jvnet.hudson.test.HudsonTestCase;

                        public class MyPluginTest extends HudsonTestCase {

                            public void testSomething() throws Exception {
                                // test body
                            }
                        }
                        """, """
                        import org.junit.Rule;
                        import org.junit.Test;
                        import org.jvnet.hudson.test.JenkinsRule;

                        public class MyPluginTest {

                            @Rule
                            public JenkinsRule j = new JenkinsRule();

                            @Test
                            public void testSomething() throws Exception {
                                // test body
                            }
                        }
                        """));
    }

    @Test
    void shouldConvertSetUpAndTearDownMethods() {
        rewriteRun(
                spec -> {
                    var parser = JavaParser.fromJavaVersion().logCompilationWarningsAndErrors(true);
                    collectRewriteTestDependencies().forEach(parser::addClasspathEntry);
                    spec.recipe(new MigrateHudsonTestCaseToJenkinsRule())
                            .parser(parser)
                            .expectedCyclesThatMakeChanges(1)
                            .cycles(1);
                },
                // language=java
                java("""
                        import org.jvnet.hudson.test.HudsonTestCase;

                        public class MyPluginTest extends HudsonTestCase {

                            @Override
                            protected void setUp() throws Exception {
                                super.setUp();
                                System.out.println("custom setup");
                            }

                            @Override
                            protected void tearDown() throws Exception {
                                super.tearDown();
                                System.out.println("custom teardown");
                            }

                            public void testSomething() throws Exception {
                                // test body
                            }
                        }
                        """, """
                        import org.junit.After;
                        import org.junit.Before;
                        import org.junit.Rule;
                        import org.junit.Test;
                        import org.jvnet.hudson.test.JenkinsRule;

                        public class MyPluginTest {

                            @Rule
                            public JenkinsRule j = new JenkinsRule();

                            @Before
                            public void setUp() throws Exception {
                                System.out.println("custom setup");
                            }

                            @After
                            public void tearDown() throws Exception {
                                System.out.println("custom teardown");
                            }

                            @Test
                            public void testSomething() throws Exception {
                                // test body
                            }
                        }
                        """));
    }

    @Test
    void shouldPrefixDelegatedMethodCallsWithJ() {
        rewriteRun(
                spec -> {
                    var parser = JavaParser.fromJavaVersion().logCompilationWarningsAndErrors(true);
                    collectRewriteTestDependencies().forEach(parser::addClasspathEntry);
                    spec.recipe(new MigrateHudsonTestCaseToJenkinsRule())
                            .parser(parser)
                            .expectedCyclesThatMakeChanges(1)
                            .cycles(1);
                },
                // language=java
                java("""
                        import hudson.model.FreeStyleProject;
                        import hudson.model.FreeStyleBuild;
                        import org.jvnet.hudson.test.HudsonTestCase;

                        public class MyPluginTest extends HudsonTestCase {

                            public void testBuild() throws Exception {
                                FreeStyleProject p = createFreeStyleProject();
                                FreeStyleBuild b = buildAndAssertSuccess(p);
                                assertLogContains("SUCCESS", b);
                            }
                        }
                        """, """
                        import hudson.model.FreeStyleProject;
                        import org.junit.Rule;
                        import org.junit.Test;
                        import org.jvnet.hudson.test.JenkinsRule;
                        import hudson.model.FreeStyleBuild;

                        public class MyPluginTest {

                            @Rule
                            public JenkinsRule j = new JenkinsRule();

                            @Test
                            public void testBuild() throws Exception {
                                FreeStyleProject p = j.createFreeStyleProject();
                                FreeStyleBuild b = j.buildAndAssertSuccess(p);
                                j.assertLogContains("SUCCESS", b);
                            }
                        }
                        """));
    }

    @Test
    void shouldNotModifyClassesWithoutHudsonTestCase() {
        rewriteRun(
                spec -> {
                    var parser = JavaParser.fromJavaVersion().logCompilationWarningsAndErrors(true);
                    collectRewriteTestDependencies().forEach(parser::addClasspathEntry);
                    spec.recipe(new MigrateHudsonTestCaseToJenkinsRule())
                            .parser(parser)
                            .cycles(1);
                },
                // language=java
                java("""
                        import org.junit.Rule;
                        import org.junit.Test;
                        import org.jvnet.hudson.test.JenkinsRule;

                        public class AlreadyModernTest {

                            @Rule
                            public JenkinsRule j = new JenkinsRule();

                            @Test
                            public void testSomething() throws Exception {
                                // already modern — no changes expected
                            }
                        }
                        """));
    }

    @Test
    void shouldHandleSetUpWithoutSuperCall() {
        rewriteRun(
                spec -> {
                    var parser = JavaParser.fromJavaVersion().logCompilationWarningsAndErrors(true);
                    collectRewriteTestDependencies().forEach(parser::addClasspathEntry);
                    spec.recipe(new MigrateHudsonTestCaseToJenkinsRule())
                            .parser(parser)
                            .expectedCyclesThatMakeChanges(1)
                            .cycles(1);
                },
                // language=java
                java("""
                        import org.jvnet.hudson.test.HudsonTestCase;

                        public class MyPluginTest extends HudsonTestCase {

                            @Override
                            protected void setUp() throws Exception {
                                System.out.println("no super call here");
                            }

                            public void testSomething() {}
                        }
                        """, """
                        import org.junit.Before;
                        import org.junit.Rule;
                        import org.junit.Test;
                        import org.jvnet.hudson.test.JenkinsRule;

                        public class MyPluginTest {

                            @Rule
                            public JenkinsRule j = new JenkinsRule();

                            @Before
                            public void setUp() throws Exception {
                                System.out.println("no super call here");
                            }

                            @Test
                            public void testSomething() {}
                        }
                        """));
    }
}
