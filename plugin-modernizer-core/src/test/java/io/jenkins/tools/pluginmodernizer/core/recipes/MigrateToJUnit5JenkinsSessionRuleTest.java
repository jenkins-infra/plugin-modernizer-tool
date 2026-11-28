package io.jenkins.tools.pluginmodernizer.core.recipes;

import static io.jenkins.tools.pluginmodernizer.core.recipes.DeclarativeRecipesTest.collectRewriteTestDependencies;
import static org.openrewrite.java.Assertions.java;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RewriteTest;

/**
 * Regression test for <a href="https://github.com/jenkins-infra/plugin-modernizer-tool/issues/914">#914</a>.
 */
@Execution(ExecutionMode.CONCURRENT)
class MigrateToJUnit5JenkinsSessionRuleTest implements RewriteTest {

    @Test
    void skipsMigrationWhenJenkinsSessionRuleIsPresent() {
        rewriteRun(
                spec -> {
                    var parser = JavaParser.fromJavaVersion().logCompilationWarningsAndErrors(true);
                    collectRewriteTestDependencies().forEach(parser::addClasspathEntry);
                    spec.recipeFromResource(
                                    "/META-INF/rewrite/recipes.yml",
                                    "io.jenkins.tools.pluginmodernizer.MigrateToJUnit5")
                            .parser(parser);
                },
                java("""
                        import org.junit.Rule;
                        import org.junit.Test;
                        import org.jvnet.hudson.test.JenkinsSessionRule;
                        import org.jvnet.hudson.test.JenkinsRule;

                        class SessionRuleTest {
                            @Rule
                            public JenkinsSessionRule sessions = new JenkinsSessionRule();

                            @Test
                            public void restartTest() throws Throwable {
                                sessions.then(j -> {});
                            }
                        }
                        """));
    }
}
