package io.jenkins.tools.pluginmodernizer.core.recipes.code;

import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.java.Assertions.srcTestJava;

import io.jenkins.tools.pluginmodernizer.core.recipes.DeclarativeRecipesTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RewriteTest;

/**
 * Test for {@link ReplaceRemovedSSHLauncherConstructor}
 */
@Execution(ExecutionMode.CONCURRENT)
public class ReplaceRemovedSSHLauncherConstructorTest implements RewriteTest {

    @Language("java")
    public static final String BEFORE =
            """
            import hudson.plugins.sshslaves.SSHLauncher;

            public class Foo {
                public void foo() {
                    SSHLauncher launcher = new SSHLauncher("127.0.0.1", 22, "username", "password", "privatekey", "jvmOptions");
                }
            }
            """;

    @Language("java")
    public static final String AFTER =
            """
            import hudson.plugins.sshslaves.SSHLauncher;

            public class Foo {
                public void foo() {
                    SSHLauncher launcher = new SSHLauncher("127.0.0.1", 22, null);
                }
            }
            """;

    @Test
    void replaceConstructor() {
        rewriteRun(
                spec -> {
                    var parser = JavaParser.fromJavaVersion().logCompilationWarningsAndErrors(true);
                    DeclarativeRecipesTest.collectRewriteTestDependencies().stream()
                            .filter(entry -> entry.getFileName().toString().contains("ssh-slaves-1.12"))
                            .forEach(parser::addClasspathEntry);
                    spec.recipe(new ReplaceRemovedSSHLauncherConstructor()).parser(parser);
                },
                // language=java
                srcTestJava(java(BEFORE, AFTER)));
    }

    @Test
    void keepConstructor() {
        rewriteRun(
                spec -> {
                    var parser = JavaParser.fromJavaVersion().logCompilationWarningsAndErrors(true);
                    DeclarativeRecipesTest.collectRewriteTestDependencies().stream()
                            .filter(entry ->
                                    entry.getFileName().toString().contains("ssh-slaves-3.1021.va_cc11b_de26a_e"))
                            .forEach(parser::addClasspathEntry);
                    spec.recipe(new ReplaceRemovedSSHLauncherConstructor()).parser(parser);
                },
                // language=java
                srcTestJava(java(AFTER)));
    }
}
