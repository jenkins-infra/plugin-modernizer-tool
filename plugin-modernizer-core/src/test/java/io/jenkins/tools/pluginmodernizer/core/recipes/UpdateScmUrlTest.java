package io.jenkins.tools.pluginmodernizer.core.recipes;

import static org.openrewrite.maven.Assertions.pomXml;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RewriteTest;

/**
 * Test for {@link UpdateScmUrl}.
 */
public class UpdateScmUrlTest implements RewriteTest {

    @Test
    void updateScmUrls() {
        rewriteRun(
                spec -> spec.recipe(new UpdateScmUrl()),
                // language=xml
                pomXml(
                        """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>org.jenkins-ci.plugins</groupId>
                    <artifactId>plugin</artifactId>
                    <version>4.88</version>
                    <packaging>hpi</packaging>
                    <name>Empty Plugin</name>
                    <properties>
                        <jenkins.version>2.440.3</jenkins.version>
                    </properties>
                    <scm>
                        <connection>scm:git:git://github.com/jenkinsci/your-plugin.git</connection>
                    </scm>
                    <repositories>
                        <repository>
                            <id>repo.jenkins-ci.org</id>
                            <url>https://repo.jenkins-ci.org/public/</url>
                        </repository>
                    </repositories>
                </project>
                """,
                        """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>org.jenkins-ci.plugins</groupId>
                    <artifactId>plugin</artifactId>
                    <version>4.88</version>
                    <packaging>hpi</packaging>
                    <name>Empty Plugin</name>
                    <properties>
                        <jenkins.version>2.440.3</jenkins.version>
                    </properties>
                    <scm>
                        <connection>scm:git:https://github.com/jenkinsci/your-plugin.git</connection>
                    </scm>
                    <repositories>
                        <repository>
                            <id>repo.jenkins-ci.org</id>
                            <url>https://repo.jenkins-ci.org/public/</url>
                        </repository>
                    </repositories>
                </project>
                """));
    }

    @Test
    void keepExistingHttpsUrls() {
        rewriteRun(
                spec -> spec.recipe(new UpdateScmUrl()),
                // language=xml
                pomXml(
                        """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>org.jenkins-ci.plugins</groupId>
                    <artifactId>plugin</artifactId>
                    <version>4.88</version>
                    <packaging>hpi</packaging>
                    <name>Empty Plugin</name>
                    <properties>
                        <jenkins.version>2.440.3</jenkins.version>
                    </properties>
                    <scm>
                        <connection>scm:git:https://github.com/jenkinsci/your-plugin.git</connection>
                    </scm>
                    <repositories>
                        <repository>
                            <id>repo.jenkins-ci.org</id>
                            <url>https://repo.jenkins-ci.org/public/</url>
                        </repository>
                    </repositories>
                </project>
                """));
    }
}
