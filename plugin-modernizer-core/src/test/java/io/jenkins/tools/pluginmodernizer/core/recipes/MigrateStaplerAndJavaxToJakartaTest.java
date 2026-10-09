package io.jenkins.tools.pluginmodernizer.core.recipes;

import static io.jenkins.tools.pluginmodernizer.core.recipes.DeclarativeRecipesTest.collectRewriteTestDependencies;
import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.java.Assertions.srcMainResources;
import static org.openrewrite.java.Assertions.srcTestJava;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RewriteTest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Test for {@link MigrateStaplerAndJavaxToJakarta}.
 */
@Execution(ExecutionMode.CONCURRENT)
public class MigrateStaplerAndJavaxToJakartaTest implements RewriteTest {

    /**
     * LOGGER.
     */
    private static final Logger LOG = LoggerFactory.getLogger(MigrateStaplerAndJavaxToJakartaTest.class);

    @Test
    void migrateStaplerAndJavaxToJakartaAsChartUtilIsNotUsed() {
        rewriteRun(
                spec -> {
                    var parser = JavaParser.fromJavaVersion().logCompilationWarningsAndErrors(true);
                    collectRewriteTestDependencies().forEach(parser::addClasspathEntry);
                    spec.recipe(new MigrateStaplerAndJavaxToJakarta()).parser(parser);
                },
                srcMainResources(
                        // language=java
                        java("""
                        import javax.servlet.ServletException;
                        import org.kohsuke.stapler.Stapler;
                        import org.kohsuke.stapler.StaplerRequest;
                        import org.kohsuke.stapler.StaplerResponse;

                        public class Foo {
                            public void foo() {
                                StaplerRequest req = Stapler.getCurrentRequest();
                                StaplerResponse response = Stapler.getCurrentResponse();
                            }
                        }
                        """, """
                        import jakarta.servlet.ServletException;
                        import org.kohsuke.stapler.Stapler;
                        import org.kohsuke.stapler.StaplerRequest2;
                        import org.kohsuke.stapler.StaplerResponse2;

                        public class Foo {
                            public void foo() {
                                StaplerRequest2 req = Stapler.getCurrentRequest2();
                                StaplerResponse2 response = Stapler.getCurrentResponse2();
                            }
                        }
                        """)));
    }

    /**
     * Regression test for <a href="https://github.com/jenkins-infra/plugin-modernizer-tool/issues/826">#826</a>.
     */
    @Test
    void migrateMockitoHttpSessionInTestCode() {
        rewriteRun(
                spec -> {
                    var parser = JavaParser.fromJavaVersion().logCompilationWarningsAndErrors(true);
                    collectRewriteTestDependencies().forEach(parser::addClasspathEntry);
                    spec.recipe(new MigrateStaplerAndJavaxToJakarta()).parser(parser);
                },
                srcTestJava(
                        // language=java
                        java("""
                                import javax.servlet.http.HttpSession;
                                import org.kohsuke.stapler.StaplerRequest;

                                import static org.mockito.Mockito.*;

                                public class TuleapAuthorizationCodeUrlBuilderImplTest {
                                    public void testItShouldReturnTheAuthorizationCodeUriWithTheRightParameters() {
                                        StaplerRequest request = mock(StaplerRequest.class);
                                        HttpSession session = spy(HttpSession.class);
                                        when(request.getSession()).thenReturn(session);
                                    }
                                }
                                """, """
                                import jakarta.servlet.http.HttpSession;
                                import org.kohsuke.stapler.StaplerRequest2;

                                import static org.mockito.Mockito.*;

                                public class TuleapAuthorizationCodeUrlBuilderImplTest {
                                    public void testItShouldReturnTheAuthorizationCodeUriWithTheRightParameters() {
                                        StaplerRequest2 request = mock(StaplerRequest2.class);
                                        HttpSession session = spy(HttpSession.class);
                                        when(request.getSession()).thenReturn(session);
                                    }
                                }
                                """)));
    }

    /**
     * Regression test for <a href="https://github.com/jenkins-infra/plugin-modernizer-tool/issues/826">#826</a>
     * when javax servlet types are fully qualified.
     */
    @Test
    void migrateMockitoHttpSessionWithFullyQualifiedTypeNames() {
        rewriteRun(
                spec -> {
                    var parser = JavaParser.fromJavaVersion().logCompilationWarningsAndErrors(true);
                    collectRewriteTestDependencies().forEach(parser::addClasspathEntry);
                    spec.recipe(new MigrateStaplerAndJavaxToJakarta()).parser(parser);
                },
                srcTestJava(
                        // language=java
                        java("""
                                import org.kohsuke.stapler.StaplerRequest;

                                import static org.mockito.Mockito.*;

                                public class TuleapAuthorizationCodeUrlBuilderImplTest {
                                    public void testItShouldReturnTheAuthorizationCodeUriWithTheRightParameters() {
                                        StaplerRequest request = mock(StaplerRequest.class);
                                        javax.servlet.http.HttpSession session = spy(javax.servlet.http.HttpSession.class);
                                        when(request.getSession()).thenReturn(session);
                                    }
                                }
                                """, """
                                import org.kohsuke.stapler.StaplerRequest2;

                                import static org.mockito.Mockito.*;

                                public class TuleapAuthorizationCodeUrlBuilderImplTest {
                                    public void testItShouldReturnTheAuthorizationCodeUriWithTheRightParameters() {
                                        StaplerRequest2 request = mock(StaplerRequest2.class);
                                        jakarta.servlet.http.HttpSession session = spy(jakarta.servlet.http.HttpSession.class);
                                        when(request.getSession()).thenReturn(session);
                                    }
                                }
                                """)));
    }

    @Test
    void notMigrateStaplerAndJavaxToJakartaAsChartUtilIsUsed() {
        rewriteRun(
                spec -> {
                    var parser = JavaParser.fromJavaVersion().logCompilationWarningsAndErrors(true);
                    collectRewriteTestDependencies().forEach(parser::addClasspathEntry);
                    spec.recipe(new MigrateStaplerAndJavaxToJakarta()).parser(parser);
                },
                srcTestJava(
                        // language=java
                        java("""
                        package hudson.util;
                        public class ChartUtil {}
                        """)),
                srcMainResources(
                        // language=java
                        java("""
                        import javax.servlet.ServletException;
                        import org.kohsuke.stapler.Stapler;
                        import org.kohsuke.stapler.StaplerRequest;
                        import org.kohsuke.stapler.StaplerResponse;
                        import hudson.util.ChartUtil;

                        public class Foo {
                            public void foo() {
                                StaplerRequest req = Stapler.getCurrentRequest();
                                StaplerResponse response = Stapler.getCurrentResponse();
                            }
                        }
                        """)));
    }
}
