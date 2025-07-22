package io.jenkins.tools.pluginmodernizer.core.recipes;

import static org.openrewrite.java.Assertions.java;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.openrewrite.test.RewriteTest;

/**
 * Test for {@link AddThrowsExceptionWhereAssertThrows}.
 */
@Execution(ExecutionMode.CONCURRENT)
public class AddThrowsExceptionWhereAssertThrowsTest implements RewriteTest {

    @Test
    void shouldAddThrowsExceptionToTestMethodWhenUsingAssertThrows() {
        rewriteRun(
                spec -> spec.recipe(new AddThrowsExceptionWhereAssertThrows()),
                // language=java
                java(
                        """
                    import static org.junit.jupiter.api.Assertions.assertThrows;
                    import org.junit.jupiter.api.Test;

                    public class MyTest {
                        @Test
                        public void testSomething() {
                            assertThrows(Exception.class, () -> {
                                //someMethod()
                             });
                        }
                    }
                    """,
                        """
                    import static org.junit.jupiter.api.Assertions.assertThrows;
                    import org.junit.jupiter.api.Test;

                    public class MyTest {
                        @Test
                        public void testSomething()throws Exception {
                            assertThrows(Exception.class, () -> {
                                //someMethod()
                             });
                        }
                    }
                    """));
    }

    @Test
    void shouldAddThrowsExceptionForQualifiedAssertThrows() {
        rewriteRun(
                spec -> spec.recipe(new AddThrowsExceptionWhereAssertThrows()),
                // language=java
                java(
                        """
                        import org.junit.jupiter.api.Assertions;
                        import org.junit.jupiter.api.Test;

                        public class MyTest {
                            @Test
                            public void testSomething() {
                                Assertions.assertThrows(Exception.class, () -> {
                                  //someMethod();
                                });
                            }
                        }
                        """,
                        """
                        import org.junit.jupiter.api.Assertions;
                        import org.junit.jupiter.api.Test;

                        public class MyTest {
                            @Test
                            public void testSomething()throws Exception {
                                Assertions.assertThrows(Exception.class, () -> {
                                  //someMethod();
                                });
                            }
                        }
                        """));
    }

    @Test
    void shouldNotAddThrowsExceptionIfAlreadyPresent() {
        rewriteRun(
                spec -> spec.recipe(new AddThrowsExceptionWhereAssertThrows()),
                // language=java
                java(
                        """
                        import static org.junit.jupiter.api.Assertions.assertThrows;
                        import org.junit.jupiter.api.Test;

                        public class MyTest {
                            @Test
                            public void testSomething() throws Exception {
                                assertThrows(Exception.class, () -> {
                                  //someMethod();
                                });
                            }
                        }
                        """));
    }

    @Test
    void shouldNotAddThrowsExceptionIfNoAssertThrows() {
        rewriteRun(
                spec -> spec.recipe(new AddThrowsExceptionWhereAssertThrows()),
                // language=java
                java(
                        """
                        import org.junit.jupiter.api.Test;

                        public class MyTest {
                            @Test
                            public void testSomething() {
                                // No assertThrows
                            }
                        }
                        """));
    }

    @Test
    void shouldAddThrowsExceptionAlongWithExistingThrowsByReplacingIt() {
        rewriteRun(
                spec -> spec.recipe(new AddThrowsExceptionWhereAssertThrows()),
                // language=java
                java(
                        """
                        import static org.junit.jupiter.api.Assertions.assertThrows;
                        import org.junit.jupiter.api.Test;
                        import java.io.IOException;

                        public class MyTest {
                            @Test
                            public void testSomething() throws IOException {
                                assertThrows(Exception.class, () -> {
                                  //someMethod();
                                });
                            }
                        }
                        """,
                        """
                        import static org.junit.jupiter.api.Assertions.assertThrows;
                        import org.junit.jupiter.api.Test;
                        import java.io.IOException;

                        public class MyTest {
                            @Test
                            public void testSomething() throws Exception {
                                assertThrows(Exception.class, () -> {
                                  //someMethod();
                                });
                            }
                        }
                        """));
    }
}
