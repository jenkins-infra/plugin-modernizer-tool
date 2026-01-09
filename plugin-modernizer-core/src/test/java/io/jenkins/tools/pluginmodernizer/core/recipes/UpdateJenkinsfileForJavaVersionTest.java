package io.jenkins.tools.pluginmodernizer.core.recipes;

import static org.openrewrite.groovy.Assertions.groovy;

import io.jenkins.tools.pluginmodernizer.core.extractor.ArchetypeCommonFile;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.openrewrite.test.RewriteTest;

/**
 * Test for {@link UpdateJenkinsfileForJavaVersion}.
 */
@Execution(ExecutionMode.CONCURRENT)
public class UpdateJenkinsfileForJavaVersionTest implements RewriteTest {

    @Test
    void shouldUpgradeLegacyConfigAndAddJava25() {
        rewriteRun(
                spec -> spec.recipe(new UpdateJenkinsfileForJavaVersion(25)),
                // language=groovy
                groovy("""
                        buildPlugin(
                          dontRemoveMe: 'true',
                          forkCount: '1C',
                          jdkVersions: ['8', '11'],
                          platforms: ['linux', 'windows']
                        )
                        """, """
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
                            [platform: 'linux', jdk: 11],
                            [platform: 'windows', jdk: 11],
                            [platform: 'linux', jdk: 8],
                            [platform: 'windows', jdk: 8],
                        ])
                        """, sourceSpecs -> sourceSpecs.path(ArchetypeCommonFile.JENKINSFILE.getPath())));
    }

    @Test
    void shouldRemoveOldJDKFromLegacyConfig() {
        rewriteRun(
                spec -> spec.recipe(new UpdateJenkinsfileForJavaVersion(25, List.of(8, 11, 17))),
                // language=groovy
                groovy("""
                        buildPlugin(
                          dontRemoveMe: 'true',
                          forkCount: '1C',
                          jdkVersions: ['8', '11'],
                          platforms: ['linux', 'windows']
                        )
                        """, """
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
                        """, sourceSpecs -> sourceSpecs.path(ArchetypeCommonFile.JENKINSFILE.getPath())));
    }

    @Test
    void shouldRemoveOldJDKFromRecentConfigWithWindows() {
        rewriteRun(
                spec -> spec.recipe(new UpdateJenkinsfileForJavaVersion(25, List.of(8, 11, 17))),
                // language=groovy
                groovy("""
                    /*
                     See the documentation for more options:
                     https://github.com/jenkins-infra/pipeline-library/
                    */
                    buildPlugin(
                      forkCount: '1C', // run this number of tests in parallel for faster feedback.  If the number terminates with a 'C', the value will be multiplied by the number of available CPU cores
                      useContainerAgent: true, // Set to `false` if you need to use Docker for containerized tests
                      configurations: [
                        [platform: 'linux', jdk: 21],
                        [platform: 'windows', jdk: 17],
                    ])
                    """, """
                    /*
                     See the documentation for more options:
                     https://github.com/jenkins-infra/pipeline-library/
                    */
                    buildPlugin(
                      forkCount: '1C', // run this number of tests in parallel for faster feedback.  If the number terminates with a 'C', the value will be multiplied by the number of available CPU cores
                      useContainerAgent: true, // Set to `false` if you need to use Docker for containerized tests
                      configurations: [
                        [platform: 'linux', jdk: 25],
                        [platform: 'windows', jdk: 21],
                    ])
                    """, sourceSpecs -> sourceSpecs.path(ArchetypeCommonFile.JENKINSFILE.getPath())));
    }

    @Test
    void shouldRemoveOldJDKFromRecentConfigWithoutWindows() {
        rewriteRun(
                spec -> spec.recipe(new UpdateJenkinsfileForJavaVersion(25, List.of(8, 11, 17))),
                // language=groovy
                groovy("""
                    /*
                     See the documentation for more options:
                     https://github.com/jenkins-infra/pipeline-library/
                    */
                    buildPlugin(
                      forkCount: '1C', // run this number of tests in parallel for faster feedback.  If the number terminates with a 'C', the value will be multiplied by the number of available CPU cores
                      useContainerAgent: true, // Set to `false` if you need to use Docker for containerized tests
                      configurations: [
                        [platform: 'linux', jdk: 21],
                        [platform: 'linux', jdk: 17],
                        [platform: 'linux', jdk: 25],
                    ])
                    """, """
                    /*
                     See the documentation for more options:
                     https://github.com/jenkins-infra/pipeline-library/
                    */
                    buildPlugin(
                      forkCount: '1C', // run this number of tests in parallel for faster feedback.  If the number terminates with a 'C', the value will be multiplied by the number of available CPU cores
                      useContainerAgent: true, // Set to `false` if you need to use Docker for containerized tests
                      configurations: [
                        [platform: 'linux', jdk: 25],
                        [platform: 'linux', jdk: 21],
                    ])
                    """, sourceSpecs -> sourceSpecs.path(ArchetypeCommonFile.JENKINSFILE.getPath())));
    }

    @Test
    void shouldUpgradeLegacyConfigAndAddJava25LimitJDK() {
        rewriteRun(
                spec -> spec.recipe(new UpdateJenkinsfileForJavaVersion(25, 2)),
                // language=groovy
                groovy("""
                        buildPlugin(
                          dontRemoveMe: 'true',
                          forkCount: '1C',
                          jdkVersions: ['8', '11'],
                          platforms: ['linux', 'windows']
                        )
                        """, """
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
                            [platform: 'windows', jdk: 11],
                        ])
                        """, sourceSpecs -> sourceSpecs.path(ArchetypeCommonFile.JENKINSFILE.getPath())));
    }

    @Test
    void shouldAddJava25ToModernConfigurations() {
        rewriteRun(
                spec -> spec.recipe(new UpdateJenkinsfileForJavaVersion(25)),
                // language=groovy
                groovy("""
                        buildPlugin(
                          useContainerAgent: true,
                          configurations: [
                            [platform: 'linux', jdk: 17],
                            [platform: 'windows', jdk: 17]
                          ]
                        )
                        """, """
                        /*
                         See the documentation for more options:
                         https://github.com/jenkins-infra/pipeline-library/
                        */
                        buildPlugin(
                          useContainerAgent: true,
                          forkCount: '1C', // Set to `false` if you need to use Docker for containerized tests
                          configurations: [
                            [platform: 'linux', jdk: 25],
                            [platform: 'linux', jdk: 17],
                            [platform: 'windows', jdk: 17],
                        ])
                        """, sourceSpecs -> sourceSpecs.path(ArchetypeCommonFile.JENKINSFILE.getPath())));
    }

    @Test
    void shouldAddJava21ToModernConfigurations() {
        rewriteRun(
                spec -> spec.recipe(new UpdateJenkinsfileForJavaVersion(21)),
                // language=groovy
                groovy("""
                        buildPlugin(
                          useContainerAgent: true,
                          configurations: [
                            [platform: 'linux', jdk: 17],
                            [platform: 'windows', jdk: 17]
                          ]
                        )
                        """, """
                        /*
                         See the documentation for more options:
                         https://github.com/jenkins-infra/pipeline-library/
                        */
                        buildPlugin(
                          useContainerAgent: true,
                          forkCount: '1C', // Set to `false` if you need to use Docker for containerized tests
                          configurations: [
                            [platform: 'linux', jdk: 21],
                            [platform: 'linux', jdk: 17],
                            [platform: 'windows', jdk: 17],
                        ])
                        """, sourceSpecs -> sourceSpecs.path(ArchetypeCommonFile.JENKINSFILE.getPath())));
    }

    @Test
    void shouldAddJava21ToModernConfigurationsAndRemoveOldJDK() {
        rewriteRun(
                spec -> spec.recipe(new UpdateJenkinsfileForJavaVersion(21, List.of(17))),
                // language=groovy
                groovy("""
                        buildPlugin(
                          useContainerAgent: true,
                          configurations: [
                            [platform: 'linux', jdk: 11],
                            [platform: 'linux', jdk: 17],
                            [platform: 'windows', jdk: 17]
                          ]
                        )
                        """, """
                        /*
                         See the documentation for more options:
                         https://github.com/jenkins-infra/pipeline-library/
                        */
                        buildPlugin(
                          useContainerAgent: true,
                          forkCount: '1C', // Set to `false` if you need to use Docker for containerized tests
                          configurations: [
                            [platform: 'linux', jdk: 21],
                            [platform: 'windows', jdk: 11],
                        ])
                        """, sourceSpecs -> sourceSpecs.path(ArchetypeCommonFile.JENKINSFILE.getPath())));
    }

    @Test
    void shouldReplaceJava17To25InModernConfigurations() {
        rewriteRun(
                spec -> spec.recipe(new UpdateJenkinsfileForJavaVersion(25, 2)),
                // language=groovy
                groovy("""
                        buildPlugin(
                          useContainerAgent: true,
                          configurations: [
                            [platform: 'windows', jdk: 17],
                            [platform: 'linux', jdk: 21],
                          ]
                        )
                        """, """
                        /*
                         See the documentation for more options:
                         https://github.com/jenkins-infra/pipeline-library/
                        */
                        buildPlugin(
                          useContainerAgent: true,
                          forkCount: '1C', // Set to `false` if you need to use Docker for containerized tests
                          configurations: [
                            [platform: 'linux', jdk: 25],
                            [platform: 'windows', jdk: 21],
                        ])
                        """, sourceSpecs -> sourceSpecs.path(ArchetypeCommonFile.JENKINSFILE.getPath())));
    }

    @Test
    void shouldNotAddJavaVersionToModernConfigurationsAsNotLTS() {
        rewriteRun(
                spec -> spec.recipe(new UpdateJenkinsfileForJavaVersion(18)),
                // language=groovy
                groovy("""
                        buildPlugin(
                          useContainerAgent: true,
                          configurations: [
                            [platform: 'linux', jdk: 17],
                            [platform: 'windows', jdk: 17]
                          ]
                        )
                        """, sourceSpecs -> sourceSpecs.path(ArchetypeCommonFile.JENKINSFILE.getPath())));
    }

    @Test
    void shouldNotAddJavaVersionWhenAlreadyPresent() {
        rewriteRun(
                spec -> spec.recipe(new UpdateJenkinsfileForJavaVersion(25)),
                // language=groovy
                groovy("""
                        buildPlugin(
                          configurations: [
                            [platform: 'linux', jdk: 17],
                            [platform: 'linux', jdk: 25]
                          ]
                        )
                        """, sourceSpecs -> sourceSpecs.path(ArchetypeCommonFile.JENKINSFILE.getPath())));
    }

    @Test
    void shouldAddConfigurationsBlockForJavaVersionWhenMissing() {
        rewriteRun(
                spec -> spec.recipe(new UpdateJenkinsfileForJavaVersion(25)),
                // language=groovy
                groovy("""
                        buildPlugin(
                          forkCount: '2C'
                        )
                        """, """
                        /*
                         See the documentation for more options:
                         https://github.com/jenkins-infra/pipeline-library/
                        */
                        buildPlugin(
                          forkCount: '2C'
                        , // run this number of tests in parallel for faster feedback.  If the number terminates with a 'C', the value will be multiplied by the number of available CPU cores
                          useContainerAgent: true, // Set to `false` if you need to use Docker for containerized tests
                          configurations: [
                            [platform: 'linux', jdk: 25],
                        ])
                        """, sourceSpecs -> sourceSpecs.path(ArchetypeCommonFile.JENKINSFILE.getPath())));
    }
}
