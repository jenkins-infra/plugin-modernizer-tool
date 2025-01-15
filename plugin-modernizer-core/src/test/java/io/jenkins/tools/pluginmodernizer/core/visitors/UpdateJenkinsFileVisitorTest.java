package io.jenkins.tools.pluginmodernizer.core.visitors;

import static org.openrewrite.groovy.Assertions.groovy;
import static org.openrewrite.test.RewriteTest.toRecipe;

import io.jenkins.tools.pluginmodernizer.core.extractor.ArchetypeCommonFile;
import io.jenkins.tools.pluginmodernizer.core.model.JDK;
import io.jenkins.tools.pluginmodernizer.core.model.Platform;
import io.jenkins.tools.pluginmodernizer.core.model.PlatformConfig;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.openrewrite.ExecutionContext;
import org.openrewrite.groovy.GroovyIsoVisitor;
import org.openrewrite.groovy.tree.G;
import org.openrewrite.test.RewriteTest;

/**
 * Tests for {@link UpdateJenkinsFileVisitor}.
 */
@Execution(ExecutionMode.CONCURRENT)
public class UpdateJenkinsFileVisitorTest implements RewriteTest {

    @Test
    void removeLegacyParams() {
        rewriteRun(
                spec -> spec.recipe(toRecipe(() -> new GroovyIsoVisitor<>() {
                    @Override
                    public G.CompilationUnit visitCompilationUnit(
                            G.CompilationUnit cu, ExecutionContext executionContext) {
                        doAfterVisit(new UpdateJenkinsFileVisitor());
                        return super.visitCompilationUnit(cu, executionContext);
                    }
                })),
                // language=groovy
                groovy(
                        """
                buildPlugin(
                  dontRemoveMe: 'true',
                  forkCount: '1C', // run this number of tests in parallel for faster feedback.  If the number terminates with a 'C', the value will be multiplied by the number of available CPU cores
                  jdkVersions: ['8', '11'],
                  jenkinsVersions: ['2.222.1', '2.249.1'],
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
                    [platform: 'linux', jdk: 21],
                    [platform: 'windows', jdk: 17],
                ])
                """,
                        sourceSpecs -> {
                            sourceSpecs.path(ArchetypeCommonFile.JENKINSFILE.getPath());
                        }));
    }

    @Test
    void addOnePlatformConfig() {
        rewriteRun(
                spec -> spec.recipe(toRecipe(() -> new GroovyIsoVisitor<>() {
                    @Override
                    public G.CompilationUnit visitCompilationUnit(
                            G.CompilationUnit cu, ExecutionContext executionContext) {
                        doAfterVisit(new UpdateJenkinsFileVisitor(
                                null, null, List.of(new PlatformConfig(Platform.LINUX, JDK.JAVA_17, null, true))));
                        return super.visitCompilationUnit(cu, executionContext);
                    }
                })),
                // language=groovy
                groovy(
                        """
                buildPlugin()
                """,
                        """
                /*
                 See the documentation for more options:
                 https://github.com/jenkins-infra/pipeline-library/
                */
                buildPlugin(
                  forkCount: '1C', // run this number of tests in parallel for faster feedback.  If the number terminates with a 'C', the value will be multiplied by the number of available CPU cores
                  useContainerAgent: true, // Set to `false` if you need to use Docker for containerized tests
                  configurations: [
                    [platform: 'linux', jdk: 17],
                ])
                """,
                        sourceSpecs -> {
                            sourceSpecs.path(ArchetypeCommonFile.JENKINSFILE.getPath());
                        }));
    }

    @Test
    void addOneTwoPlatformConfig() {
        rewriteRun(
                spec -> spec.recipe(toRecipe(() -> new GroovyIsoVisitor<>() {
                    @Override
                    public G.CompilationUnit visitCompilationUnit(
                            G.CompilationUnit cu, ExecutionContext executionContext) {
                        doAfterVisit(new UpdateJenkinsFileVisitor(
                                null,
                                null,
                                List.of(
                                        PlatformConfig.build(Platform.LINUX, JDK.JAVA_17),
                                        PlatformConfig.build(Platform.WINDOWS, JDK.JAVA_11, "2.249.1"))));
                        return super.visitCompilationUnit(cu, executionContext);
                    }
                })),
                // language=groovy
                groovy(
                        """
                buildPlugin()
                """,
                        """
                /*
                 See the documentation for more options:
                 https://github.com/jenkins-infra/pipeline-library/
                */
                buildPlugin(
                  forkCount: '1C', // run this number of tests in parallel for faster feedback.  If the number terminates with a 'C', the value will be multiplied by the number of available CPU cores
                  useContainerAgent: true, // Set to `false` if you need to use Docker for containerized tests
                  configurations: [
                    [platform: 'linux', jdk: 17],
                    [platform: 'windows', jdk: 11, jenkins: '2.249.1'],
                ])
                """,
                        sourceSpecs -> {
                            sourceSpecs.path(ArchetypeCommonFile.JENKINSFILE.getPath());
                        }));
    }

    @Test
    void addOnePlatformConfigWithJenkinsVersion() {
        rewriteRun(
                spec -> spec.recipe(toRecipe(() -> new GroovyIsoVisitor<>() {
                    @Override
                    public G.CompilationUnit visitCompilationUnit(
                            G.CompilationUnit cu, ExecutionContext executionContext) {
                        doAfterVisit(new UpdateJenkinsFileVisitor(
                                null, null, List.of(PlatformConfig.build(Platform.LINUX, JDK.JAVA_17, "2.479.1"))));
                        return super.visitCompilationUnit(cu, executionContext);
                    }
                })),
                // language=groovy
                groovy(
                        """
                buildPlugin()
                """,
                        """
                /*
                 See the documentation for more options:
                 https://github.com/jenkins-infra/pipeline-library/
                */
                buildPlugin(
                  forkCount: '1C', // run this number of tests in parallel for faster feedback.  If the number terminates with a 'C', the value will be multiplied by the number of available CPU cores
                  useContainerAgent: true, // Set to `false` if you need to use Docker for containerized tests
                  configurations: [
                    [platform: 'linux', jdk: 17, jenkins: '2.479.1'],
                ])
                """,
                        sourceSpecs -> {
                            sourceSpecs.path(ArchetypeCommonFile.JENKINSFILE.getPath());
                        }));
    }

    @Test
    void addMissingComment() {
        rewriteRun(
                spec -> spec.recipe(toRecipe(() -> new GroovyIsoVisitor<>() {
                    @Override
                    public G.CompilationUnit visitCompilationUnit(
                            G.CompilationUnit cu, ExecutionContext executionContext) {
                        doAfterVisit(new UpdateJenkinsFileVisitor());
                        return super.visitCompilationUnit(cu, executionContext);
                    }
                })),
                // language=groovy
                groovy(
                        """
                buildPlugin()
                """,
                        """
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
                """,
                        sourceSpecs -> {
                            sourceSpecs.path(ArchetypeCommonFile.JENKINSFILE.getPath());
                        }));
    }

    @Test
    void replaceWrongComment() {
        rewriteRun(
                spec -> spec.recipe(toRecipe(() -> new GroovyIsoVisitor<>() {
                    @Override
                    public G.CompilationUnit visitCompilationUnit(
                            G.CompilationUnit cu, ExecutionContext executionContext) {
                        doAfterVisit(new UpdateJenkinsFileVisitor());
                        return super.visitCompilationUnit(cu, executionContext);
                    }
                })),
                // language=groovy
                groovy(
                        """
                // This is a comment
                buildPlugin()
                """,
                        """
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
                """,
                        sourceSpecs -> {
                            sourceSpecs.path(ArchetypeCommonFile.JENKINSFILE.getPath());
                        }));
    }

    @Test
    void addContainerAgentTrue() {
        rewriteRun(
                spec -> spec.recipe(toRecipe(() -> new GroovyIsoVisitor<>() {
                    @Override
                    public G.CompilationUnit visitCompilationUnit(
                            G.CompilationUnit cu, ExecutionContext executionContext) {
                        doAfterVisit(new UpdateJenkinsFileVisitor(true, null, List.of()));
                        return super.visitCompilationUnit(cu, executionContext);
                    }
                })),
                // language=groovy
                groovy(
                        """
                buildPlugin()
                """,
                        """
                /*
                 See the documentation for more options:
                 https://github.com/jenkins-infra/pipeline-library/
                */
                buildPlugin(
                  forkCount: '1C', // run this number of tests in parallel for faster feedback.  If the number terminates with a 'C', the value will be multiplied by the number of available CPU cores
                  useContainerAgent: true, // Set to `false` if you need to use Docker for containerized tests
                  configurations: []
                )
                """,
                        sourceSpecs -> {
                            sourceSpecs.path(ArchetypeCommonFile.JENKINSFILE.getPath());
                        }));
    }

    @Test
    void addForkCount() {
        rewriteRun(
                spec -> spec.recipe(toRecipe(() -> new GroovyIsoVisitor<>() {
                    @Override
                    public G.CompilationUnit visitCompilationUnit(
                            G.CompilationUnit cu, ExecutionContext executionContext) {
                        doAfterVisit(new UpdateJenkinsFileVisitor(null, "2C", List.of()));
                        return super.visitCompilationUnit(cu, executionContext);
                    }
                })),
                // language=groovy
                groovy(
                        """
                buildPlugin()
                """,
                        """
                /*
                 See the documentation for more options:
                 https://github.com/jenkins-infra/pipeline-library/
                */
                buildPlugin(
                  forkCount: '2C', // run this number of tests in parallel for faster feedback.  If the number terminates with a 'C', the value will be multiplied by the number of available CPU cores
                  useContainerAgent: true, // Set to `false` if you need to use Docker for containerized tests
                  configurations: []
                )
                """,
                        sourceSpecs -> {
                            sourceSpecs.path(ArchetypeCommonFile.JENKINSFILE.getPath());
                        }));
    }

    @Test
    void addContainerAgentFalse() {
        rewriteRun(
                spec -> spec.recipe(toRecipe(() -> new GroovyIsoVisitor<>() {
                    @Override
                    public G.CompilationUnit visitCompilationUnit(
                            G.CompilationUnit cu, ExecutionContext executionContext) {
                        doAfterVisit(new UpdateJenkinsFileVisitor(false, null, List.of()));
                        return super.visitCompilationUnit(cu, executionContext);
                    }
                })),
                // language=groovy
                groovy(
                        /*
                         See the documentation for more options:
                         https://github.com/jenkins-infra/pipeline-library/
                        */
                        """
                        buildPlugin()
                        """,
                        """
                /*
                 See the documentation for more options:
                 https://github.com/jenkins-infra/pipeline-library/
                */
                buildPlugin(
                  forkCount: '1C', // run this number of tests in parallel for faster feedback.  If the number terminates with a 'C', the value will be multiplied by the number of available CPU cores
                  useContainerAgent: false, // Set to `false` if you need to use Docker for containerized tests
                  configurations: []
                )
                """,
                        sourceSpecs -> {
                            sourceSpecs.path(ArchetypeCommonFile.JENKINSFILE.getPath());
                        }));
    }

    @Test
    void noChangesOnCustomPipeline() {
        rewriteRun(
                spec -> spec.recipe(toRecipe(() -> new GroovyIsoVisitor<>() {
                    @Override
                    public G.CompilationUnit visitCompilationUnit(
                            G.CompilationUnit cu, ExecutionContext executionContext) {
                        doAfterVisit(new UpdateJenkinsFileVisitor(false, null, List.of()));
                        return super.visitCompilationUnit(cu, executionContext);
                    }
                })),
                // language=groovy
                groovy(
                        """
                        pipeline {
                            agent any
                            stages {
                                stage('Build') {
                                    steps {
                                        echo 'Building..'
                                    }
                                }
                            }
                        }
                        """,
                        sourceSpecs -> {
                            sourceSpecs.path(ArchetypeCommonFile.JENKINSFILE.getPath());
                        }));
    }

    @Test
    void removeVariables() {
        rewriteRun(
                spec -> spec.recipe(toRecipe(() -> new GroovyIsoVisitor<>() {
                    @Override
                    public G.CompilationUnit visitCompilationUnit(
                            G.CompilationUnit cu, ExecutionContext executionContext) {
                        doAfterVisit(new UpdateJenkinsFileVisitor(
                                false, null, List.of(PlatformConfig.build(Platform.LINUX, JDK.JAVA_17))));
                        return super.visitCompilationUnit(cu, executionContext);
                    }
                })),
                // language=groovy
                groovy(
                        """
                        def versions = [21, 17]
                        def platforms = ['linux', 'windows']
                        def jenkinsVersions = ['2.249.1', '2.222.1']
                        buildPlugin(
                            jdkVersions: versions,
                            jenkinsVersions: jenkinsVersions,
                            platforms: platforms
                        )
                        """,
                        """
                        /*
                         See the documentation for more options:
                         https://github.com/jenkins-infra/pipeline-library/
                        */
                        buildPlugin(
                          forkCount: '1C', // run this number of tests in parallel for faster feedback.  If the number terminates with a 'C', the value will be multiplied by the number of available CPU cores
                          useContainerAgent: false, // Set to `false` if you need to use Docker for containerized tests
                          configurations: [
                            [platform: 'linux', jdk: 17],
                        ])
                        """,
                        sourceSpecs -> {
                            sourceSpecs.path(ArchetypeCommonFile.JENKINSFILE.getPath());
                        }));
    }

    @Test
    void performUpdate() {
        rewriteRun(
                spec -> spec.recipe(toRecipe(() -> new GroovyIsoVisitor<>() {
                    @Override
                    public G.CompilationUnit visitCompilationUnit(
                            G.CompilationUnit cu, ExecutionContext executionContext) {
                        doAfterVisit(new UpdateJenkinsFileVisitor(
                                true, "2C", List.of(new PlatformConfig(Platform.LINUX, JDK.JAVA_21, null, true))));
                        return super.visitCompilationUnit(cu, executionContext);
                    }
                })),
                // language=groovy
                groovy(
                        """
                /*
                 See the documentation for more options:
                 https://github.com/jenkins-infra/pipeline-library/
                */
                buildPlugin(
                  forkCount: '1C', // run this number of tests in parallel for faster feedback.  If the number terminates with a 'C', the value will be multiplied by the number of available CPU cores
                  useContainerAgent: false, // Set to `false` if you need to use Docker for containerized tests
                  configurations: [
                    [platform: 'linux', jdk: 11],
                    [platform: 'windows', jdk: 17],
                    [platform: 'linux', jdk: 21],
                ])
                """,
                        """
                /*
                 See the documentation for more options:
                 https://github.com/jenkins-infra/pipeline-library/
                */
                buildPlugin(
                  forkCount: '2C', // run this number of tests in parallel for faster feedback.  If the number terminates with a 'C', the value will be multiplied by the number of available CPU cores
                  useContainerAgent: true, // Set to `false` if you need to use Docker for containerized tests
                  configurations: [
                    [platform: 'linux', jdk: 21],
                ])
                """,
                        sourceSpecs -> {
                            sourceSpecs.path(ArchetypeCommonFile.JENKINSFILE.getPath());
                        }));
    }

    @Test
    void performUpdateWithVars() {
        rewriteRun(
                spec -> spec.recipe(toRecipe(() -> new GroovyIsoVisitor<>() {
                    @Override
                    public G.CompilationUnit visitCompilationUnit(
                            G.CompilationUnit cu, ExecutionContext executionContext) {
                        doAfterVisit(new UpdateJenkinsFileVisitor(
                                true, "2C", List.of(new PlatformConfig(Platform.LINUX, JDK.JAVA_21, null, true))));
                        return super.visitCompilationUnit(cu, executionContext);
                    }
                })),
                // language=groovy
                groovy(
                        """
                /*
                 See the documentation for more options:
                 https://github.com/jenkins-infra/pipeline-library/
                */
                def forkCount = '1C'
                def useContainer = false
                def configurations = [
                    [platform: 'linux', jdk: 11],
                    [platform: 'windows', jdk: 17],
                    [platform: 'linux', jdk: 21],
                ]
                buildPlugin(
                  forkCount: forkCount, // run this number of tests in parallel for faster feedback.  If the number terminates with a 'C', the value will be multiplied by the number of available CPU cores
                  useContainerAgent: useContainer, // Set to `false` if you need to use Docker for containerized tests
                  configurations: configurations
                )
                """,
                        """
                /*
                 See the documentation for more options:
                 https://github.com/jenkins-infra/pipeline-library/
                */
                buildPlugin(
                  forkCount: '2C', // run this number of tests in parallel for faster feedback.  If the number terminates with a 'C', the value will be multiplied by the number of available CPU cores
                  useContainerAgent: true, // Set to `false` if you need to use Docker for containerized tests
                  configurations: [
                    [platform: 'linux', jdk: 21],
                ])
                """,
                        sourceSpecs -> {
                            sourceSpecs.path(ArchetypeCommonFile.JENKINSFILE.getPath());
                        }));
    }
}
