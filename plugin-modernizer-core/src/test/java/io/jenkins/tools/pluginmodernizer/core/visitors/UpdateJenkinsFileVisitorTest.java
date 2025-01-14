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
                buildPlugin()
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
                buildPlugin()
                """,
                        sourceSpecs -> {
                            sourceSpecs.path(ArchetypeCommonFile.JENKINSFILE.getPath());
                        }));
    }
}
