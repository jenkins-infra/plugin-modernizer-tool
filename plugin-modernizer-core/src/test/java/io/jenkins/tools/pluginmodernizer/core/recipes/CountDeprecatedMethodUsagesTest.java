package io.jenkins.tools.pluginmodernizer.core.recipes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.openrewrite.java.Assertions.java;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.openrewrite.test.RewriteTest;

/**
 * Test for {@link CountDeprecatedMethodUsages}.
 */
@Execution(ExecutionMode.CONCURRENT)
public class CountDeprecatedMethodUsagesTest implements RewriteTest {
    @Test
    void shouldReturnDeprecatedMethodUsagesCount() {
        CountDeprecatedMethodUsages recipe = new CountDeprecatedMethodUsages(); // Initialize the recipe object
        rewriteRun(
                spec -> spec.recipe(recipe).expectedCyclesThatMakeChanges(1).cycles(1),
                // language=java
                java(
                        """
                        public class Test {
                            @Deprecated
                            void oldMethod() {}

                            void call() {
                                oldMethod();  // Deprecated usage
                            }
                        }
                        """,
                        """
                        public class Test {
                            @Deprecated
                            void oldMethod() {}

                            void call() {
                                /*~~>*/oldMethod();  // Deprecated usage
                            }
                        }
                """));
        assertEquals(1, recipe.getTotalDeprecatedCount(), "Expected 1 deprecated method usage to be found");
    }

    @Test
    void shouldReturnDeprecatedMethodUsagesCountAcrossMultipleFiles() {
        CountDeprecatedMethodUsages recipe = new CountDeprecatedMethodUsages();
        // language=java
        rewriteRun(
                spec -> spec.recipe(recipe).expectedCyclesThatMakeChanges(1).cycles(1),
                java(
                        """
                        public class A {
                            @Deprecated
                            void oldMethodA() {}

                            void call() {
                                oldMethodA();  // Deprecated usage
                            }
                        }
                        """,
                        """
                        public class A {
                            @Deprecated
                            void oldMethodA() {}

                            void call() {
                                /*~~>*/oldMethodA();  // Deprecated usage
                            }
                        }
                        """),
                java(
                        """
                        public class B {
                            @Deprecated
                            void oldMethodB() {}

                            void call1() {
                                oldMethodB();  // Deprecated usage
                            }

                            void call2() {
                                oldMethodB();  // Deprecated usage
                            }
                        }
                        """,
                        """
                        public class B {
                            @Deprecated
                            void oldMethodB() {}

                            void call1() {
                                /*~~>*/oldMethodB();  // Deprecated usage
                            }

                            void call2() {
                                /*~~>*/oldMethodB();  // Deprecated usage
                            }
                        }
                        """));

        assertEquals(3, recipe.getTotalDeprecatedCount(), "Expected 3 deprecated method usages across all files");
    }

    @Test
    void shouldReturnDeprecatedMethodUsagesCountAsZero() {
        CountDeprecatedMethodUsages recipe = new CountDeprecatedMethodUsages();
        rewriteRun(
                spec -> spec.recipe(recipe),
                // language=java
                java(
                        """
                        public class Test {
                            void newMethod() {}

                            void call() {
                                newMethod(); // Not deprecated usage
                            }
                        }
                        """));
        assertEquals(0, recipe.getTotalDeprecatedCount(), "No deprecated method usage should be found");
    }
}
