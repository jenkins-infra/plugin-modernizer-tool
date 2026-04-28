package io.jenkins.tools.pluginmodernizer.core.recipes;

import static org.openrewrite.maven.Assertions.pomXml;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.openrewrite.test.RewriteTest;

/**
 * Test for {@link AddProperty}.
 */
@Execution(ExecutionMode.CONCURRENT)
public class AddPropertyTest implements RewriteTest {

    @Test
    void shouldAddProperty() {
        rewriteRun(
                spec -> spec.recipe(new AddProperty("ban-junit4-imports.skip", "false")),
                // language=xml
                pomXml("""
                        <?xml version="1.0" encoding="UTF-8"?>
                        <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
                          <modelVersion>4.0.0</modelVersion>
                          <parent>
                            <groupId>org.jenkins-ci.plugins</groupId>
                            <artifactId>plugin</artifactId>
                            <version>4.88</version>
                            <relativePath />
                          </parent>
                          <groupId>io.jenkins.plugins</groupId>
                          <artifactId>empty</artifactId>
                          <version>1.0.0-SNAPSHOT</version>
                          <packaging>hpi</packaging>
                          <name>Empty Plugin</name>
                          <properties>
                            <jenkins.version>2.440</jenkins.version>
                          </properties>
                          <repositories>
                            <repository>
                              <id>repo.jenkins-ci.org</id>
                              <url>https://repo.jenkins-ci.org/public/</url>
                            </repository>
                          </repositories>
                          <pluginRepositories>
                            <pluginRepository>
                              <id>repo.jenkins-ci.org</id>
                              <url>https://repo.jenkins-ci.org/public/</url>
                            </pluginRepository>
                          </pluginRepositories>
                        </project>
                        """, """
                        <?xml version="1.0" encoding="UTF-8"?>
                        <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
                          <modelVersion>4.0.0</modelVersion>
                          <parent>
                            <groupId>org.jenkins-ci.plugins</groupId>
                            <artifactId>plugin</artifactId>
                            <version>4.88</version>
                            <relativePath />
                          </parent>
                          <groupId>io.jenkins.plugins</groupId>
                          <artifactId>empty</artifactId>
                          <version>1.0.0-SNAPSHOT</version>
                          <packaging>hpi</packaging>
                          <name>Empty Plugin</name>
                          <properties>
                            <jenkins.version>2.440</jenkins.version>
                            <ban-junit4-imports.skip>false</ban-junit4-imports.skip>
                          </properties>
                          <repositories>
                            <repository>
                              <id>repo.jenkins-ci.org</id>
                              <url>https://repo.jenkins-ci.org/public/</url>
                            </repository>
                          </repositories>
                          <pluginRepositories>
                            <pluginRepository>
                              <id>repo.jenkins-ci.org</id>
                              <url>https://repo.jenkins-ci.org/public/</url>
                            </pluginRepository>
                          </pluginRepositories>
                        </project>
                        """));
    }

    @Test
    void shouldNotAddDuplicateProperty() {
        rewriteRun(
                spec -> spec.recipe(new AddProperty("ban-junit4-imports.skip", "false")),
                // language=xml
                pomXml("""
                        <?xml version="1.0" encoding="UTF-8"?>
                        <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
                          <modelVersion>4.0.0</modelVersion>
                          <parent>
                            <groupId>org.jenkins-ci.plugins</groupId>
                            <artifactId>plugin</artifactId>
                            <version>4.88</version>
                            <relativePath />
                          </parent>
                          <groupId>io.jenkins.plugins</groupId>
                          <artifactId>empty</artifactId>
                          <version>1.0.0-SNAPSHOT</version>
                          <packaging>hpi</packaging>
                          <name>Empty Plugin</name>
                          <properties>
                            <jenkins.version>2.440</jenkins.version>
                            <ban-junit4-imports.skip>false</ban-junit4-imports.skip>
                          </properties>
                          <repositories>
                            <repository>
                              <id>repo.jenkins-ci.org</id>
                              <url>https://repo.jenkins-ci.org/public/</url>
                            </repository>
                          </repositories>
                          <pluginRepositories>
                            <pluginRepository>
                              <id>repo.jenkins-ci.org</id>
                              <url>https://repo.jenkins-ci.org/public/</url>
                            </pluginRepository>
                          </pluginRepositories>
                        </project>
                        """));
    }
}
