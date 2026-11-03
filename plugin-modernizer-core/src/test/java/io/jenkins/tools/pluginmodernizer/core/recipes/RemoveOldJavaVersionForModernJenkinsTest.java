package io.jenkins.tools.pluginmodernizer.core.recipes;

import static org.openrewrite.groovy.Assertions.groovy;

import io.jenkins.tools.pluginmodernizer.core.extractor.ArchetypeCommonFile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.openrewrite.test.RewriteTest;

/**
 * Smoke test for the declarative {@code RemoveOldJavaVersionForModernJenkins} recipe.
 */
@Execution(ExecutionMode.CONCURRENT)
class RemoveOldJavaVersionForModernJenkinsTest implements RewriteTest {

    @Test
    void removesLegacyJdksFromJenkinsfile() {
        rewriteRun(
                spec -> spec.recipeFromResource(
                        "/META-INF/rewrite/recipes.yml",
                        "io.jenkins.tools.pluginmodernizer.RemoveOldJavaVersionForModernJenkins"),
                groovy(
                        """
                        buildPlugin(
                          dontRemoveMe: 'true',
                          forkCount: '1C',
                          jdkVersions: ['8', '11'],
                          platforms: ['linux', 'windows']
                        )
                        """,
                        """
                        /*
                         See the documentation for more options:
                         https://github.com/jenkins-infra/pipeline-library/
                        */
                        buildPlugin(
                          dontRemoveMe: 'true',
                          forkCount: '1C', // run this number of tests in parallel for faster feedback.  If the number terminates with a 'C', the value will be multiplied by the number of available CPU cores
                          useContainerAgent: true, // Set to `false` if you need to use Docker for containerized tests
                          configurations: [
                            [platform: 'linux', jdk: 25],
                        ])
                        """,
                        sourceSpecs -> sourceSpecs.path(ArchetypeCommonFile.JENKINSFILE.getPath())));
    }
}
