package io.jenkins.tools.pluginmodernizer.core.recipes;

import static org.openrewrite.maven.Assertions.pomXml;

import io.jenkins.tools.pluginmodernizer.core.utils.Utils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.openrewrite.test.RewriteTest;

/**
 * Test for {@link RemoveDependencyManagementVersionOverride}.
 */
@Execution(ExecutionMode.CONCURRENT)
public class RemoveDependencyManagementVersionOverrideTest implements RewriteTest {

    @Test
    void shouldRemoveDependencyManagementVersionOverridesWhenUsingBom() {
        rewriteRun(
                spec -> spec.executionContext(Utils.getMavenExecutionContext())
                        .recipe(new RemoveDependencyManagementVersionOverride()),
                // language=xml
                pomXml("""
                        <?xml version="1.0" encoding="UTF-8"?>
                        <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
                          <modelVersion>4.0.0</modelVersion>
                          <parent>
                            <groupId>org.jenkins-ci.plugins</groupId>
                            <artifactId>plugin</artifactId>
                            <version>4.51</version>
                            <relativePath />
                          </parent>
                          <groupId>io.jenkins.plugins</groupId>
                          <artifactId>ascentialtest-cmd-line</artifactId>
                          <version>1.2-SNAPSHOT</version>
                          <packaging>hpi</packaging>
                          <properties>
                            <jenkins.baseline>2.346</jenkins.baseline>
                            <jenkins.version>${jenkins.baseline}.3</jenkins.version>
                          </properties>
                          <dependencyManagement>
                            <dependencies>
                              <dependency>
                                <groupId>io.jenkins.tools.bom</groupId>
                                <artifactId>bom-${jenkins.baseline}.x</artifactId>
                                <version>1763.v092b_8980a_f5e</version>
                                <type>pom</type>
                                <scope>import</scope>
                              </dependency>
                              <dependency>
                                <groupId>org.jenkins-ci.plugins</groupId>
                                <artifactId>script-security</artifactId>
                                <version>1.39</version>
                                <scope>test</scope>
                              </dependency>
                              <dependency>
                                <groupId>org.jenkins-ci.plugins</groupId>
                                <artifactId>scm-api</artifactId>
                                <version>2.2.6</version>
                                <scope>test</scope>
                              </dependency>
                            </dependencies>
                          </dependencyManagement>
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
                            <version>4.51</version>
                            <relativePath />
                          </parent>
                          <groupId>io.jenkins.plugins</groupId>
                          <artifactId>ascentialtest-cmd-line</artifactId>
                          <version>1.2-SNAPSHOT</version>
                          <packaging>hpi</packaging>
                          <properties>
                            <jenkins.baseline>2.346</jenkins.baseline>
                            <jenkins.version>${jenkins.baseline}.3</jenkins.version>
                          </properties>
                          <dependencyManagement>
                            <dependencies>
                              <dependency>
                                <groupId>io.jenkins.tools.bom</groupId>
                                <artifactId>bom-${jenkins.baseline}.x</artifactId>
                                <version>1763.v092b_8980a_f5e</version>
                                <type>pom</type>
                                <scope>import</scope>
                              </dependency>
                              <dependency>
                                <groupId>org.jenkins-ci.plugins</groupId>
                                <artifactId>script-security</artifactId>
                                <scope>test</scope>
                              </dependency>
                              <dependency>
                                <groupId>org.jenkins-ci.plugins</groupId>
                                <artifactId>scm-api</artifactId>
                                <scope>test</scope>
                              </dependency>
                            </dependencies>
                          </dependencyManagement>
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
    void shouldNotRemoveBomImportVersions() {
        rewriteRun(
                spec -> spec.executionContext(Utils.getMavenExecutionContext())
                        .recipe(new RemoveDependencyManagementVersionOverride()),
                // language=xml
                pomXml("""
                        <?xml version="1.0" encoding="UTF-8"?>
                        <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
                          <modelVersion>4.0.0</modelVersion>
                          <parent>
                            <groupId>org.jenkins-ci.plugins</groupId>
                            <artifactId>plugin</artifactId>
                            <version>4.51</version>
                            <relativePath />
                          </parent>
                          <groupId>io.jenkins.plugins</groupId>
                          <artifactId>empty</artifactId>
                          <version>1.0.0-SNAPSHOT</version>
                          <packaging>hpi</packaging>
                          <properties>
                            <jenkins.baseline>2.346</jenkins.baseline>
                            <jenkins.version>${jenkins.baseline}.3</jenkins.version>
                          </properties>
                          <dependencyManagement>
                            <dependencies>
                              <dependency>
                                <groupId>com.fasterxml.jackson</groupId>
                                <artifactId>jackson-bom</artifactId>
                                <version>2.17.0</version>
                                <type>pom</type>
                                <scope>import</scope>
                              </dependency>
                              <dependency>
                                <groupId>io.jenkins.tools.bom</groupId>
                                <artifactId>bom-${jenkins.baseline}.x</artifactId>
                                <version>1763.v092b_8980a_f5e</version>
                                <type>pom</type>
                                <scope>import</scope>
                              </dependency>
                              <dependency>
                                <groupId>org.jenkins-ci.plugins</groupId>
                                <artifactId>scm-api</artifactId>
                                <version>2.2.6</version>
                              </dependency>
                            </dependencies>
                          </dependencyManagement>
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
                            <version>4.51</version>
                            <relativePath />
                          </parent>
                          <groupId>io.jenkins.plugins</groupId>
                          <artifactId>empty</artifactId>
                          <version>1.0.0-SNAPSHOT</version>
                          <packaging>hpi</packaging>
                          <properties>
                            <jenkins.baseline>2.346</jenkins.baseline>
                            <jenkins.version>${jenkins.baseline}.3</jenkins.version>
                          </properties>
                          <dependencyManagement>
                            <dependencies>
                              <dependency>
                                <groupId>com.fasterxml.jackson</groupId>
                                <artifactId>jackson-bom</artifactId>
                                <version>2.17.0</version>
                                <type>pom</type>
                                <scope>import</scope>
                              </dependency>
                              <dependency>
                                <groupId>io.jenkins.tools.bom</groupId>
                                <artifactId>bom-${jenkins.baseline}.x</artifactId>
                                <version>1763.v092b_8980a_f5e</version>
                                <type>pom</type>
                                <scope>import</scope>
                              </dependency>
                              <dependency>
                                <groupId>org.jenkins-ci.plugins</groupId>
                                <artifactId>scm-api</artifactId>
                              </dependency>
                            </dependencies>
                          </dependencyManagement>
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
    void shouldNotRemoveVersionsWhenBomIsMissing() {
        rewriteRun(
                spec -> spec.executionContext(Utils.getMavenExecutionContext())
                        .recipe(new RemoveDependencyManagementVersionOverride()),
                // language=xml
                pomXml("""
                        <?xml version="1.0" encoding="UTF-8"?>
                        <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
                          <modelVersion>4.0.0</modelVersion>
                          <parent>
                            <groupId>org.jenkins-ci.plugins</groupId>
                            <artifactId>plugin</artifactId>
                            <version>4.51</version>
                            <relativePath />
                          </parent>
                          <groupId>io.jenkins.plugins</groupId>
                          <artifactId>empty</artifactId>
                          <version>1.0.0-SNAPSHOT</version>
                          <packaging>hpi</packaging>
                          <dependencyManagement>
                            <dependencies>
                              <dependency>
                                <groupId>org.jenkins-ci.plugins</groupId>
                                <artifactId>scm-api</artifactId>
                                <version>2.2.6</version>
                              </dependency>
                            </dependencies>
                          </dependencyManagement>
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
