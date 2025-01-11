package io.jenkins.tools.pluginmodernizer.core.recipes;

import static org.openrewrite.test.SourceSpecs.text;
import static org.openrewrite.yaml.Assertions.yaml;

import io.jenkins.tools.pluginmodernizer.core.extractor.ArchetypeCommonFile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.openrewrite.test.RewriteTest;

/**
 * Test for {@link SetupDependabot}.
 */
@Execution(ExecutionMode.CONCURRENT)
public class SetupDependabotTest implements RewriteTest {

    @Test
    void shouldAddDependabot() {
        rewriteRun(
                spec -> spec.recipe(new SetupDependabot()),
                // language=yaml
                text(""), // Need one minimum file to trigger the recipe
                yaml(
                        null,
                        """
                    version: 2
                    updates:
                    - package-ecosystem: maven
                      directory: /
                      schedule:
                        interval: monthly
                    - package-ecosystem: github-actions
                      directory: /
                      schedule:
                        interval: monthly
                    """,
                        sourceSpecs -> {
                            sourceSpecs.path(ArchetypeCommonFile.DEPENDABOT.getPath());
                        }));
    }

    /**
     * Note this test need to be adapted to fix the dependabot config
     * (For example to reduce frequency or increase frequency for API plugins)
     */
    @Test
    void shouldNotAddDependabotIfRenovateConfigured() {
        rewriteRun(
                spec -> spec.recipe(new SetupDependabot()),
                text(""), // Need one minimum file to trigger the recipe
                text("{}", sourceSpecs -> {
                    sourceSpecs.path(ArchetypeCommonFile.RENOVATE.getPath());
                }));
    }

    /**
     * Note this test need to be adapted to fix the dependabot config
     * (For example to reduce frequency or increase frequency for API plugins)
     */
    @Test
    void shouldNotChangeDependabotIfAlreadyExists() {
        rewriteRun(
                spec -> spec.recipe(new SetupDependabot()),
                text(""), // Need one minimum file to trigger the recipe
                // language=yaml
                yaml(
                        """
                    ---
                    version: 2
                    """,
                        sourceSpecs -> {
                            sourceSpecs.path(ArchetypeCommonFile.DEPENDABOT.getPath());
                        }));
    }
}
