package io.jenkins.tools.pluginmodernizer.core.recipes;

import static org.openrewrite.yaml.Assertions.yaml;

import io.jenkins.tools.pluginmodernizer.core.extractor.ArchetypeCommonFile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.openrewrite.test.RewriteTest;

/**
 * Test for {@code io.jenkins.tools.pluginmodernizer.RemoveSecurityScanJavaVersion}.
 */
@Execution(ExecutionMode.CONCURRENT)
class RemoveSecurityScanJavaVersionTest implements RewriteTest {

    private static final String SECURITY_SCAN_WITH_JAVA_VERSION = """
            # More information about the Jenkins security scan can be found at the developer docs: https://www.jenkins.io/redirect/jenkins-security-scan/
            ---
            name: Jenkins Security Scan
            on:
              push:
                branches:
                  - "master"
                  - "main"
              pull_request:
                types: [opened, synchronize, reopened]
              workflow_dispatch:

            permissions:
              security-events: write
              contents: read
              actions: read

            jobs:
              security-scan:
                uses: jenkins-infra/jenkins-security-scan/.github/workflows/jenkins-security-scan.yaml@v2
                with:
                  java-cache: 'maven'
                  java-version: 17
            """;

    private static final String SECURITY_SCAN_WITHOUT_JAVA_VERSION = """
            # More information about the Jenkins security scan can be found at the developer docs: https://www.jenkins.io/redirect/jenkins-security-scan/
            ---
            name: Jenkins Security Scan
            on:
              push:
                branches:
                  - "master"
                  - "main"
              pull_request:
                types: [opened, synchronize, reopened]
              workflow_dispatch:

            permissions:
              security-events: write
              contents: read
              actions: read

            jobs:
              security-scan:
                uses: jenkins-infra/jenkins-security-scan/.github/workflows/jenkins-security-scan.yaml@v2
                with:
                  java-cache: 'maven'
            """;

    /**
     * Regression test for <a href="https://github.com/jenkins-infra/plugin-modernizer-tool/issues/814">#814</a>.
     */
    @Test
    void removeSecurityScanJavaVersion() {
        rewriteRun(
                spec -> spec.recipeFromResource(
                        "/META-INF/rewrite/recipes.yml",
                        "io.jenkins.tools.pluginmodernizer.RemoveSecurityScanJavaVersion"),
                yaml(
                        SECURITY_SCAN_WITH_JAVA_VERSION,
                        SECURITY_SCAN_WITHOUT_JAVA_VERSION,
                        s -> s.path(ArchetypeCommonFile.WORKFLOW_SECURITY.getPath())));
    }

    @Test
    void doesNotChangeSecurityScanWithoutJavaVersion() {
        rewriteRun(
                spec -> spec.recipeFromResource(
                        "/META-INF/rewrite/recipes.yml",
                        "io.jenkins.tools.pluginmodernizer.RemoveSecurityScanJavaVersion"),
                yaml(SECURITY_SCAN_WITHOUT_JAVA_VERSION, s -> s.path(ArchetypeCommonFile.WORKFLOW_SECURITY.getPath())));
    }
}
