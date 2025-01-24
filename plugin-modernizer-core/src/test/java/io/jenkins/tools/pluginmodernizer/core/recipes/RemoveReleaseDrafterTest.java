package io.jenkins.tools.pluginmodernizer.core.recipes;

import static org.openrewrite.yaml.Assertions.yaml;

import io.jenkins.tools.pluginmodernizer.core.extractor.ArchetypeCommonFile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.openrewrite.test.RewriteTest;

/**
 * Test for {@link RemoveReleaseDrafter}.
 */
@Execution(ExecutionMode.CONCURRENT)
public class RemoveReleaseDrafterTest implements RewriteTest {

    @Test
    void shouldNotRemoveReleaseDrafter() {
        rewriteRun(
                spec -> spec.recipe(new RemoveReleaseDrafter()),
                // language=yaml
                yaml("{}", sourceSpecs -> {
                    sourceSpecs.path(ArchetypeCommonFile.RELEASE_DRAFTER.getPath());
                }),
                yaml("{}", sourceSpecs -> {
                    sourceSpecs.path(ArchetypeCommonFile.RELEASE_DRAFTER_WORKFLOW.getPath());
                }));
    }

    @Test
    void shouldRemoveReleaseDrafterIfContinuousDeliveryEnabled() {
        rewriteRun(
                spec -> spec.recipe(new RemoveReleaseDrafter()),
                // language=yaml
                yaml("{}", sourceSpecs -> {
                    sourceSpecs.path(ArchetypeCommonFile.WORKFLOW_CD.getPath());
                }),
                yaml("{}", null, sourceSpecs -> {
                    sourceSpecs.path(ArchetypeCommonFile.RELEASE_DRAFTER.getPath());
                }),
                yaml("{}", null, sourceSpecs -> {
                    sourceSpecs.path(ArchetypeCommonFile.RELEASE_DRAFTER_WORKFLOW.getPath());
                }));
    }

    @Test
    void shouldRemoveReleaseDrafterIfContinuousDeliveryEnabledAlternativePath() {
        rewriteRun(
                spec -> spec.recipe(new RemoveReleaseDrafter()),
                // language=yaml
                yaml("{}", sourceSpecs -> {
                    sourceSpecs.path(ArchetypeCommonFile.WORKFLOW_CD.getPaths().stream()
                            .sorted()
                            .toList()
                            .get(0));
                }),
                yaml("{}", null, sourceSpecs -> {
                    sourceSpecs.path(ArchetypeCommonFile.RELEASE_DRAFTER.getPaths().stream()
                            .sorted()
                            .toList()
                            .get(0));
                }),
                yaml("{}", null, sourceSpecs -> {
                    sourceSpecs.path(ArchetypeCommonFile.RELEASE_DRAFTER_WORKFLOW.getPaths().stream()
                            .sorted()
                            .toList()
                            .get(0));
                }));
    }
}
