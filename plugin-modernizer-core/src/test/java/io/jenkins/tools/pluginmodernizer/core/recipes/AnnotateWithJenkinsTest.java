package io.jenkins.tools.pluginmodernizer.core.recipes;

import static io.jenkins.tools.pluginmodernizer.core.recipes.DeclarativeRecipesTest.collectRewriteTestDependencies;
import static org.openrewrite.java.Assertions.java;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RewriteTest;

/**
 * Test for {@link AnnotateWithJenkins}.
 */
@Execution(ExecutionMode.CONCURRENT)
public class AnnotateWithJenkinsTest implements RewriteTest {
    @Test
    void shouldAddWithJenkinsAnnotationWhenRuleWithJenkinsRuleIsUsedAndPassJenkinsRuleAsParameter() {
        rewriteRun(
                spec -> {
                    var parser = JavaParser.fromJavaVersion().logCompilationWarningsAndErrors(true);
                    collectRewriteTestDependencies().forEach(parser::addClasspathEntry);
                    spec.recipe(new AnnotateWithJenkins())
                            .parser(parser)
                            .expectedCyclesThatMakeChanges(1)
                            .cycles(1);
                },
                // language=java
                java(
                        """
                        import org.junit.Rule;
                        import org.jvnet.hudson.test.JenkinsRule;
                        import org.junit.Test;

                        public class MyTest {
                            @Rule
                            public JenkinsRule jr = new JenkinsRule();

                            @Test
                            public void myTestMethod() {
                                jr.before();
                            }

                            @Test
                            public void myTestMethodWithParam(String str) {
                                jr.before();
                            }
                        }
                        """,
                        """
                        import org.junit.Rule;
                        import org.jvnet.hudson.test.JenkinsRule;
                        import org.jvnet.hudson.test.junit.jupiter.WithJenkins;
                        import org.junit.Test;

                        @WithJenkins
                        public class MyTest {

                            @Test
                            public void myTestMethod(JenkinsRule jr) {
                                jr.before();
                            }

                            @Test
                            public void myTestMethodWithParam(String str, JenkinsRule jr) {
                                jr.before();
                            }
                        }
                        """));
    }

    @Test
    void shouldAddWithJenkinsAnnotationWhenRuleWithJenkinsRuleIsUsedAndPassJenkinsRuleAsParameterInChildAndParent() {
        rewriteRun(
                spec -> {
                    var parser = JavaParser.fromJavaVersion().logCompilationWarningsAndErrors(true);
                    collectRewriteTestDependencies().forEach(parser::addClasspathEntry);
                    spec.recipe(new AnnotateWithJenkins())
                            .parser(parser)
                            .expectedCyclesThatMakeChanges(1)
                            .cycles(1);
                },
                // language=java
                java(
                        """
                        import org.junit.Rule;
                        import org.jvnet.hudson.test.JenkinsRule;
                        import org.junit.Test;

                        public class MyTest {
                            @Rule
                            public JenkinsRule jenkinsRule = new JenkinsRule();

                            @Test
                            public void myTestMethod() {
                                jenkinsRule.before();
                            }

                            @Test
                            public void myTestMethodWithParam(String str) {
                                jenkinsRule.before();
                            }
                        }
                        class MyTestChild extends MyTest {
                            @Test
                            public void myTestMethodChild() {
                                jenkinsRule.before();
                            }

                            @Test
                            public void myTestMethodWithParamChild(String str) {
                                jenkinsRule.before();
                            }
                        }
                        """,
                        """
                        import org.junit.Rule;
                        import org.jvnet.hudson.test.JenkinsRule;
                        import org.jvnet.hudson.test.junit.jupiter.WithJenkins;
                        import org.junit.Test;

                        @WithJenkins
                        public class MyTest {

                            @Test
                            public void myTestMethod(JenkinsRule jenkinsRule) {
                                jenkinsRule.before();
                            }

                            @Test
                            public void myTestMethodWithParam(String str, JenkinsRule jenkinsRule) {
                                jenkinsRule.before();
                            }
                        }
                        class MyTestChild extends MyTest {
                            @Test
                            public void myTestMethodChild(JenkinsRule jenkinsRule) {
                                jenkinsRule.before();
                            }

                            @Test
                            public void myTestMethodWithParamChild(String str, JenkinsRule jenkinsRule) {
                                jenkinsRule.before();
                            }
                        }
                        """));
    }

    @Test
    void shouldAddWithJenkinsAnnotationWhenRuleWithJenkinsRuleIsUsedButNotPassJenkinsRuleAsParameter() {
        rewriteRun(
                spec -> {
                    var parser = JavaParser.fromJavaVersion().logCompilationWarningsAndErrors(true);
                    collectRewriteTestDependencies().forEach(parser::addClasspathEntry);
                    spec.recipe(new AnnotateWithJenkins())
                            .parser(parser)
                            .expectedCyclesThatMakeChanges(1)
                            .cycles(1);
                },
                // language=java
                java(
                        """
                        import org.junit.Rule;
                        import org.jvnet.hudson.test.JenkinsRule;
                        import org.junit.Test;

                        public class MyTest {
                            @Rule
                            public JenkinsRule j = new JenkinsRule();

                            @Test
                            public void myTestMethod() {
                                // j.before();
                            }
                        }
                        """,
                        """
                        import org.junit.Rule;
                        import org.jvnet.hudson.test.JenkinsRule;
                        import org.jvnet.hudson.test.junit.jupiter.WithJenkins;
                        import org.junit.Test;

                        @WithJenkins
                        public class MyTest {

                            @Test
                            public void myTestMethod() {
                                // j.before();
                            }
                        }
                        """));
    }

    @Test
    void shouldNotAddWithJenkinsAnnotationWhenRuleIsNotUsed() {
        rewriteRun(
                spec -> {
                    var parser = JavaParser.fromJavaVersion().logCompilationWarningsAndErrors(true);
                    collectRewriteTestDependencies().forEach(parser::addClasspathEntry);
                    spec.recipe(new AnnotateWithJenkins()).parser(parser);
                },
                // language=java
                java(
                        """
                        import org.junit.Test;

                        public class AnotherTest {
                            @Test
                            public void myTestMethod() {
                                // some test
                            }
                        }
                        """));
    }

    @Test
    void shouldNotAddWithJenkinsAnnotationWhenRuleIsNotJenkinsRule() {
        rewriteRun(
                spec -> {
                    var parser = JavaParser.fromJavaVersion().logCompilationWarningsAndErrors(true);
                    collectRewriteTestDependencies().forEach(parser::addClasspathEntry);
                    spec.recipe(new AnnotateWithJenkins()).parser(parser);
                },
                // language=java
                java(
                        """
                        import org.junit.Rule;
                        import org.junit.rules.TemporaryFolder;
                        import org.junit.Test;

                        public class FolderTest {
                            @Rule
                            public TemporaryFolder folder = new TemporaryFolder();

                            @Test
                            public void myTestMethod() {
                                // some test
                            }
                        }
                        """));
    }
}
