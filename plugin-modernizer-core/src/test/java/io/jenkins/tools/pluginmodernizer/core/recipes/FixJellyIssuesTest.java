package io.jenkins.tools.pluginmodernizer.core.recipes;

import static org.openrewrite.maven.Assertions.pomXml;
import static org.openrewrite.test.SourceSpecs.text;

import io.jenkins.tools.pluginmodernizer.core.extractor.ArchetypeCommonFile;
import io.jenkins.tools.pluginmodernizer.core.utils.Utils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.openrewrite.test.RewriteTest;

/**
 * Test for {@code io.jenkins.tools.pluginmodernizer.FixJellyIssues}.
 */
@Execution(ExecutionMode.CONCURRENT)
class FixJellyIssuesTest implements RewriteTest {

    private static final String PLUGIN_POM =
            """
            <?xml version="1.0" encoding="UTF-8"?>
            <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/maven-v4_0_0.xsd">
              <modelVersion>4.0.0</modelVersion>
              <parent>
                <groupId>org.jenkins-ci.plugins</groupId>
                <artifactId>plugin</artifactId>
                <version>4.88</version>
                <relativePath />
              </parent>
              <artifactId>report-info</artifactId>
              <version>${changelist}</version>
              <packaging>hpi</packaging>
              <description>Report info plugin</description>
              <repositories>
                <repository>
                  <id>repo.jenkins-ci.org</id>
                  <url>https://repo.jenkins-ci.org/public/</url>
                </repository>
              </repositories>
            </project>""";

    private static final String JELLY_WITHOUT_DECLARATION =
            """
            <div>
               Report info plugin
            </div>
            """;

    private static final String JELLY_WITH_DECLARATION_AND_NEWLINE =
            """
            <?jelly escape-by-default='true'?>
            <div>
               Report info plugin
            </div>""" + "\n";

    private static final String JELLY_WITH_DECLARATION_WITHOUT_NEWLINE =
            """
            <?jelly escape-by-default='true'?>
            <div>
               Report info plugin
            </div>""";

    /**
     * Regression test for <a href="https://github.com/jenkins-infra/plugin-modernizer-tool/issues/699">#699</a>.
     */
    @Test
    void fixJellyIssuesPreservesNewlineAtEndOfFile() {
        rewriteRun(
                spec -> spec.executionContext(Utils.getMavenExecutionContext())
                        .recipeFromResource(
                                "/META-INF/rewrite/recipes.yml", "io.jenkins.tools.pluginmodernizer.FixJellyIssues")
                        .cycles(2)
                        .expectedCyclesThatMakeChanges(2),
                // language=xml
                pomXml(PLUGIN_POM, PLUGIN_POM + "\n", s -> s.noTrim()),
                text(
                        JELLY_WITHOUT_DECLARATION,
                        JELLY_WITH_DECLARATION_AND_NEWLINE,
                        s -> s.path(ArchetypeCommonFile.INDEX_JELLY.getPath()).noTrim()));
    }

    /**
     * Documents why {@code EndOfLineAtEndOfFile} is required in {@code FixJellyIssues}: {@code AddJellyXmlDeclaration}
     * alone does not guarantee a trailing newline.
     */
    @Test
    void addJellyXmlDeclarationAloneDoesNotEnsureTrailingNewline() {
        rewriteRun(
                spec -> spec.executionContext(Utils.getMavenExecutionContext())
                        .recipe(new org.openrewrite.jenkins.AddJellyXmlDeclaration()),
                // language=xml
                pomXml(PLUGIN_POM),
                text(JELLY_WITHOUT_DECLARATION, JELLY_WITH_DECLARATION_WITHOUT_NEWLINE, s -> s.path(ArchetypeCommonFile.INDEX_JELLY.getPath())));
    }

    /**
     * Verifies the trailing-newline step in isolation.
     */
    @Test
    void endOfLineAtEndOfFileEnsuresTrailingNewline() {
        rewriteRun(
                spec -> spec.executionContext(Utils.getMavenExecutionContext())
                        .recipe(new org.openrewrite.text.EndOfLineAtEndOfFile()),
                text(
                        JELLY_WITH_DECLARATION_WITHOUT_NEWLINE,
                        JELLY_WITH_DECLARATION_AND_NEWLINE,
                        s -> s.path(ArchetypeCommonFile.INDEX_JELLY.getPath()).noTrim()));
    }
}
