// CreateDynamicJenkinsFileTest.java
package io.jenkins.tools.pluginmodernizer.core.recipes;

import static org.openrewrite.groovy.Assertions.groovy;
import static org.openrewrite.test.SourceSpecs.text;

import io.jenkins.tools.pluginmodernizer.core.extractor.ArchetypeCommonFile;
import org.junit.jupiter.api.Test;
import org.openrewrite.test.RewriteTest;

class CreateDynamicJenkinsFileTest implements RewriteTest {

    @Test
    void shouldAddJenkinsfile() {
        rewriteRun(
                spec -> spec.recipe(new CreateDynamicJenkinsFile("2.452.4")),
                text(""), // Trigger the recipe
                groovy(
                        null,
                        """
                /*
                See the documentation for more options:
                https://github.com/jenkins-infra/pipeline-library/
                */
                buildPlugin(
                    forkCount: '1C', // Run a JVM per core in tests
                    useContainerAgent: true, // Set to `false` if you need to use Docker for containerized tests
                    configurations: [
                        [platform: 'linux', jdk: 21],
                        [platform: 'windows', jdk: 17]
                    ]
                )
                """,
                        sourceSpecs -> sourceSpecs.path(ArchetypeCommonFile.JENKINSFILE.getPath())));
    }

    @Test
    void shouldNotAddJenkinsfileIfAlreadyPresent() {
        rewriteRun(
                spec -> spec.recipe(new CreateDynamicJenkinsFile("2.452.4")), groovy("buildPlugin()", sourceSpecs -> {
                    sourceSpecs.path(ArchetypeCommonFile.JENKINSFILE.getPath());
                }));
    }
}
