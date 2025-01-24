package io.jenkins.tools.pluginmodernizer.core.recipes;

import static org.openrewrite.groovy.Assertions.groovy;
import static org.openrewrite.maven.Assertions.pomXml;

import io.jenkins.tools.pluginmodernizer.core.extractor.ArchetypeCommonFile;
import org.junit.jupiter.api.Test;
import org.openrewrite.test.RewriteTest;

class CreateJenkinsFileTest implements RewriteTest {

    @Test
    void shouldAddJenkinsfileFromPomVersion() {
        rewriteRun(
                spec -> spec.recipe(new CreateJenkinsFile()),
                pomXml(
                        """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>org.jenkins-ci.plugins</groupId>
                    <artifactId>plugin</artifactId>
                    <version>4.75</version>
                    <packaging>hpi</packaging>
                    <name>Test Plugin</name>
                    <properties>
                        <jenkins.version>2.452.4</jenkins.version>
                    </properties>
                    <repositories>
                        <repository>
                            <id>repo.jenkins-ci.org</id>
                            <url>https://repo.jenkins-ci.org/public/</url>
                        </repository>
                    </repositories>
                </project>
                """),
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
            )""",
                        spec -> spec.path(ArchetypeCommonFile.JENKINSFILE.getPath())));
    }

    @Test
    void shouldNotAddJenkinsfileIfAlreadyPresent() {
        rewriteRun(
                spec -> spec.recipe(new CreateJenkinsFile()),
                pomXml(
                        """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>org.jenkins-ci.plugins</groupId>
                    <artifactId>plugin</artifactId>
                    <version>4.75</version>
                    <packaging>hpi</packaging>
                    <properties>
                        <jenkins.version>2.452.4</jenkins.version>
                    </properties>
                </project>
                """),
                groovy("buildPlugin()", sourceSpecs -> sourceSpecs.path(ArchetypeCommonFile.JENKINSFILE.getPath())));
    }

    @Test
    void shouldNotAddJenkinsfileIfNoJenkinsVersion() {
        rewriteRun(
                spec -> spec.recipe(new CreateJenkinsFile()),
                pomXml(
                        """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>org.jenkins-ci.plugins</groupId>
                    <artifactId>plugin</artifactId>
                    <version>4.75</version>
                    <packaging>hpi</packaging>
                </project>
                """));
    }

    @Test
    void shouldHandleDifferentJenkinsVersions() {
        rewriteRun(
                spec -> spec.recipe(new CreateJenkinsFile()),
                pomXml(
                        """
            <?xml version="1.0" encoding="UTF-8"?>
            <project xmlns="http://maven.apache.org/POM/4.0.0"
                     xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                     xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
                <modelVersion>4.0.0</modelVersion>
                <groupId>org.jenkins-ci.plugins</groupId>
                <artifactId>test-plugin</artifactId>
                <version>1.0.0</version>
                <packaging>hpi</packaging>
                <properties>
                    <jenkins.version>2.401.3</jenkins.version>
                </properties>
            </project>
            """),
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
                    [platform: 'linux', jdk: 17],
                    [platform: 'windows', jdk: 11]
                ]
            )""",
                        spec -> spec.path(ArchetypeCommonFile.JENKINSFILE.getPath())));
    }

    @Test
    void shouldHandleOlderJenkinsVersions() {
        rewriteRun(
                spec -> spec.recipe(new CreateJenkinsFile()),
                pomXml(
                        """
            <?xml version="1.0" encoding="UTF-8"?>
            <project xmlns="http://maven.apache.org/POM/4.0.0"
                     xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                     xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
                <modelVersion>4.0.0</modelVersion>
                <groupId>org.jenkins-ci.plugins</groupId>
                <artifactId>test-plugin</artifactId>
                <version>1.0.0</version>
                <packaging>hpi</packaging>
                <properties>
                    <jenkins.version>2.164.3</jenkins.version>
                </properties>
            </project>
            """),
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
                    [platform: 'linux', jdk: 11],
                    [platform: 'windows', jdk: 8]
                ]
            )""",
                        spec -> spec.path(ArchetypeCommonFile.JENKINSFILE.getPath())));
    }
}
