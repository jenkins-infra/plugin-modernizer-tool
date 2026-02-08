package io.jenkins.tools.pluginmodernizer.core.recipes;

import static org.openrewrite.maven.Assertions.pomXml;
import static org.openrewrite.test.SourceSpecs.text;
import static org.openrewrite.yaml.Assertions.yaml;

import io.jenkins.tools.pluginmodernizer.core.extractor.ArchetypeCommonFile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.openrewrite.test.RewriteTest;

/**
 * Test for {@link EnableCD}
 */
@Execution(ExecutionMode.CONCURRENT)
class EnableCDTest implements RewriteTest {

    private static final String EXPECTED_CD_WORKFLOW = """
        # Note: additional setup is required, see https://www.jenkins.io/redirect/continuous-delivery-of-plugins
        #
        # Please find additional hints for individual trigger use case
        # configuration options inline this script below.
        #
        ---
        name: cd
        on:
          workflow_dispatch:
            inputs:
              validate_only:
                required: false
                type: boolean
                description: |
                  Run validation with release drafter only
                  → Skip the release job
                # Note: Change this default to true,
                #       if the checkbox should be checked by default.
                default: false
          # If you don't want any automatic trigger in general, then
          # the following check_run trigger lines should all be commented.
          # Note: Consider the use case #2 config for 'validate_only' below
          #       as an alternative option!
          check_run:
            types:
              - completed

        permissions:
          checks: read
          contents: write

        jobs:
          maven-cd:
            uses: jenkins-infra/github-reusable-workflows/.github/workflows/maven-cd.yml@v1
            with:
              # Comment / uncomment the validate_only config appropriate to your preference:
              #
              # Use case #1 (automatic release):
              #   - Let any successful Jenkins build trigger another release,
              #     if there are merged pull requests of interest
              #   - Perform a validation only run with drafting a release note,
              #     if manually triggered AND inputs.validate_only has been checked.
              #
              validate_only: ${{ inputs.validate_only == true }}
              #
              # Alternative use case #2 (no automatic release):
              #   - Same as use case #1 - but:
              #     - Let any check_run trigger a validate_only run.
              #       => enforce the release job to be skipped.
              #
              #validate_only: ${{ inputs.validate_only == true || github.event_name == 'check_run' }}
            secrets:
              MAVEN_USERNAME: ${{ secrets.MAVEN_USERNAME }}
              MAVEN_TOKEN: ${{ secrets.MAVEN_TOKEN }}
        """;

    @Test
    void shouldEnableCDForBasicPlugin() {
        rewriteRun(
                spec -> spec.recipe(new EnableCD()),
                // language=xml
                pomXml("""
                    <?xml version="1.0" encoding="UTF-8"?>
                    <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                        xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
                      <modelVersion>4.0.0</modelVersion>
                      <parent>
                        <groupId>org.jenkins-ci.plugins</groupId>
                        <artifactId>plugin</artifactId>
                        <version>4.87</version>
                        <relativePath />
                      </parent>
                      <artifactId>test-plugin</artifactId>
                      <version>1.0.0-SNAPSHOT</version>
                      <packaging>hpi</packaging>
                      <name>Test Plugin</name>
                      <url>https://github.com/jenkinsci/test-plugin</url>
                      <scm>
                        <connection>scm:git:https://github.com/jenkinsci/test-plugin.git</connection>
                        <developerConnection>scm:git:git@github.com:jenkinsci/test-plugin.git</developerConnection>
                        <tag>HEAD</tag>
                        <url>https://github.com/jenkinsci/test-plugin</url>
                      </scm>
                      <properties>
                        <jenkins.version>2.452.4</jenkins.version>
                      </properties>
                    </project>
                    """, """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                        xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
                      <modelVersion>4.0.0</modelVersion>
                      <parent>
                        <groupId>org.jenkins-ci.plugins</groupId>
                        <artifactId>plugin</artifactId>
                        <version>4.87</version>
                        <relativePath />
                      </parent>
                      <artifactId>test-plugin</artifactId>
                      <version>${changelist}</version>
                      <packaging>hpi</packaging>
                      <name>Test Plugin</name>
                      <url>https://github.com/jenkinsci/test-plugin</url>
                      <scm>
                        <connection>scm:git:https://github.com/jenkinsci/test-plugin.git</connection>
                        <developerConnection>scm:git:git@github.com:jenkinsci/test-plugin.git</developerConnection>
                        <tag>HEAD</tag>
                        <url>https://github.com/jenkinsci/test-plugin</url>
                      </scm>
                      <properties>
                        <jenkins.version>2.452.4</jenkins.version>
                        <changelist>999999-SNAPSHOT</changelist>
                      </properties>
                    </project>
                    """),
                text(null, "-Dchangelist.format=%d.v%s", spec -> spec.path(ArchetypeCommonFile.MAVEN_CONFIG.getPath())),
                yaml(null, EXPECTED_CD_WORKFLOW, spec -> spec.path(ArchetypeCommonFile.WORKFLOW_CD.getPath())));
    }

    @Test
    void shouldEnableCDWithRevisionPrefix() {
        rewriteRun(
                spec -> spec.recipe(new EnableCD()),
                // language=xml
                pomXml("""
                    <?xml version="1.0" encoding="UTF-8"?>
                    <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                        xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
                      <modelVersion>4.0.0</modelVersion>
                      <parent>
                        <groupId>org.jenkins-ci.plugins</groupId>
                        <artifactId>plugin</artifactId>
                        <version>4.87</version>
                        <relativePath />
                      </parent>
                      <artifactId>test-plugin</artifactId>
                      <version>2.5.3-SNAPSHOT</version>
                      <packaging>hpi</packaging>
                      <name>Test Plugin</name>
                      <properties>
                        <jenkins.version>2.452.4</jenkins.version>
                      </properties>
                    </project>
                    """, """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                        xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
                      <modelVersion>4.0.0</modelVersion>
                      <parent>
                        <groupId>org.jenkins-ci.plugins</groupId>
                        <artifactId>plugin</artifactId>
                        <version>4.87</version>
                        <relativePath />
                      </parent>
                      <artifactId>test-plugin</artifactId>
                      <version>${changelist}</version>
                      <packaging>hpi</packaging>
                      <name>Test Plugin</name>
                      <properties>
                        <jenkins.version>2.452.4</jenkins.version>
                        <changelist>999999-SNAPSHOT</changelist>
                      </properties>
                    </project>
                    """),
                text(null, "-Dchangelist.format=%d.v%s", spec -> spec.path(ArchetypeCommonFile.MAVEN_CONFIG.getPath())),
                yaml(null, EXPECTED_CD_WORKFLOW, spec -> spec.path(ArchetypeCommonFile.WORKFLOW_CD.getPath())));
    }

    @Test
    void shouldNotModifyIfAlreadyUsingCD() {
        rewriteRun(
                spec -> spec.recipe(new EnableCD()),
                // language=xml
                pomXml("""
                    <?xml version="1.0" encoding="UTF-8"?>
                    <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                        xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
                      <modelVersion>4.0.0</modelVersion>
                      <parent>
                        <groupId>org.jenkins-ci.plugins</groupId>
                        <artifactId>plugin</artifactId>
                        <version>4.87</version>
                        <relativePath />
                      </parent>
                      <artifactId>test-plugin</artifactId>
                      <version>${changelist}</version>
                      <packaging>hpi</packaging>
                      <name>Test Plugin</name>
                      <properties>
                        <changelist>999999-SNAPSHOT</changelist>
                        <jenkins.version>2.452.4</jenkins.version>
                      </properties>
                    </project>
                    """),
                yaml("existing: content", spec -> spec.path(ArchetypeCommonFile.WORKFLOW_CD.getPath())),
                text("existing content", spec -> spec.path(ArchetypeCommonFile.MAVEN_CONFIG.getPath())));
    }

    @Test
    void shouldUpdateExistingMavenConfig() {
        rewriteRun(
                spec -> spec.recipe(new EnableCD()),
                // language=xml
                pomXml("""
                    <?xml version="1.0" encoding="UTF-8"?>
                    <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                        xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
                      <modelVersion>4.0.0</modelVersion>
                      <parent>
                        <groupId>org.jenkins-ci.plugins</groupId>
                        <artifactId>plugin</artifactId>
                        <version>4.87</version>
                        <relativePath />
                      </parent>
                      <artifactId>test-plugin</artifactId>
                      <version>1.0-SNAPSHOT</version>
                      <packaging>hpi</packaging>
                      <properties>
                        <jenkins.version>2.452.4</jenkins.version>
                      </properties>
                    </project>
                    """, """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                        xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
                      <modelVersion>4.0.0</modelVersion>
                      <parent>
                        <groupId>org.jenkins-ci.plugins</groupId>
                        <artifactId>plugin</artifactId>
                        <version>4.87</version>
                        <relativePath />
                      </parent>
                      <artifactId>test-plugin</artifactId>
                      <version>${changelist}</version>
                      <packaging>hpi</packaging>
                      <properties>
                        <jenkins.version>2.452.4</jenkins.version>
                        <changelist>999999-SNAPSHOT</changelist>
                      </properties>
                    </project>
                    """),
                text(
                        "-Pconsume-incrementals\n-Pmight-produce-incrementals\n",
                        "-Pconsume-incrementals\n-Pmight-produce-incrementals\n-Dchangelist.format=%d.v%s",
                        spec -> spec.path(ArchetypeCommonFile.MAVEN_CONFIG.getPath())),
                yaml(null, EXPECTED_CD_WORKFLOW, spec -> spec.path(ArchetypeCommonFile.WORKFLOW_CD.getPath())));
    }

    @Test
    void shouldSkipIfNoPropertiesSection() {
        rewriteRun(
                spec -> spec.recipe(new EnableCD()),
                // language=xml
                pomXml("""
                    <?xml version="1.0" encoding="UTF-8"?>
                    <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                        xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
                      <modelVersion>4.0.0</modelVersion>
                      <parent>
                        <groupId>org.jenkins-ci.plugins</groupId>
                        <artifactId>plugin</artifactId>
                        <version>4.87</version>
                        <relativePath />
                      </parent>
                      <artifactId>test-plugin</artifactId>
                      <version>1.0-SNAPSHOT</version>
                      <packaging>hpi</packaging>
                    </project>
                    """));
    }

    @Test
    void shouldDeleteReleaseDrafterWorkflow() {
        rewriteRun(
                spec -> spec.recipe(new EnableCD()),
                // language=xml
                pomXml("""
                    <?xml version="1.0" encoding="UTF-8"?>
                    <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                        xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
                      <modelVersion>4.0.0</modelVersion>
                      <parent>
                        <groupId>org.jenkins-ci.plugins</groupId>
                        <artifactId>plugin</artifactId>
                        <version>4.87</version>
                        <relativePath />
                      </parent>
                      <artifactId>test-plugin</artifactId>
                      <version>1.0-SNAPSHOT</version>
                      <packaging>hpi</packaging>
                      <properties>
                        <jenkins.version>2.440.3</jenkins.version>
                      </properties>
                    </project>
                    """, """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                        xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
                      <modelVersion>4.0.0</modelVersion>
                      <parent>
                        <groupId>org.jenkins-ci.plugins</groupId>
                        <artifactId>plugin</artifactId>
                        <version>4.87</version>
                        <relativePath />
                      </parent>
                      <artifactId>test-plugin</artifactId>
                      <version>${changelist}</version>
                      <packaging>hpi</packaging>
                      <properties>
                        <jenkins.version>2.440.3</jenkins.version>
                        <changelist>999999-SNAPSHOT</changelist>
                      </properties>
                    </project>
                    """),
                // language=yaml
                yaml("""
                    name: Release Drafter
                    on:
                      push:
                        branches:
                          - master
                    jobs:
                      update_release_draft:
                        runs-on: ubuntu-latest
                        steps:
                          - uses: release-drafter/release-drafter@v5
                    """, null, spec -> spec.path(ArchetypeCommonFile.RELEASE_DRAFTER_WORKFLOW.getPath())),
                text(null, EXPECTED_CD_WORKFLOW, spec -> spec.path(ArchetypeCommonFile.WORKFLOW_CD.getPath())),
                text(
                        null,
                        "-Dchangelist.format=%d.v%s",
                        spec -> spec.path(ArchetypeCommonFile.MAVEN_CONFIG.getPath())));
    }

    @Test
    void shouldConvertRevisionChangelistToChangelist() {
        rewriteRun(
                spec -> spec.recipe(new EnableCD()),
                // language=xml
                pomXml("""
                    <?xml version="1.0" encoding="UTF-8"?>
                    <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                        xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
                      <modelVersion>4.0.0</modelVersion>
                      <parent>
                        <groupId>org.jenkins-ci.plugins</groupId>
                        <artifactId>plugin</artifactId>
                        <version>4.87</version>
                        <relativePath />
                      </parent>
                      <artifactId>test-plugin</artifactId>
                      <version>${revision}${changelist}</version>
                      <packaging>hpi</packaging>
                      <name>Test Plugin</name>
                      <properties>
                        <jenkins.version>2.452.4</jenkins.version>
                        <revision>1.1.42</revision>
                        <changelist>-SNAPSHOT</changelist>
                      </properties>
                    </project>
                    """, """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                        xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
                      <modelVersion>4.0.0</modelVersion>
                      <parent>
                        <groupId>org.jenkins-ci.plugins</groupId>
                        <artifactId>plugin</artifactId>
                        <version>4.87</version>
                        <relativePath />
                      </parent>
                      <artifactId>test-plugin</artifactId>
                      <version>${changelist}</version>
                      <packaging>hpi</packaging>
                      <name>Test Plugin</name>
                      <properties>
                        <jenkins.version>2.452.4</jenkins.version>
                        <changelist>999999-SNAPSHOT</changelist>
                      </properties>
                    </project>
                    """),
                text(null, "-Dchangelist.format=%d.v%s", spec -> spec.path(ArchetypeCommonFile.MAVEN_CONFIG.getPath())),
                yaml(null, EXPECTED_CD_WORKFLOW, spec -> spec.path(ArchetypeCommonFile.WORKFLOW_CD.getPath())));
    }
}
