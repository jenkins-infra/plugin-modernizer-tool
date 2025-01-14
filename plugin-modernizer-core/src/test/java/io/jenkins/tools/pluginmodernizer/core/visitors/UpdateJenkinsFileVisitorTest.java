package io.jenkins.tools.pluginmodernizer.core.visitors;

import static org.openrewrite.groovy.Assertions.groovy;
import static org.openrewrite.test.RewriteTest.toRecipe;

import io.jenkins.tools.pluginmodernizer.core.extractor.ArchetypeCommonFile;
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
    // TODO: Be adapted to replace with configurations block
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
                    jdkVersions: ['8', '11'], // run this number of tests in parallel for faster feedback.  If the number terminates with a 'C', the value will be multiplied by the number of available CPU cores
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
                    forkCount: '1C',
                    useContainerAgent: true,
                    configurations: []
                )
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
                    forkCount: '1C',
                    useContainerAgent: true,
                    configurations: []
                )
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
                    forkCount: '1C',
                    useContainerAgent: true,
                    configurations: []
                )
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
                        doAfterVisit(new UpdateJenkinsFileVisitor(true, null));
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
                    forkCount: '1C',
                    useContainerAgent: true,
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
                        doAfterVisit(new UpdateJenkinsFileVisitor(null, "2C"));
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
                    forkCount: '2C',
                    useContainerAgent: true,
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
                        doAfterVisit(new UpdateJenkinsFileVisitor(false, null));
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
                    forkCount: '1C',
                    useContainerAgent: false,
                    configurations: []
                )
                """,
                        sourceSpecs -> {
                            sourceSpecs.path(ArchetypeCommonFile.JENKINSFILE.getPath());
                        }));
    }
}
