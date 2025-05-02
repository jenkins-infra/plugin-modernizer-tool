package io.jenkins.tools.pluginmodernizer.core.recipes;

import static org.openrewrite.groovy.Assertions.groovy;
import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.java.Assertions.mavenProject;
import static org.openrewrite.java.Assertions.srcMainJava;
import static org.openrewrite.java.Assertions.srcMainResources;
import static org.openrewrite.java.Assertions.srcTestJava;
import static org.openrewrite.maven.Assertions.pomXml;
import static org.openrewrite.test.SourceSpecs.text;
import static org.openrewrite.yaml.Assertions.yaml;

import io.github.yamlpath.YamlPath;
import io.jenkins.tools.pluginmodernizer.core.config.Settings;
import io.jenkins.tools.pluginmodernizer.core.extractor.ArchetypeCommonFile;
import io.jenkins.tools.pluginmodernizer.core.recipes.code.ReplaceRemovedSSHLauncherConstructorTest;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.openrewrite.java.JavaParser;
import org.openrewrite.maven.MavenParser;
import org.openrewrite.test.RewriteTest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Test for declarative recipes from recipes.yml.
 */
// ConcurrentModification on rewriteRun (need fix upstream?
// This only occur on recipeFromResource
@Execution(ExecutionMode.SAME_THREAD)
public class DeclarativeRecipesTest implements RewriteTest {

    @Language("xml")
    public static final String EXPECTED_JELLY =
            """
            <?jelly escape-by-default='true'?>
            <div>
               empty
            </div>
            """;

    @Language("groovy")
    private static final String EXPECTED_MODERN_JENKINSFILE =
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
            )""";

    @Language("groovy")
    // For example 2.401 was not supporting yet officially Java 21
    private static final String NO_JAVA_21_SUPPORT_JENKINSFILE =
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
            )""";

    @Language("groovy")
    private static final String JAVA_8_JENKINS_FILE =
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
            )""";

    /**
     * LOGGER.
     */
    private static final Logger LOG = LoggerFactory.getLogger(DeclarativeRecipesTest.class);

    /**
     * Test declarative recipe
     * See @{{@link io.jenkins.tools.pluginmodernizer.core.extractor.FetchMetadataTest} for more tests
     */
    @Test
    void fetchMinimalMetadata() {
        rewriteRun(
                spec -> spec.recipeFromResource(
                        "/META-INF/rewrite/recipes.yml", "io.jenkins.tools.pluginmodernizer.FetchMetadata"),
                // language=xml
                pomXml(
                        """
                        <?xml version="1.0" encoding="UTF-8"?>
                        <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
                          <modelVersion>4.0.0</modelVersion>
                          <groupId>io.jenkins.plugins</groupId>
                          <artifactId>empty</artifactId>
                          <version>1.0.0-SNAPSHOT</version>
                          <name>Empty pom</name>
                        </project>
                        """));
    }

    @Test
    @EnabledOnOs(OS.LINUX) // https://github.com/openrewrite/rewrite-jenkins/pull/83
    void addCodeOwner() {
        rewriteRun(
                spec -> spec.recipeFromResource(
                        "/META-INF/rewrite/recipes.yml", "io.jenkins.tools.pluginmodernizer.AddCodeOwner"),
                // language=xml
                pomXml(
                        """
                        <?xml version="1.0" encoding="UTF-8"?>
                        <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
                          <modelVersion>4.0.0</modelVersion>
                          <groupId>io.jenkins.plugins</groupId>
                          <artifactId>empty</artifactId>
                          <version>1.0.0-SNAPSHOT</version>
                          <name>Empty pom</name>
                        </project>
                        """),
                text(null, "* @jenkinsci/empty-plugin-developers", sourceSpecs -> {
                    sourceSpecs.path(ArchetypeCommonFile.CODEOWNERS.getPath());
                }));
    }

    @Test
    @EnabledOnOs(OS.LINUX) // https://github.com/openrewrite/rewrite-jenkins/pull/83
    void shouldNotAddCodeOwnerIfAdded() {
        rewriteRun(
                spec -> spec.recipeFromResource(
                        "/META-INF/rewrite/recipes.yml", "io.jenkins.tools.pluginmodernizer.AddCodeOwner"),
                // language=xml
                pomXml(
                        """
                        <?xml version="1.0" encoding="UTF-8"?>
                        <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
                          <modelVersion>4.0.0</modelVersion>
                          <groupId>io.jenkins.plugins</groupId>
                          <artifactId>empty</artifactId>
                          <version>1.0.0-SNAPSHOT</version>
                          <name>Empty pom</name>
                        </project>
                        """),
                text("* @jenkinsci/empty-plugin-developers", sourceSpecs -> {
                    sourceSpecs.path(ArchetypeCommonFile.CODEOWNERS.getPath());
                }));
    }

    @Test
    @EnabledOnOs(OS.LINUX) // https://github.com/openrewrite/rewrite-jenkins/pull/83
    void shouldAddCodeOwnerIfNeeded() {
        rewriteRun(
                spec -> spec.recipeFromResource(
                        "/META-INF/rewrite/recipes.yml", "io.jenkins.tools.pluginmodernizer.AddCodeOwner"),
                // language=xml
                pomXml(
                        """
                        <?xml version="1.0" encoding="UTF-8"?>
                        <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
                          <modelVersion>4.0.0</modelVersion>
                          <groupId>io.jenkins.plugins</groupId>
                          <artifactId>empty</artifactId>
                          <version>1.0.0-SNAPSHOT</version>
                          <name>Empty pom</name>
                        </project>
                        """),
                text(
                        """
                        * @my-custom-team
                        """,
                        """
                        * @jenkinsci/empty-plugin-developers
                        * @my-custom-team
                    """,
                        sourceSpecs -> {
                            sourceSpecs.path(ArchetypeCommonFile.CODEOWNERS.getPath());
                        }));
    }

    @Test
    void upgradeParentpom() {
        rewriteRun(
                spec -> spec.recipeFromResource(
                        "/META-INF/rewrite/recipes.yml", "io.jenkins.tools.pluginmodernizer.UpgradeParentVersion"),
                // language=xml
                pomXml(
                        """
                        <?xml version="1.0" encoding="UTF-8"?>
                        <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
                          <modelVersion>4.0.0</modelVersion>
                          <parent>
                            <groupId>org.jenkins-ci.plugins</groupId>
                            <artifactId>plugin</artifactId>
                            <version>4.87</version>
                            <relativePath />
                          </parent>
                          <groupId>io.jenkins.plugins</groupId>
                          <artifactId>empty</artifactId>
                          <version>1.0.0-SNAPSHOT</version>
                          <packaging>hpi</packaging>
                          <name>Empty Plugin</name>
                          <properties>
                            <jenkins.version>2.440.3</jenkins.version>
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
                        """,
                        """
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
                            <jenkins.version>2.440.3</jenkins.version>
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
    void testUpgradeBomVersion() {
        rewriteRun(
                spec -> spec.recipeFromResource(
                        "/META-INF/rewrite/recipes.yml", "io.jenkins.tools.pluginmodernizer.UpgradeBomVersion"),
                // language=xml
                pomXml(
                        """
                        <?xml version="1.0" encoding="UTF-8"?>
                        <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
                          <modelVersion>4.0.0</modelVersion>
                          <parent>
                            <groupId>org.jenkins-ci.plugins</groupId>
                            <artifactId>plugin</artifactId>
                            <version>4.87</version>
                            <relativePath />
                          </parent>
                          <groupId>io.jenkins.plugins</groupId>
                          <artifactId>empty</artifactId>
                          <version>1.0.0-SNAPSHOT</version>
                          <packaging>hpi</packaging>
                          <name>Empty Plugin</name>
                          <properties>
                             <jenkins.version>2.440.3</jenkins.version>
                          </properties>
                          <dependencyManagement>
                            <dependencies>
                              <dependency>
                                <groupId>io.jenkins.tools.bom</groupId>
                                <artifactId>bom-2.440.x</artifactId>
                                <version>3120.v4d898e1e9fc4</version>
                                <type>pom</type>
                                <scope>import</scope>
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
                        """,
                        """
                        <?xml version="1.0" encoding="UTF-8"?>
                        <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
                          <modelVersion>4.0.0</modelVersion>
                          <parent>
                            <groupId>org.jenkins-ci.plugins</groupId>
                            <artifactId>plugin</artifactId>
                            <version>4.87</version>
                            <relativePath />
                          </parent>
                          <groupId>io.jenkins.plugins</groupId>
                          <artifactId>empty</artifactId>
                          <version>1.0.0-SNAPSHOT</version>
                          <packaging>hpi</packaging>
                          <name>Empty Plugin</name>
                          <properties>
                             <jenkins.version>2.440.3</jenkins.version>
                          </properties>
                          <dependencyManagement>
                            <dependencies>
                              <dependency>
                                <groupId>io.jenkins.tools.bom</groupId>
                                <artifactId>bom-2.440.x</artifactId>
                                <version>3435.v238d66a_043fb_</version>
                                <type>pom</type>
                                <scope>import</scope>
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
                        """
                                .formatted(Settings.getJenkinsParentVersion(), Settings.getBomVersion())));
    }

    @Test
    void testUpgradeOldBomVersionFormat() {
        rewriteRun(
                spec -> spec.recipeFromResource(
                        "/META-INF/rewrite/recipes.yml", "io.jenkins.tools.pluginmodernizer.UpgradeBomVersion"),
                // language=xml
                pomXml(
                        """
                        <?xml version="1.0" encoding="UTF-8"?>
                        <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
                          <modelVersion>4.0.0</modelVersion>
                          <parent>
                            <groupId>org.jenkins-ci.plugins</groupId>
                            <artifactId>plugin</artifactId>
                            <version>4.0</version>
                            <relativePath />
                          </parent>
                          <groupId>io.jenkins.plugins</groupId>
                          <artifactId>empty</artifactId>
                          <version>1.0.0-SNAPSHOT</version>
                          <packaging>hpi</packaging>
                          <name>Empty Plugin</name>
                          <properties>
                             <jenkins.version>2.164.3</jenkins.version>
                          </properties>
                          <dependencyManagement>
                            <dependencies>
                              <dependency>
                                <groupId>io.jenkins.tools.bom</groupId>
                                <artifactId>bom-2.164.x</artifactId>
                                <version>3</version>
                                <type>pom</type>
                                <scope>import</scope>
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
                        """,
                        """
                        <?xml version="1.0" encoding="UTF-8"?>
                        <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
                          <modelVersion>4.0.0</modelVersion>
                          <parent>
                            <groupId>org.jenkins-ci.plugins</groupId>
                            <artifactId>plugin</artifactId>
                            <version>4.0</version>
                            <relativePath />
                          </parent>
                          <groupId>io.jenkins.plugins</groupId>
                          <artifactId>empty</artifactId>
                          <version>1.0.0-SNAPSHOT</version>
                          <packaging>hpi</packaging>
                          <name>Empty Plugin</name>
                          <properties>
                             <jenkins.version>2.164.3</jenkins.version>
                          </properties>
                          <dependencyManagement>
                            <dependencies>
                              <dependency>
                                <groupId>io.jenkins.tools.bom</groupId>
                                <artifactId>bom-2.164.x</artifactId>
                                <version>10</version>
                                <type>pom</type>
                                <scope>import</scope>
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
    void testRemoveDependenciesOverride() {
        rewriteRun(
                spec -> spec.recipeFromResource(
                        "/META-INF/rewrite/recipes.yml",
                        "io.jenkins.tools.pluginmodernizer.RemoveDependencyVersionOverride"),
                // language=xml
                pomXml(
                        """
                        <?xml version="1.0" encoding="UTF-8"?>
                        <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
                          <modelVersion>4.0.0</modelVersion>
                          <parent>
                            <groupId>org.jenkins-ci.plugins</groupId>
                            <artifactId>plugin</artifactId>
                            <version>4.87</version>
                            <relativePath />
                          </parent>
                          <groupId>io.jenkins.plugins</groupId>
                          <artifactId>empty</artifactId>
                          <version>1.0.0-SNAPSHOT</version>
                          <packaging>hpi</packaging>
                          <name>Empty Plugin</name>
                          <properties>
                             <jenkins.version>2.440.3</jenkins.version>
                          </properties>
                          <dependencyManagement>
                            <dependencies>
                              <dependency>
                                <groupId>io.jenkins.tools.bom</groupId>
                                <artifactId>bom-2.440.x</artifactId>
                                <version>3120.v4d898e1e9fc4</version>
                                <type>pom</type>
                                <scope>import</scope>
                              </dependency>
                            </dependencies>
                          </dependencyManagement>
                          <dependencies>
                            <dependency>
                              <groupId>io.jenkins.plugins</groupId>
                              <artifactId>json-api</artifactId>
                              <version>20240303-41.v94e11e6de726</version>
                            </dependency>
                          </dependencies>
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
                        """,
                        """
                        <?xml version="1.0" encoding="UTF-8"?>
                        <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
                          <modelVersion>4.0.0</modelVersion>
                          <parent>
                            <groupId>org.jenkins-ci.plugins</groupId>
                            <artifactId>plugin</artifactId>
                            <version>4.87</version>
                            <relativePath />
                          </parent>
                          <groupId>io.jenkins.plugins</groupId>
                          <artifactId>empty</artifactId>
                          <version>1.0.0-SNAPSHOT</version>
                          <packaging>hpi</packaging>
                          <name>Empty Plugin</name>
                          <properties>
                             <jenkins.version>2.440.3</jenkins.version>
                          </properties>
                          <dependencyManagement>
                            <dependencies>
                              <dependency>
                                <groupId>io.jenkins.tools.bom</groupId>
                                <artifactId>bom-2.440.x</artifactId>
                                <version>3120.v4d898e1e9fc4</version>
                                <type>pom</type>
                                <scope>import</scope>
                              </dependency>
                            </dependencies>
                          </dependencyManagement>
                          <dependencies>
                            <dependency>
                              <groupId>io.jenkins.plugins</groupId>
                              <artifactId>json-api</artifactId>
                            </dependency>
                          </dependencies>
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
                        """
                                .formatted(Settings.getJenkinsParentVersion(), Settings.getBomVersion())));
    }

    @Test
    void shouldRemoveExtraProperties() {
        rewriteRun(
                spec -> spec.recipeFromResource(
                        "/META-INF/rewrite/recipes.yml",
                        "io.jenkins.tools.pluginmodernizer.RemoveExtraMavenProperties"),
                // language=xml
                pomXml(
                        """
                        <?xml version="1.0" encoding="UTF-8"?>
                        <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
                          <modelVersion>4.0.0</modelVersion>
                          <groupId>io.jenkins.plugins</groupId>
                          <artifactId>empty</artifactId>
                          <version>1.0.0-SNAPSHOT</version>
                          <name>Empty pom</name>
                          <properties>
                            <configuration-as-code.version>1909.vb_b_f59a_27d013</configuration-as-code.version>
                            <java.version>11</java.version>
                          </properties>
                        </project>
                        """,
                        """
                        <?xml version="1.0" encoding="UTF-8"?>
                        <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
                          <modelVersion>4.0.0</modelVersion>
                          <groupId>io.jenkins.plugins</groupId>
                          <artifactId>empty</artifactId>
                          <version>1.0.0-SNAPSHOT</version>
                          <name>Empty pom</name>
                        </project>
                        """));
    }

    @Test
    void removeDevelopersTag() {
        rewriteRun(
                spec -> spec.recipeFromResource(
                        "/META-INF/rewrite/recipes.yml", "io.jenkins.tools.pluginmodernizer.RemoveDevelopersTag"),
                // language=xml
                pomXml(
                        """
                        <?xml version="1.0" encoding="UTF-8"?>
                        <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
                          <modelVersion>4.0.0</modelVersion>
                          <groupId>io.jenkins.plugins</groupId>
                          <artifactId>empty</artifactId>
                          <version>1.0.0-SNAPSHOT</version>
                          <name>Empty pom</name>
                          <developers>
                            <developer>
                              <id>bhacker</id>
                              <name>Bob Q. Hacker</name>
                              <email>bhacker@nowhere.net</email>
                            </developer>
                          </developers>
                        </project>
                        """,
                        """
                        <?xml version="1.0" encoding="UTF-8"?>
                        <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
                          <modelVersion>4.0.0</modelVersion>
                          <groupId>io.jenkins.plugins</groupId>
                          <artifactId>empty</artifactId>
                          <version>1.0.0-SNAPSHOT</version>
                          <name>Empty pom</name>
                        </project>
                        """));
    }

    @Test
    void upgradeToRecommendCoreVersionTest() {
        rewriteRun(
                spec -> spec.recipeFromResource(
                        "/META-INF/rewrite/recipes.yml",
                        "io.jenkins.tools.pluginmodernizer.UpgradeToRecommendCoreVersion"),
                // language=yaml
                yaml("{}", sourceSpecs -> {
                    sourceSpecs.path(ArchetypeCommonFile.WORKFLOW_CD.getPath());
                }),
                yaml("{}", sourceSpecs -> {
                    sourceSpecs.path(ArchetypeCommonFile.DEPENDABOT.getPath());
                }),
                // language=xml
                srcMainResources(text(
                        null,
                        EXPECTED_JELLY,
                        s -> s.path(ArchetypeCommonFile.INDEX_JELLY.getPath().getFileName()))),
                // language=groovy
                groovy(
                        null,
                        EXPECTED_MODERN_JENKINSFILE,
                        s -> s.path(ArchetypeCommonFile.JENKINSFILE.getPath().getFileName())),
                // language=xml
                pomXml(
                        """
                        <?xml version="1.0" encoding="UTF-8"?>
                        <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
                          <modelVersion>4.0.0</modelVersion>
                          <parent>
                            <groupId>org.jenkins-ci.plugins</groupId>
                            <artifactId>plugin</artifactId>
                            <version>4.87</version>
                          </parent>
                          <groupId>io.jenkins.plugins</groupId>
                          <artifactId>empty</artifactId>
                          <version>1.0.0-SNAPSHOT</version>
                          <packaging>hpi</packaging>
                          <name>Empty Plugin</name>
                          <developers>
                            <developer>
                              <id>bhacker</id>
                              <name>Bob Q. Hacker</name>
                              <email>bhacker@nowhere.net</email>
                            </developer>
                          </developers>
                          <scm>
                            <connection>scm:git:git://github.com/jenkinsci/empty-plugin.git</connection>
                          </scm>
                          <properties>
                            <jenkins-test-harness.version>2.41.1</jenkins-test-harness.version>
                            <jenkins.version>2.440.3</jenkins.version>
                          </properties>
                          <dependencies>
                            <dependency>
                              <groupId>io.jenkins.plugins</groupId>
                              <artifactId>asm-api</artifactId>
                              <version>9.6-3.v2e1fa_b_338cd7</version>
                            </dependency>
                            <dependency>
                              <groupId>org.jenkins-ci.main</groupId>
                              <artifactId>jenkins-test-harness</artifactId>
                              <version>2.41.1</version>
                            </dependency>
                            <dependency>
                              <groupId>com.github.tomakehurst</groupId>
                              <artifactId>wiremock-jre8-standalone</artifactId>
                              <version>2.35.2</version>
                              <scope>test</scope>
                            </dependency>
                          </dependencies>
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
                        """,
                        """
                        <?xml version="1.0" encoding="UTF-8"?>
                        <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
                          <modelVersion>4.0.0</modelVersion>
                          <parent>
                            <groupId>org.jenkins-ci.plugins</groupId>
                            <artifactId>plugin</artifactId>
                            <version>4.88</version>
                            <relativePath/>
                          </parent>
                          <groupId>io.jenkins.plugins</groupId>
                          <artifactId>empty</artifactId>
                          <version>1.0.0-SNAPSHOT</version>
                          <packaging>hpi</packaging>
                          <name>Empty Plugin</name>
                          <scm>
                            <connection>scm:git:https://github.com/jenkinsci/empty-plugin.git</connection>
                          </scm>
                          <properties>
                            <jenkins-test-harness.version>2225.v04fa_3929c9b_5</jenkins-test-harness.version>
                            <!-- https://www.jenkins.io/doc/developer/plugin-development/choosing-jenkins-baseline/ -->
                            <jenkins.baseline>%s</jenkins.baseline>
                            <jenkins.version>${jenkins.baseline}.%s</jenkins.version>
                          </properties>
                          <dependencyManagement>
                            <dependencies>
                              <dependency>
                                <groupId>io.jenkins.tools.bom</groupId>
                                <artifactId>bom-${jenkins.baseline}.x</artifactId>
                                <version>%s</version>
                                <type>pom</type>
                                <scope>import</scope>
                              </dependency>
                            </dependencies>
                          </dependencyManagement>
                          <dependencies>
                            <dependency>
                              <groupId>io.jenkins.plugins</groupId>
                              <artifactId>asm-api</artifactId>
                            </dependency>
                            <dependency>
                              <groupId>org.jenkins-ci.main</groupId>
                              <artifactId>jenkins-test-harness</artifactId>
                            </dependency>
                            <dependency>
                              <groupId>org.wiremock</groupId>
                              <artifactId>wiremock-standalone</artifactId>
                              <version>%s</version>
                              <scope>test</scope>
                            </dependency>
                          </dependencies>
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
                        """
                                .formatted(
                                        Settings.getJenkinsMinimumBaseline(),
                                        Settings.getJenkinsMinimumPatchVersion(),
                                        Settings.getRecommendedBomVersion(),
                                        Settings.getWiremockVersion())));
    }

    @Test
    void upgradeToRecommendCoreVersionTestWithoutPluginDependencies() {
        rewriteRun(
                spec -> spec.recipeFromResource(
                        "/META-INF/rewrite/recipes.yml",
                        "io.jenkins.tools.pluginmodernizer.UpgradeToRecommendCoreVersion"),
                // language=xml
                srcMainResources(text(
                        null,
                        EXPECTED_JELLY,
                        s -> s.path(ArchetypeCommonFile.INDEX_JELLY.getPath().getFileName()))),
                // language=groovy
                groovy(
                        null,
                        EXPECTED_MODERN_JENKINSFILE,
                        s -> s.path(ArchetypeCommonFile.JENKINSFILE.getPath().getFileName())),
                // language=xml
                pomXml(
                        """
                        <?xml version="1.0" encoding="UTF-8"?>
                        <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
                          <modelVersion>4.0.0</modelVersion>
                          <parent>
                            <groupId>org.jenkins-ci.plugins</groupId>
                            <artifactId>plugin</artifactId>
                            <version>4.87</version>
                          </parent>
                          <groupId>io.jenkins.plugins</groupId>
                          <artifactId>empty</artifactId>
                          <version>1.0.0-SNAPSHOT</version>
                          <packaging>hpi</packaging>
                          <name>Empty Plugin</name>
                          <developers>
                            <developer>
                              <id>bhacker</id>
                              <name>Bob Q. Hacker</name>
                              <email>bhacker@nowhere.net</email>
                            </developer>
                          </developers>
                          <scm>
                            <connection>scm:git:git://github.com/jenkinsci/empty-plugin.git</connection>
                          </scm>
                          <properties>
                            <jenkins.version>2.440.3</jenkins.version>
                            <jenkins-test-harness.version>2.41.1</jenkins-test-harness.version>
                             <maven.compiler.source>17</maven.compiler.source>
                             <maven.compiler.release>17</maven.compiler.release>
                             <maven.compiler.target>17</maven.compiler.target>
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
                        """,
                        """
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
                          <scm>
                            <connection>scm:git:https://github.com/jenkinsci/empty-plugin.git</connection>
                          </scm>
                          <properties>
                            <jenkins.version>%s</jenkins.version>
                            <jenkins-test-harness.version>2225.v04fa_3929c9b_5</jenkins-test-harness.version>
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
                        """
                                .formatted(Settings.getJenkinsMinimumVersion())));
    }

    @Test
    void upgradeToRecommendCoreVersionTestWithBaseline() {
        rewriteRun(
                spec -> spec.recipeFromResource(
                        "/META-INF/rewrite/recipes.yml",
                        "io.jenkins.tools.pluginmodernizer.UpgradeToRecommendCoreVersion"),
                // language=xml
                srcMainResources(text(
                        null,
                        EXPECTED_JELLY,
                        s -> s.path(ArchetypeCommonFile.INDEX_JELLY.getPath().getFileName()))),
                // language=groovy
                groovy(
                        null,
                        EXPECTED_MODERN_JENKINSFILE,
                        s -> s.path(ArchetypeCommonFile.JENKINSFILE.getPath().getFileName())),
                // language=xml
                pomXml(
                        """
                        <?xml version="1.0" encoding="UTF-8"?>
                        <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
                          <modelVersion>4.0.0</modelVersion>
                          <parent>
                            <groupId>org.jenkins-ci.plugins</groupId>
                            <artifactId>plugin</artifactId>
                            <version>4.87</version>
                          </parent>
                          <groupId>io.jenkins.plugins</groupId>
                          <artifactId>empty</artifactId>
                          <version>1.0.0-SNAPSHOT</version>
                          <packaging>hpi</packaging>
                          <name>Empty Plugin</name>
                          <developers>
                            <developer>
                              <id>bhacker</id>
                              <name>Bob Q. Hacker</name>
                              <email>bhacker@nowhere.net</email>
                            </developer>
                          </developers>
                          <properties>
                            <jenkins-test-harness.version>2.41.1</jenkins-test-harness.version>
                            <jenkins.baseline>2.440</jenkins.baseline>
                            <jenkins.version>${jenkins.baseline}.3</jenkins.version>
                          </properties>
                          <dependencyManagement>
                            <dependencies>
                              <dependency>
                                <groupId>io.jenkins.tools.bom</groupId>
                                <artifactId>bom-${jenkins.baseline}.x</artifactId>
                                <version>3435.v238d66a_043fb_</version>
                                <type>pom</type>
                                <scope>import</scope>
                              </dependency>
                            </dependencies>
                          </dependencyManagement>
                            <dependencies>
                              <dependency>
                                <groupId>org.jenkins-ci.main</groupId>
                                <artifactId>jenkins-test-harness</artifactId>
                                <version>2.41.1</version>
                              </dependency>
                            </dependencies>
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
                        """,
                        """
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
                            <jenkins-test-harness.version>2225.v04fa_3929c9b_5</jenkins-test-harness.version>
                            <!-- https://www.jenkins.io/doc/developer/plugin-development/choosing-jenkins-baseline/ -->
                            <jenkins.baseline>%s</jenkins.baseline>
                            <jenkins.version>${jenkins.baseline}.%s</jenkins.version>
                          </properties>
                          <dependencyManagement>
                            <dependencies>
                              <dependency>
                                <groupId>io.jenkins.tools.bom</groupId>
                                <artifactId>bom-${jenkins.baseline}.x</artifactId>
                                <version>%s</version>
                                <type>pom</type>
                                <scope>import</scope>
                              </dependency>
                            </dependencies>
                          </dependencyManagement>
                            <dependencies>
                              <dependency>
                                <groupId>org.jenkins-ci.main</groupId>
                                <artifactId>jenkins-test-harness</artifactId>
                              </dependency>
                            </dependencies>
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
                        """
                                .formatted(
                                        Settings.getJenkinsMinimumBaseline(),
                                        Settings.getJenkinsMinimumPatchVersion(),
                                        Settings.getRecommendedBomVersion())));
    }

    @Test
    void upgradeToRecommendCoreVersionTestWithMultipleBom() {
        rewriteRun(
                spec -> {
                    spec.parser(MavenParser.builder().activeProfiles("consume-incrementals"));
                    spec.recipeFromResource(
                            "/META-INF/rewrite/recipes.yml",
                            "io.jenkins.tools.pluginmodernizer.UpgradeToRecommendCoreVersion");
                },
                // language=xml
                srcMainResources(text(
                        null,
                        EXPECTED_JELLY,
                        s -> s.path(ArchetypeCommonFile.INDEX_JELLY.getPath().getFileName()))),
                // language=groovy
                groovy(
                        null,
                        NO_JAVA_21_SUPPORT_JENKINSFILE,
                        s -> s.path(ArchetypeCommonFile.JENKINSFILE.getPath().getFileName())),
                // language=xml
                pomXml(
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
                      <artifactId>empty</artifactId>
                      <version>${revision}-${changelist}</version>
                      <packaging>hpi</packaging>
                      <name>My API Plugin</name>
                      <developers>
                        <developer>
                          <id>bhacker</id>
                          <name>Bob Q. Hacker</name>
                          <email>bhacker@nowhere.net</email>
                        </developer>
                      </developers>
                      <properties>
                        <jenkins-test-harness.version>2.41.1</jenkins-test-harness.version>
                        <revision>2.17.0</revision>
                        <changelist>999999-SNAPSHOT</changelist>
                        <jenkins.version>2.401.3</jenkins.version>
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
                      <dependencyManagement>
                        <dependencies>
                          <dependency>
                            <groupId>com.fasterxml.jackson</groupId>
                            <artifactId>jackson-bom</artifactId>
                            <version>2.17.0</version>
                            <scope>import</scope>
                            <type>pom</type>
                          </dependency>
                          <dependency>
                            <groupId>io.jenkins.tools.bom</groupId>
                            <artifactId>bom-2.401.x</artifactId>
                            <version>2745.vc7b_fe4c876fa_</version>
                            <scope>import</scope>
                            <type>pom</type>
                          </dependency>
                        </dependencies>
                      </dependencyManagement>
                      <dependencies>
                        <dependency>
                          <groupId>com.fasterxml.jackson.core</groupId>
                          <artifactId>jackson-databind</artifactId>
                        </dependency>
                        <dependency>
                          <groupId>org.jenkins-ci.main</groupId>
                          <artifactId>jenkins-test-harness</artifactId>
                          <version>2.41.1</version>
                        </dependency>
                        <dependency>
                          <groupId>io.jenkins.plugins</groupId>
                          <artifactId>json-api</artifactId>
                        </dependency>
                        <dependency>
                          <groupId>com.github.tomakehurst</groupId>
                          <artifactId>wiremock</artifactId>
                          <version>3.0.1</version>
                          <scope>test</scope>
                        </dependency>
                      </dependencies>
                    </project>
                    """,
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
                      <artifactId>empty</artifactId>
                      <version>${revision}-${changelist}</version>
                      <packaging>hpi</packaging>
                      <name>My API Plugin</name>
                      <properties>
                        <jenkins-test-harness.version>2225.v04fa_3929c9b_5</jenkins-test-harness.version>
                        <revision>2.17.0</revision>
                        <changelist>999999-SNAPSHOT</changelist>
                        <!-- https://www.jenkins.io/doc/developer/plugin-development/choosing-jenkins-baseline/ -->
                        <jenkins.baseline>%s</jenkins.baseline>
                        <jenkins.version>${jenkins.baseline}.%s</jenkins.version>
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
                      <dependencyManagement>
                        <dependencies>
                          <dependency>
                            <groupId>com.fasterxml.jackson</groupId>
                            <artifactId>jackson-bom</artifactId>
                            <version>2.17.0</version>
                            <scope>import</scope>
                            <type>pom</type>
                          </dependency>
                          <dependency>
                            <groupId>io.jenkins.tools.bom</groupId>
                            <artifactId>bom-${jenkins.baseline}.x</artifactId>
                            <version>%s</version>
                            <scope>import</scope>
                            <type>pom</type>
                          </dependency>
                        </dependencies>
                      </dependencyManagement>
                      <dependencies>
                        <dependency>
                          <groupId>com.fasterxml.jackson.core</groupId>
                          <artifactId>jackson-databind</artifactId>
                        </dependency>
                        <dependency>
                          <groupId>org.jenkins-ci.main</groupId>
                          <artifactId>jenkins-test-harness</artifactId>
                        </dependency>
                        <dependency>
                          <groupId>io.jenkins.plugins</groupId>
                          <artifactId>json-api</artifactId>
                        </dependency>
                        <dependency>
                          <groupId>org.wiremock</groupId>
                          <artifactId>wiremock</artifactId>
                          <version>%s</version>
                          <scope>test</scope>
                        </dependency>
                      </dependencies>
                    </project>
                    """
                                .formatted(
                                        Settings.getJenkinsMinimumBaseline(),
                                        Settings.getJenkinsMinimumPatchVersion(),
                                        Settings.getRecommendedBomVersion(),
                                        Settings.getWiremockVersion())));
    }

    @Test
    void upgradeToUpgradeToLatestJava11CoreVersion() {
        rewriteRun(
                spec -> spec.recipeFromResource(
                        "/META-INF/rewrite/recipes.yml",
                        "io.jenkins.tools.pluginmodernizer.UpgradeToLatestJava11CoreVersion"),
                // language=xml
                srcMainResources(text(
                        null,
                        EXPECTED_JELLY,
                        s -> s.path(ArchetypeCommonFile.INDEX_JELLY.getPath().getFileName()))),
                // language=groovy
                groovy(
                        null,
                        EXPECTED_MODERN_JENKINSFILE,
                        s -> s.path(ArchetypeCommonFile.JENKINSFILE.getPath().getFileName())),
                // language=xml
                pomXml(
                        """
                        <?xml version="1.0" encoding="UTF-8"?>
                        <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
                          <modelVersion>4.0.0</modelVersion>
                          <parent>
                            <groupId>org.jenkins-ci.plugins</groupId>
                            <artifactId>plugin</artifactId>
                            <version>4.55</version>
                          </parent>
                          <groupId>io.jenkins.plugins</groupId>
                          <artifactId>empty</artifactId>
                          <version>1.0.0-SNAPSHOT</version>
                          <packaging>hpi</packaging>
                          <name>Empty Plugin</name>
                          <developers>
                            <developer>
                              <id>bhacker</id>
                              <name>Bob Q. Hacker</name>
                              <email>bhacker@nowhere.net</email>
                            </developer>
                          </developers>
                          <scm>
                            <connection>scm:git:git://github.com/jenkinsci/empty-plugin.git</connection>
                          </scm>
                          <properties>
                            <jenkins-test-harness.version>2.41.1</jenkins-test-harness.version>
                            <jenkins.version>2.440.3</jenkins.version>
                          </properties>
                          <dependencyManagement>
                            <dependencies>
                              <dependency>
                                <groupId>io.jenkins.tools.bom</groupId>
                                <artifactId>bom-2.440.x</artifactId>
                                <version>3435.v238d66a_043fb_</version>
                                <type>pom</type>
                                <scope>import</scope>
                              </dependency>
                            </dependencies>
                          </dependencyManagement>
                            <dependencies>
                              <dependency>
                                <groupId>org.jenkins-ci.main</groupId>
                                <artifactId>jenkins-test-harness</artifactId>
                                <version>2.41.1</version>
                              </dependency>
                              <dependency>
                                <groupId>com.github.tomakehurst</groupId>
                                <artifactId>wiremock-jre8-standalone</artifactId>
                                <version>2.35.2</version>
                                <scope>test</scope>
                              </dependency>
                            </dependencies>
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
                        """,
                        """
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
                          <scm>
                            <connection>scm:git:https://github.com/jenkinsci/empty-plugin.git</connection>
                          </scm>
                          <properties>
                            <jenkins-test-harness.version>2225.v04fa_3929c9b_5</jenkins-test-harness.version>
                            <!-- https://www.jenkins.io/doc/developer/plugin-development/choosing-jenkins-baseline/ -->
                            <jenkins.baseline>2.462</jenkins.baseline>
                            <jenkins.version>${jenkins.baseline}.3</jenkins.version>
                          </properties>
                          <dependencyManagement>
                            <dependencies>
                              <dependency>
                                <groupId>io.jenkins.tools.bom</groupId>
                                <artifactId>bom-${jenkins.baseline}.x</artifactId>
                                <version>%s</version>
                                <type>pom</type>
                                <scope>import</scope>
                              </dependency>
                            </dependencies>
                          </dependencyManagement>
                            <dependencies>
                              <dependency>
                                <groupId>org.jenkins-ci.main</groupId>
                                <artifactId>jenkins-test-harness</artifactId>
                              </dependency>
                              <dependency>
                                <groupId>org.wiremock</groupId>
                                <artifactId>wiremock-standalone</artifactId>
                                <version>%s</version>
                                <scope>test</scope>
                              </dependency>
                            </dependencies>
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
                        """
                                .formatted(Settings.getRecommendedBomVersion(), Settings.getWiremockVersion())));
    }

    @Test
    void upgradeToUpgradeToLatestJava8CoreVersion() {
        rewriteRun(
                spec -> {
                    var parser = JavaParser.fromJavaVersion().logCompilationWarningsAndErrors(true);
                    collectRewriteTestDependencies().stream()
                            .filter(entry -> entry.getFileName().toString().contains("ssh-slaves-1.12"))
                            .forEach(parser::addClasspathEntry);
                    spec.recipeFromResource(
                                    "/META-INF/rewrite/recipes.yml",
                                    "io.jenkins.tools.pluginmodernizer.UpgradeToLatestJava8CoreVersion")
                            .parser(parser);
                },
                // language=java
                srcTestJava(java(
                        ReplaceRemovedSSHLauncherConstructorTest.BEFORE,
                        ReplaceRemovedSSHLauncherConstructorTest.AFTER)),
                srcMainResources(text(
                        null,
                        EXPECTED_JELLY,
                        s -> s.path(ArchetypeCommonFile.INDEX_JELLY.getPath().getFileName()))),
                // language=groovy
                groovy(
                        null,
                        JAVA_8_JENKINS_FILE,
                        s -> s.path(ArchetypeCommonFile.JENKINSFILE.getPath().getFileName())),
                // language=xml
                pomXml(
                        """
                        <?xml version="1.0" encoding="UTF-8"?>
                        <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
                          <modelVersion>4.0.0</modelVersion>
                          <parent>
                            <groupId>org.jenkins-ci.plugins</groupId>
                            <artifactId>plugin</artifactId>
                            <version>4.40</version>
                            <relativePath />
                          </parent>
                          <groupId>io.jenkins.plugins</groupId>
                          <artifactId>empty</artifactId>
                          <version>1.0.0-SNAPSHOT</version>
                          <packaging>hpi</packaging>
                          <name>Empty Plugin</name>
                          <developers>
                            <developer>
                              <id>bhacker</id>
                              <name>Bob Q. Hacker</name>
                              <email>bhacker@nowhere.net</email>
                            </developer>
                          </developers>
                          <scm>
                            <connection>scm:git:git://github.com/jenkinsci/empty-plugin.git</connection>
                          </scm>
                          <properties>
                            <jenkins-test-harness.version>2.41.1</jenkins-test-harness.version>
                            <jenkins.version>2.303.3</jenkins.version>
                          </properties>
                          <dependencyManagement>
                            <dependencies>
                              <dependency>
                                <groupId>io.jenkins.tools.bom</groupId>
                                <artifactId>bom-2.303.x</artifactId>
                                <version>1500.ve4d05cd32975</version>
                                <type>pom</type>
                                <scope>import</scope>
                              </dependency>
                            </dependencies>
                          </dependencyManagement>
                            <dependencies>
                              <dependency>
                                <groupId>org.jenkins-ci.main</groupId>
                                <artifactId>jenkins-test-harness</artifactId>
                                <version>2.41.1</version>
                              </dependency>
                            </dependencies>
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
                        """,
                        """
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
                          <name>Empty Plugin</name>
                          <scm>
                            <connection>scm:git:https://github.com/jenkinsci/empty-plugin.git</connection>
                          </scm>
                          <properties>
                            <jenkins-test-harness.version>1900.v9e128c991ef4</jenkins-test-harness.version>
                            <!-- https://www.jenkins.io/doc/developer/plugin-development/choosing-jenkins-baseline/ -->
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
                            </dependencies>
                          </dependencyManagement>
                            <dependencies>
                            <dependency>
                              <groupId>io.jenkins.plugins</groupId>
                              <artifactId>javax-activation-api</artifactId>
                            </dependency>
                            <dependency>
                              <groupId>io.jenkins.plugins</groupId>
                              <artifactId>javax-mail-api</artifactId>
                            </dependency>
                            <dependency>
                              <groupId>io.jenkins.plugins</groupId>
                              <artifactId>jaxb</artifactId>
                            </dependency>
                              <dependency>
                                <groupId>org.jenkins-ci.main</groupId>
                                <artifactId>jenkins-test-harness</artifactId>
                              </dependency>
                            <dependency>
                              <groupId>org.jenkins-ci.main</groupId>
                              <artifactId>maven-plugin</artifactId>
                              <version>RELEASE</version>
                            </dependency>
                            <dependency>
                              <groupId>org.jenkins-ci.modules</groupId>
                              <artifactId>sshd</artifactId>
                            </dependency>
                            <dependency>
                              <groupId>org.jenkins-ci.plugins</groupId>
                              <artifactId>ant</artifactId>
                            </dependency>
                            <dependency>
                              <groupId>org.jenkins-ci.plugins</groupId>
                              <artifactId>antisamy-markup-formatter</artifactId>
                            </dependency>
                            <dependency>
                              <groupId>org.jenkins-ci.plugins</groupId>
                              <artifactId>bouncycastle-api</artifactId>
                            </dependency>
                            <dependency>
                              <groupId>org.jenkins-ci.plugins</groupId>
                              <artifactId>command-launcher</artifactId>
                            </dependency>
                            <dependency>
                              <groupId>org.jenkins-ci.plugins</groupId>
                              <artifactId>external-monitor-job</artifactId>
                              <version>RELEASE</version>
                            </dependency>
                            <dependency>
                              <groupId>org.jenkins-ci.plugins</groupId>
                              <artifactId>javadoc</artifactId>
                            </dependency>
                            <dependency>
                              <groupId>org.jenkins-ci.plugins</groupId>
                              <artifactId>jdk-tool</artifactId>
                            </dependency>
                            <dependency>
                              <groupId>org.jenkins-ci.plugins</groupId>
                              <artifactId>junit</artifactId>
                            </dependency>
                            <dependency>
                              <groupId>org.jenkins-ci.plugins</groupId>
                              <artifactId>ldap</artifactId>
                            </dependency>
                            <dependency>
                              <groupId>org.jenkins-ci.plugins</groupId>
                              <artifactId>mailer</artifactId>
                            </dependency>
                            <dependency>
                              <groupId>org.jenkins-ci.plugins</groupId>
                              <artifactId>matrix-auth</artifactId>
                            </dependency>
                            <dependency>
                              <groupId>org.jenkins-ci.plugins</groupId>
                              <artifactId>matrix-project</artifactId>
                            </dependency>
                            <dependency>
                              <groupId>org.jenkins-ci.plugins</groupId>
                              <artifactId>pam-auth</artifactId>
                            </dependency>
                            <dependency>
                              <groupId>org.jenkins-ci.plugins</groupId>
                              <artifactId>subversion</artifactId>
                            </dependency>
                            <dependency>
                              <groupId>org.jenkins-ci.plugins</groupId>
                              <artifactId>trilead-api</artifactId>
                            </dependency>
                            </dependencies>
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
                        """),
                srcTestJava(
                        java(
                                // language=java
                                """
                        package hudson.maven;
                        public class MavenModuleSet {}
                        """),
                        java(
                                // language=java
                                """
                        package hudson.scm;
                        public class SubversionSCM {}
                        """),
                        java(
                                // language=java
                                """
                        package hudson.tasks;
                        public class Ant {}
                        """),
                        java(
                                // language=java
                                """
                        package hudson.tasks;
                        public class JavadocArchiver {}
                        """),
                        java(
                                // language=java
                                """
                        package hudson.tasks;
                        public class Mailer {}
                        """),
                        java(
                                // language=java
                                """
                        package hudson.tasks.junit;
                        public class JUnitResultArchiver {}
                        """),
                        java(
                                // language=java
                                """
                        package hudson.model;
                        public class ExternalJob {}
                        """),
                        java(
                                // language=java
                                """
                        package hudson.security;
                        public class LDAPSecurityRealm {}
                        """),
                        java(
                                // language=java
                                """
                        package hudson.security;
                        public class PAMSecurityRealm {}
                        """),
                        java(
                                // language=java
                                """
                        package hudson.security;
                        public class GlobalMatrixAuthorizationStrategy {}
                        """),
                        java(
                                // language=java
                                """
                        package hudson.security;
                        public class ProjectMatrixAuthorizationStrategy {}
                        """),
                        java(
                                // language=java
                                """
                        package hudson.security;
                        public class AuthorizationMatrixProperty {}
                        """),
                        java(
                                // language=java
                                """
                        package hudson.slaves;
                        public class CommandLauncher {}
                        """),
                        java(
                                // language=java
                                """
                        package hudson.tools;
                        public class JDKInstaller {}
                        """),
                        java(
                                // language=java
                                """
                        package javax.xml.bind;
                        public class JAXBContext {}
                        """),
                        java(
                                // language=java
                                """
                        package com.trilead.ssh2;
                        public class Connection {}
                        """),
                        java(
                                // language=java
                                """
                        package org.jenkinsci.main.modules.sshd;
                        public class SSHD {}
                        """),
                        java(
                                // language=java
                                """
                        package javax.activation;
                        public class DataHandler {}
                        """),
                        java(
                                // language=java
                                """
                        package jenkins.bouncycastle.api;
                        public class BouncyCastlePlugin {}
                        """),
                        java(
                                // language=java
                                """
                        package jenkins.plugins.javax.activation;
                        public class CommandMapInitializer {}
                        """),
                        java(
                                // language=java
                                """
                        package jenkins.plugins.javax.activation;
                        public class FileTypeMapInitializer {}
                        """),
                        java(
                                // language=java
                                """
                        package org.jenkinsci.main.modules.instance_identity;
                        public class InstanceIdentity {}
                        """),
                        java(
                                // language=java
                                """
                        package hudson.markup;
                        public class RawHtmlMarkupFormatter {}
                        """),
                        java(
                                // language=java
                                """
                        package hudson.matrix;
                        public class MatrixProject {}
                        """)),
                srcMainJava(
                        java(
                                // language=java
                                """
                        import hudson.maven.MavenModuleSet;
                        import hudson.scm.SubversionSCM;
                        import hudson.tasks.Ant;
                        import hudson.tasks.JavadocArchiver;
                        import hudson.tasks.Mailer;
                        import hudson.tasks.junit.JUnitResultArchiver;
                        import hudson.model.ExternalJob;
                        import hudson.security.LDAPSecurityRealm;
                        import hudson.security.PAMSecurityRealm;
                        import hudson.security.GlobalMatrixAuthorizationStrategy;
                        import hudson.security.ProjectMatrixAuthorizationStrategy;
                        import hudson.security.AuthorizationMatrixProperty;
                        import hudson.slaves.CommandLauncher;
                        import hudson.tools.JDKInstaller;
                        import javax.xml.bind.JAXBContext;
                        import com.trilead.ssh2.Connection;
                        import org.jenkinsci.main.modules.sshd.SSHD;
                        import javax.activation.DataHandler;
                        import jenkins.bouncycastle.api.BouncyCastlePlugin;
                        import jenkins.plugins.javax.activation.CommandMapInitializer;
                        import jenkins.plugins.javax.activation.FileTypeMapInitializer;
                        import org.jenkinsci.main.modules.instance_identity.InstanceIdentity;
                        import hudson.markup.RawHtmlMarkupFormatter;
                        import hudson.matrix.MatrixProject;

                        public class TestDetachedPluginsUsage {
                            public void execute() {
                                new MavenModuleSet();
                                new SubversionSCM();
                                new Ant();
                                new JavadocArchiver();
                                new Mailer();
                                new JUnitResultArchiver();
                                new ExternalJob();
                                new LDAPSecurityRealm();
                                new PAMSecurityRealm();
                                new GlobalMatrixAuthorizationStrategy();
                                new ProjectMatrixAuthorizationStrategy();
                                new AuthorizationMatrixProperty();
                                new CommandLauncher();
                                new JDKInstaller();
                                new JAXBContext();
                                new Connection();
                                new SSHD();
                                new DataHandler();
                                new BouncyCastlePlugin();
                                new CommandMapInitializer();
                                new FileTypeMapInitializer();
                                new InstanceIdentity();
                                new RawHtmlMarkupFormatter();
                                new MatrixProject();
                            }
                        }
                        """)));
    }

    @Test
    void upgradeNextMajorParentVersionTest() {
        rewriteRun(
                spec -> {
                    var parser = JavaParser.fromJavaVersion().logCompilationWarningsAndErrors(true);
                    collectRewriteTestDependencies().forEach(parser::addClasspathEntry);
                    spec.recipeFromResource(
                                    "/META-INF/rewrite/recipes.yml",
                                    "io.jenkins.tools.pluginmodernizer.UpgradeNextMajorParentVersion")
                            .parser(parser);
                },
                mavenProject("test"),
                // language=xml
                srcMainResources(text(
                        null,
                        EXPECTED_JELLY,
                        s -> s.path(ArchetypeCommonFile.INDEX_JELLY.getPath().getFileName()))),
                // language=groovy
                groovy(
                        null,
                        EXPECTED_MODERN_JENKINSFILE,
                        s -> s.path(ArchetypeCommonFile.JENKINSFILE.getPath().getFileName())),
                // language=xml
                pomXml(
                        """
                            <?xml version="1.0" encoding="UTF-8"?>
                            <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
                              <modelVersion>4.0.0</modelVersion>
                              <parent>
                                <groupId>org.jenkins-ci.plugins</groupId>
                                <artifactId>plugin</artifactId>
                                <version>4.87</version>
                              </parent>
                              <groupId>io.jenkins.plugins</groupId>
                              <artifactId>empty</artifactId>
                              <version>1.0.0-SNAPSHOT</version>
                              <packaging>hpi</packaging>
                              <name>Empty Plugin</name>
                              <developers>
                                <developer>
                                  <id>bhacker</id>
                                  <name>Bob Q. Hacker</name>
                                  <email>bhacker@nowhere.net</email>
                                </developer>
                              </developers>
                              <scm>
                                <connection>scm:git:https://github.com/jenkinsci/empty-plugin.git</connection>
                              </scm>
                              <properties>
                                <jenkins-test-harness.version>2.41.1</jenkins-test-harness.version>
                                <maven.compiler.release>11</maven.compiler.release>
                                <jenkins.version>2.440.3</jenkins.version>
                                <maven.compiler.source>11</maven.compiler.source>
                                <maven.compiler.target>11</maven.compiler.target>
                                <maven.compiler.release>11</maven.compiler.release>
                              </properties>
                              <dependencies>
                                <dependency>
                                  <groupId>com.github.tomakehurst</groupId>
                                  <artifactId>wiremock-jre8-standalone</artifactId>
                                  <version>2.35.2</version>
                                  <scope>test</scope>
                                </dependency>
                              </dependencies>
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
                            """,
                        """
                            <?xml version="1.0" encoding="UTF-8"?>
                            <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
                              <modelVersion>4.0.0</modelVersion>
                              <parent>
                                <groupId>org.jenkins-ci.plugins</groupId>
                                <artifactId>plugin</artifactId>
                                <version>%s</version>
                                <relativePath />
                              </parent>
                              <groupId>io.jenkins.plugins</groupId>
                              <artifactId>empty</artifactId>
                              <version>1.0.0-SNAPSHOT</version>
                              <packaging>hpi</packaging>
                              <name>Empty Plugin</name>
                              <scm>
                                <connection>scm:git:https://github.com/jenkinsci/empty-plugin.git</connection>
                              </scm>
                              <properties>
                                <jenkins-test-harness.version>%s</jenkins-test-harness.version>
                                <jenkins.version>2.492.3</jenkins.version>
                              </properties>
                              <dependencies>
                                <dependency>
                                  <groupId>org.wiremock</groupId>
                                  <artifactId>wiremock-standalone</artifactId>
                                  <version>%s</version>
                                  <scope>test</scope>
                                </dependency>
                              </dependencies>
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
                            """
                                .formatted(
                                        Settings.getJenkinsParentVersion(),
                                        Settings.getJenkinsTestHarnessVersion(),
                                        Settings.getWiremockVersion())),
                srcMainResources(
                        // language=java
                        java(
                                """
                                import javax.servlet.ServletException;
                                import org.kohsuke.stapler.Stapler;
                                import org.kohsuke.stapler.StaplerRequest;
                                import org.kohsuke.stapler.StaplerResponse;
                                import org.acegisecurity.Authentication;
                                import org.acegisecurity.GrantedAuthority;
                                import org.acegisecurity.GrantedAuthorityImpl;
                                import org.acegisecurity.providers.AbstractAuthenticationToken;
                                import org.acegisecurity.context.SecurityContextHolder;
                                import org.acegisecurity.AuthenticationException;
                                import org.acegisecurity.AuthenticationManager;
                                import org.acegisecurity.BadCredentialsException;
                                import org.acegisecurity.userdetails.UserDetails;
                                import org.acegisecurity.userdetails.UserDetailsService;
                                import org.acegisecurity.userdetails.UsernameNotFoundException;
                                import jenkins.model.Jenkins;
                                import jenkins.security.SecurityListener;
                                import hudson.security.SecurityRealm;

                                public class Foo extends SecurityRealm {
                                    @Override
                                    public UserDetails loadUserByUsername(String username) {
                                       return null;
                                    }
                                    @Override
                                    public SecurityComponents createSecurityComponents() {
                                       new UserDetailsService() {
                                          public UserDetails loadUserByUsername(String username) {
                                             return null;
                                          }
                                       };
                                    }
                                    public void foo() {
                                        StaplerRequest req = Stapler.getCurrentRequest();
                                        StaplerResponse response = Stapler.getCurrentResponse();
                                        Authentication auth = Jenkins.getAuthentication();
                                    }
                                }
                                """,
                                """
                                import jakarta.servlet.ServletException;
                                import org.kohsuke.stapler.Stapler;
                                import org.kohsuke.stapler.StaplerRequest2;
                                import org.kohsuke.stapler.StaplerResponse2;
                                import org.springframework.security.core.Authentication;
                                import org.springframework.security.core.GrantedAuthority;
                                import org.springframework.security.core.context.SecurityContextHolder;
                                import org.springframework.security.core.AuthenticationException;
                                import org.springframework.security.core.userdetails.UserDetails;
                                import org.springframework.security.core.userdetails.UserDetailsService;
                                import org.springframework.security.core.userdetails.UsernameNotFoundException;
                                import jenkins.model.Jenkins;
                                import jenkins.security.SecurityListener;
                                import hudson.security.SecurityRealm;
                                import org.springframework.security.core.authority.SimpleGrantedAuthority;
                                import org.springframework.security.authentication.AbstractAuthenticationToken;
                                import java.util.List;
                                import java.util.Collection;
                                import org.springframework.security.authentication.AuthenticationManager;
                                import org.springframework.security.authentication.BadCredentialsException;

                                public class Foo extends SecurityRealm {
                                    @Override
                                    public UserDetails loadUserByUsername2(String username) {
                                       return null;
                                    }
                                    @Override
                                    public SecurityComponents createSecurityComponents() {
                                       new UserDetailsService() {
                                          public UserDetails loadUserByUsername(String username) {
                                             return null;
                                          }
                                       };
                                    }
                                    public void foo() {
                                        StaplerRequest2 req = Stapler.getCurrentRequest2();
                                        StaplerResponse2 response = Stapler.getCurrentResponse2();
                                        Authentication auth = Jenkins.getAuthentication2();
                                    }
                                }
                                """)));
    }

    @Test
    void upgradeNextMajorParentVersionTestWithBom() {
        rewriteRun(
                spec -> {
                    var parser = JavaParser.fromJavaVersion().logCompilationWarningsAndErrors(true);
                    collectRewriteTestDependencies().forEach(parser::addClasspathEntry);
                    spec.recipeFromResource(
                                    "/META-INF/rewrite/recipes.yml",
                                    "io.jenkins.tools.pluginmodernizer.UpgradeNextMajorParentVersion")
                            .parser(parser);
                }, // language=xml
                srcMainResources(text(
                        null,
                        EXPECTED_JELLY,
                        s -> s.path(ArchetypeCommonFile.INDEX_JELLY.getPath().getFileName()))),
                // language=groovy
                groovy(
                        null,
                        EXPECTED_MODERN_JENKINSFILE,
                        s -> s.path(ArchetypeCommonFile.JENKINSFILE.getPath().getFileName())),
                // language=xml
                pomXml(
                        """
                        <?xml version="1.0" encoding="UTF-8"?>
                        <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
                          <modelVersion>4.0.0</modelVersion>
                          <parent>
                            <groupId>org.jenkins-ci.plugins</groupId>
                            <artifactId>plugin</artifactId>
                            <version>4.87</version>
                            <relativePath />
                          </parent>
                          <groupId>io.jenkins.plugins</groupId>
                          <artifactId>empty</artifactId>
                          <version>1.0.0-SNAPSHOT</version>
                          <packaging>hpi</packaging>
                          <name>Empty Plugin</name>
                          <developers>
                            <developer>
                              <id>bhacker</id>
                              <name>Bob Q. Hacker</name>
                              <email>bhacker@nowhere.net</email>
                            </developer>
                          </developers>
                          <properties>
                            <jenkins-test-harness.version>2.41.1</jenkins-test-harness.version>
                            <java.version>17</java.version>
                            <jenkins.version>2.440.3</jenkins.version>
                            <maven.compiler.source>17</maven.compiler.source>
                            <maven.compiler.release>17</maven.compiler.release>
                            <maven.compiler.target>17</maven.compiler.target>
                          </properties>
                          <dependencyManagement>
                            <dependencies>
                              <dependency>
                                <groupId>io.jenkins.tools.bom</groupId>
                                <artifactId>bom-2.440.x</artifactId>
                                <version>3435.v238d66a_043fb_</version>
                                <type>pom</type>
                                <scope>import</scope>
                              </dependency>
                            </dependencies>
                          </dependencyManagement>
                            <dependencies>
                              <dependency>
                                <groupId>org.jenkins-ci.main</groupId>
                                <artifactId>jenkins-test-harness</artifactId>
                                <version>2.41.1</version>
                              </dependency>
                              <dependency>
                                <groupId>com.github.tomakehurst</groupId>
                                <artifactId>wiremock</artifactId>
                                <version>3.0.1</version>
                                <scope>test</scope>
                              </dependency>
                            </dependencies>
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
                        """,
                        """
                        <?xml version="1.0" encoding="UTF-8"?>
                        <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
                          <modelVersion>4.0.0</modelVersion>
                          <parent>
                            <groupId>org.jenkins-ci.plugins</groupId>
                            <artifactId>plugin</artifactId>
                            <version>%s</version>
                            <relativePath />
                          </parent>
                          <groupId>io.jenkins.plugins</groupId>
                          <artifactId>empty</artifactId>
                          <version>1.0.0-SNAPSHOT</version>
                          <packaging>hpi</packaging>
                          <name>Empty Plugin</name>
                          <properties>
                            <jenkins-test-harness.version>%s</jenkins-test-harness.version>
                            <!-- https://www.jenkins.io/doc/developer/plugin-development/choosing-jenkins-baseline/ -->
                            <jenkins.baseline>2.492</jenkins.baseline>
                            <jenkins.version>${jenkins.baseline}.3</jenkins.version>
                          </properties>
                          <dependencyManagement>
                            <dependencies>
                              <dependency>
                                <groupId>io.jenkins.tools.bom</groupId>
                                <artifactId>bom-${jenkins.baseline}.x</artifactId>
                                <version>%s</version>
                                <type>pom</type>
                                <scope>import</scope>
                              </dependency>
                            </dependencies>
                          </dependencyManagement>
                            <dependencies>
                              <dependency>
                                <groupId>org.jenkins-ci.main</groupId>
                                <artifactId>jenkins-test-harness</artifactId>
                              </dependency>
                              <dependency>
                                <groupId>org.wiremock</groupId>
                                <artifactId>wiremock</artifactId>
                                <version>%s</version>
                                <scope>test</scope>
                              </dependency>
                            </dependencies>
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
                        """
                                .formatted(
                                        Settings.getJenkinsParentVersion(),
                                        Settings.getJenkinsTestHarnessVersion(),
                                        Settings.getBomVersion(),
                                        Settings.getWiremockVersion())),
                srcTestJava(
                        java(
                                // language=java
                                """
                        package hudson.util;
                        public class ChartUtil {}
                        """)),
                srcMainResources(
                        // language=java
                        java(
                                """
                                import javax.servlet.ServletException;
                                import com.gargoylesoftware.htmlunit.HttpMethod;
                                import com.gargoylesoftware.htmlunit.WebRequest;
                                import com.gargoylesoftware.htmlunit.html.HtmlPage;
                                import org.kohsuke.stapler.Stapler;
                                import org.kohsuke.stapler.StaplerRequest;
                                import org.kohsuke.stapler.StaplerResponse;
                                import hudson.util.ChartUtil;

                                public class Foo {
                                    public void foo() {
                                        StaplerRequest req = Stapler.getCurrentRequest();
                                        StaplerResponse response = Stapler.getCurrentResponse();
                                    }
                                }
                                """,
                                """
                                import javax.servlet.ServletException;
                                import org.htmlunit.HttpMethod;
                                import org.htmlunit.WebRequest;
                                import org.htmlunit.html.HtmlPage;
                                import org.kohsuke.stapler.Stapler;
                                import org.kohsuke.stapler.StaplerRequest;
                                import org.kohsuke.stapler.StaplerResponse;
                                import hudson.util.ChartUtil;

                                public class Foo {
                                    public void foo() {
                                        StaplerRequest req = Stapler.getCurrentRequest();
                                        StaplerResponse response = Stapler.getCurrentResponse();
                                    }
                                }""")));
    }

    @Test
    void upgradeNextMajorParentVersionTestWithBaseline() {
        rewriteRun(
                spec -> spec.recipeFromResource(
                        "/META-INF/rewrite/recipes.yml",
                        "io.jenkins.tools.pluginmodernizer.UpgradeNextMajorParentVersion"),
                // language=xml
                srcMainResources(text(
                        null,
                        EXPECTED_JELLY,
                        s -> s.path(ArchetypeCommonFile.INDEX_JELLY.getPath().getFileName()))),
                // language=groovy
                groovy(
                        null,
                        EXPECTED_MODERN_JENKINSFILE,
                        s -> s.path(ArchetypeCommonFile.JENKINSFILE.getPath().getFileName())),
                // language=xml
                pomXml(
                        """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
                  <modelVersion>4.0.0</modelVersion>
                  <parent>
                    <groupId>org.jenkins-ci.plugins</groupId>
                    <artifactId>plugin</artifactId>
                    <version>4.87</version>
                    <relativePath />
                  </parent>
                  <groupId>io.jenkins.plugins</groupId>
                  <artifactId>empty</artifactId>
                  <version>1.0.0-SNAPSHOT</version>
                  <packaging>hpi</packaging>
                  <name>Empty Plugin</name>
                  <developers>
                    <developer>
                      <id>bhacker</id>
                      <name>Bob Q. Hacker</name>
                      <email>bhacker@nowhere.net</email>
                    </developer>
                  </developers>
                  <properties>
                    <jenkins-test-harness.version>2.41.1</jenkins-test-harness.version>
                    <jenkins.baseline>2.440</jenkins.baseline>
                    <jenkins.version>${jenkins.baseline}.3</jenkins.version>
                  </properties>
                  <dependencyManagement>
                    <dependencies>
                      <dependency>
                        <groupId>io.jenkins.tools.bom</groupId>
                        <artifactId>bom-${jenkins.baseline}.x</artifactId>
                        <version>3435.v238d66a_043fb_</version>
                        <type>pom</type>
                        <scope>import</scope>
                      </dependency>
                    </dependencies>
                  </dependencyManagement>
                    <dependencies>
                      <dependency>
                        <groupId>org.jenkins-ci.main</groupId>
                        <artifactId>jenkins-test-harness</artifactId>
                        <version>2.41.1</version>
                      </dependency>
                    </dependencies>
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
                """,
                        """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
                  <modelVersion>4.0.0</modelVersion>
                  <parent>
                    <groupId>org.jenkins-ci.plugins</groupId>
                    <artifactId>plugin</artifactId>
                    <version>%s</version>
                    <relativePath />
                  </parent>
                  <groupId>io.jenkins.plugins</groupId>
                  <artifactId>empty</artifactId>
                  <version>1.0.0-SNAPSHOT</version>
                  <packaging>hpi</packaging>
                  <name>Empty Plugin</name>
                  <properties>
                    <jenkins-test-harness.version>%s</jenkins-test-harness.version>
                    <!-- https://www.jenkins.io/doc/developer/plugin-development/choosing-jenkins-baseline/ -->
                    <jenkins.baseline>2.492</jenkins.baseline>
                    <jenkins.version>${jenkins.baseline}.3</jenkins.version>
                  </properties>
                  <dependencyManagement>
                    <dependencies>
                      <dependency>
                        <groupId>io.jenkins.tools.bom</groupId>
                        <artifactId>bom-${jenkins.baseline}.x</artifactId>
                        <version>%s</version>
                        <type>pom</type>
                        <scope>import</scope>
                      </dependency>
                    </dependencies>
                  </dependencyManagement>
                    <dependencies>
                      <dependency>
                        <groupId>org.jenkins-ci.main</groupId>
                        <artifactId>jenkins-test-harness</artifactId>
                      </dependency>
                    </dependencies>
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
                """
                                .formatted(
                                        Settings.getJenkinsParentVersion(),
                                        Settings.getJenkinsTestHarnessVersion(),
                                        Settings.getBomVersion())));
    }

    @Test
    void addPluginBomTest() {
        rewriteRun(
                spec -> spec.recipeFromResource(
                        "/META-INF/rewrite/recipes.yml", "io.jenkins.tools.pluginmodernizer.AddPluginsBom"),
                // language=xml
                pomXml(
                        """
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
                    <jenkins.version>2.440.3</jenkins.version>
                    <configuration-as-code.version>1909.vb_b_f59a_27d013</configuration-as-code.version>
                    <java.version>11</java.version>
                  </properties>
                  <dependencies>
                    <dependency>
                      <groupId>io.jenkins.plugins</groupId>
                      <artifactId>gson-api</artifactId>
                      <version>2.11.0-41.v019fcf6125dc</version>
                    </dependency>
                  </dependencies>
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
                """,
                        """
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
                    <!-- https://www.jenkins.io/doc/developer/plugin-development/choosing-jenkins-baseline/ -->
                    <jenkins.baseline>2.440</jenkins.baseline>
                    <jenkins.version>${jenkins.baseline}.3</jenkins.version>
                  </properties>
                  <dependencyManagement>
                    <dependencies>
                      <dependency>
                        <groupId>io.jenkins.tools.bom</groupId>
                        <artifactId>bom-${jenkins.baseline}.x</artifactId>
                        <version>3435.v238d66a_043fb_</version>
                        <type>pom</type>
                        <scope>import</scope>
                      </dependency>
                    </dependencies>
                  </dependencyManagement>
                  <dependencies>
                    <dependency>
                      <groupId>io.jenkins.plugins</groupId>
                      <artifactId>gson-api</artifactId>
                    </dependency>
                  </dependencies>
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
    void addPluginBomTestAndRemoveProperties() {
        rewriteRun(
                spec -> spec.recipeFromResource(
                        "/META-INF/rewrite/recipes.yml", "io.jenkins.tools.pluginmodernizer.AddPluginsBom"),
                // language=xml
                pomXml(
                        """
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
                    <jenkins.version>2.440.3</jenkins.version>
                    <configuration-as-code.version>1909.vb_b_f59a_27d013</configuration-as-code.version>
                    <java.version>11</java.version>
                  </properties>
                  <dependencies>
                    <dependency>
                      <groupId>io.jenkins.plugins</groupId>
                      <artifactId>gson-api</artifactId>
                      <version>2.11.0-41.v019fcf6125dc</version>
                    </dependency>
                    <dependency>
                      <groupId>io.jenkins</groupId>
                      <artifactId>configuration-as-code</artifactId>
                      <version>${configuration-as-code.version}</version>
                    </dependency>
                  </dependencies>
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
                """,
                        """
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
                    <!-- https://www.jenkins.io/doc/developer/plugin-development/choosing-jenkins-baseline/ -->
                    <jenkins.baseline>2.440</jenkins.baseline>
                    <jenkins.version>${jenkins.baseline}.3</jenkins.version>
                    <configuration-as-code.version>1909.vb_b_f59a_27d013</configuration-as-code.version>
                  </properties>
                  <dependencyManagement>
                    <dependencies>
                      <dependency>
                        <groupId>io.jenkins.tools.bom</groupId>
                        <artifactId>bom-${jenkins.baseline}.x</artifactId>
                        <version>3435.v238d66a_043fb_</version>
                        <type>pom</type>
                        <scope>import</scope>
                      </dependency>
                    </dependencies>
                  </dependencyManagement>
                  <dependencies>
                    <dependency>
                      <groupId>io.jenkins.plugins</groupId>
                      <artifactId>gson-api</artifactId>
                    </dependency>
                    <dependency>
                      <groupId>io.jenkins</groupId>
                      <artifactId>configuration-as-code</artifactId>
                      <version>${configuration-as-code.version}</version>
                    </dependency>
                  </dependencies>
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
    void replaceLibrariesByApiPluginsSimple() throws IOException {

        // Read API plugin version
        String jsonApiVersion = getApiPluginVersion("json-api");
        String jsonPathApiVersion = getApiPluginVersion("json-path-api");
        String gsonApiVersion = getApiPluginVersion("gson-api");
        String jodaTimeApiVersion = getApiPluginVersion("joda-time-api");
        String jsoupVersion = getApiPluginVersion("jsoup");
        String commonsLang3ApiVersion = getApiPluginVersion("commons-lang3-api");
        String byteBuddyApiVersion = getApiPluginVersion("byte-buddy-api");
        String commonTextApiVersion = getApiPluginVersion("commons-text-api");

        rewriteRun(
                spec -> spec.recipeFromResource(
                        "/META-INF/rewrite/recipes.yml",
                        "io.jenkins.tools.pluginmodernizer.ReplaceLibrariesWithApiPlugin"),
                // language=xml
                pomXml(
                        """
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
                    <jenkins.version>2.462.3</jenkins.version>
                  </properties>
                  <dependencies>
                    <dependency>
                      <groupId>com.google.code.gson</groupId>
                      <artifactId>gson</artifactId>
                      <version>2.10.1</version>
                    </dependency>
                    <dependency>
                      <groupId>joda-time</groupId>
                      <artifactId>joda-time</artifactId>
                      <version>2.13.0</version>
                    </dependency>
                    <dependency>
                      <groupId>net.bytebuddy</groupId>
                      <artifactId>byte-buddy</artifactId>
                      <version>1.15.11</version>
                    </dependency>
                    <dependency>
                      <groupId>org.apache.commons</groupId>
                      <artifactId>commons-compress</artifactId>
                      <version>1.26.1</version>
                    </dependency>
                    <dependency>
                      <groupId>org.jsoup</groupId>
                      <artifactId>jsoup</artifactId>
                      <version>1.18.3</version>
                    </dependency>
                    <dependency>
                      <groupId>org.apache.commons</groupId>
                      <artifactId>commons-lang3</artifactId>
                      <version>3.17.0</version>
                    </dependency>
                    <dependency>
                      <groupId>org.apache.commons</groupId>
                      <artifactId>commons-text</artifactId>
                      <version>1.13.0</version>
                    </dependency>
                    <dependency>
                      <groupId>org.json</groupId>
                      <artifactId>json</artifactId>
                      <version>20240303</version>
                    </dependency>
                    <dependency>
                      <groupId>com.jayway.jsonpath</groupId>
                      <artifactId>json-path</artifactId>
                      <version>2.9.0</version>
                    </dependency>
                    <dependency>
                      <groupId>org.ow2.asm</groupId>
                      <artifactId>asm</artifactId>
                      <version>9.7.1</version>
                    </dependency>
                  </dependencies>
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
                """,
                        """
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
                    <jenkins.version>2.462.3</jenkins.version>
                  </properties>
                  <dependencies>
                    <dependency>
                      <groupId>io.jenkins.plugins</groupId>
                      <artifactId>byte-buddy-api</artifactId>
                      <version>%s</version>
                    </dependency>
                    <dependency>
                      <groupId>io.jenkins.plugins</groupId>
                      <artifactId>commons-lang3-api</artifactId>
                      <version>%s</version>
                    </dependency>
                    <dependency>
                      <groupId>io.jenkins.plugins</groupId>
                      <artifactId>commons-text-api</artifactId>
                      <version>%s</version>
                    </dependency>
                    <dependency>
                      <groupId>io.jenkins.plugins</groupId>
                      <artifactId>gson-api</artifactId>
                      <version>%s</version>
                    </dependency>
                    <dependency>
                      <groupId>io.jenkins.plugins</groupId>
                      <artifactId>joda-time-api</artifactId>
                      <version>%s</version>
                    </dependency>
                    <dependency>
                      <groupId>org.apache.commons</groupId>
                      <artifactId>commons-compress</artifactId>
                      <version>1.26.1</version>
                    </dependency>
                    <dependency>
                      <groupId>org.jenkins-ci.plugins</groupId>
                      <artifactId>jsoup</artifactId>
                      <version>%s</version>
                    </dependency>
                    <dependency>
                      <groupId>io.jenkins.plugins</groupId>
                      <artifactId>json-api</artifactId>
                      <version>%s</version>
                    </dependency>
                    <dependency>
                      <groupId>io.jenkins.plugins</groupId>
                      <artifactId>json-path-api</artifactId>
                      <version>%s</version>
                    </dependency>
                    <dependency>
                      <groupId>org.ow2.asm</groupId>
                      <artifactId>asm</artifactId>
                      <version>9.7.1</version>
                    </dependency>
                  </dependencies>
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
                """
                                .formatted(
                                        byteBuddyApiVersion,
                                        commonsLang3ApiVersion,
                                        commonTextApiVersion,
                                        gsonApiVersion,
                                        jodaTimeApiVersion,
                                        jsoupVersion,
                                        jsonApiVersion,
                                        jsonPathApiVersion)));
    }

    @Test
    void replaceLibrariesByApiPluginWithBom() {
        rewriteRun(
                spec -> spec.recipeFromResource(
                        "/META-INF/rewrite/recipes.yml",
                        "io.jenkins.tools.pluginmodernizer.ReplaceLibrariesWithApiPlugin"),
                // language=xml
                pomXml(
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
                  <artifactId>antexec</artifactId>
                  <version>${changelist}</version>
                  <packaging>hpi</packaging>
                  <properties>
                    <!-- https://www.jenkins.io/doc/developer/plugin-development/choosing-jenkins-baseline/ -->
                    <jenkins.baseline>2.479</jenkins.baseline>
                    <jenkins.version>${jenkins.baseline}.1</jenkins.version>
                  </properties>
                  <dependencyManagement>
                    <dependencies>
                      <dependency>
                        <groupId>io.jenkins.tools.bom</groupId>
                        <artifactId>bom-${jenkins.baseline}.x</artifactId>
                        <version>4051.v78dce3ce8b_d6</version>
                        <scope>import</scope>
                        <type>pom</type>
                      </dependency>
                      <dependency>
                        <groupId>org.jenkins-ci.tools</groupId>
                        <artifactId>maven-hpi-plugin</artifactId>
                        <version>3.61</version>
                      </dependency>
                    </dependencies>
                  </dependencyManagement>
                  <dependencies>
                    <dependency>
                      <groupId>org.jenkins-ci.plugins</groupId>
                      <artifactId>ant</artifactId>
                    </dependency>
                    <dependency>
                      <groupId>org.jenkins-ci.plugins</groupId>
                      <artifactId>token-macro</artifactId>
                    </dependency>
                  </dependencies>
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
                """,
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
                  <artifactId>antexec</artifactId>
                  <version>${changelist}</version>
                  <packaging>hpi</packaging>
                  <properties>
                    <!-- https://www.jenkins.io/doc/developer/plugin-development/choosing-jenkins-baseline/ -->
                    <jenkins.baseline>2.479</jenkins.baseline>
                    <jenkins.version>${jenkins.baseline}.1</jenkins.version>
                  </properties>
                  <dependencyManagement>
                    <dependencies>
                      <dependency>
                        <groupId>io.jenkins.tools.bom</groupId>
                        <artifactId>bom-${jenkins.baseline}.x</artifactId>
                        <version>4051.v78dce3ce8b_d6</version>
                        <scope>import</scope>
                        <type>pom</type>
                      </dependency>
                      <dependency>
                        <groupId>org.jenkins-ci.tools</groupId>
                        <artifactId>maven-hpi-plugin</artifactId>
                        <version>3.61</version>
                      </dependency>
                    </dependencies>
                  </dependencyManagement>
                  <dependencies>
                    <dependency>
                      <groupId>io.jenkins.plugins</groupId>
                      <artifactId>asm-api</artifactId>
                    </dependency>
                    <dependency>
                      <groupId>io.jenkins.plugins</groupId>
                      <artifactId>json-path-api</artifactId>
                    </dependency>
                    <dependency>
                      <groupId>org.jenkins-ci.plugins</groupId>
                      <artifactId>ant</artifactId>
                    </dependency>
                    <dependency>
                      <groupId>org.jenkins-ci.plugins</groupId>
                      <artifactId>token-macro</artifactId>
                    </dependency>
                  </dependencies>
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
    void replaceLibrariesByApiPluginsAsm() {
        rewriteRun(
                spec -> spec.recipeFromResource(
                        "/META-INF/rewrite/recipes.yml",
                        "io.jenkins.tools.pluginmodernizer.ReplaceLibrariesWithApiPlugin"),
                // language=xml
                pomXml(
                        """
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
                    <jenkins.version>2.479.1</jenkins.version>
                  </properties>
                  <dependencies>
                    <dependency>
                      <groupId>org.ow2.asm</groupId>
                      <artifactId>asm</artifactId>
                      <version>9.7.1</version>
                    </dependency>
                  </dependencies>
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
                """,
                        """
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
                    <jenkins.version>2.479.1</jenkins.version>
                  </properties>
                  <dependencies>
                    <dependency>
                      <groupId>io.jenkins.plugins</groupId>
                      <artifactId>asm-api</artifactId>
                      <version>%s</version>
                    </dependency>
                  </dependencies>
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
                """
                                .formatted(Settings.getPluginVersion("asm-api"))));
    }

    @Test
    void replaceLibrariesByApiPluginsCompress() {
        rewriteRun(
                spec -> spec.recipeFromResource(
                        "/META-INF/rewrite/recipes.yml", "io.jenkins.tools.pluginmodernizer.UseCompressApiPlugin"),
                // language=xml
                pomXml(
                        """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
                  <modelVersion>4.0.0</modelVersion>
                  <parent>
                    <groupId>org.jenkins-ci.plugins</groupId>
                    <artifactId>plugin</artifactId>
                    <version>5.13</version>
                    <relativePath />
                  </parent>
                  <groupId>io.jenkins.plugins</groupId>
                  <artifactId>empty</artifactId>
                  <version>1.0.0-SNAPSHOT</version>
                  <packaging>hpi</packaging>
                  <name>Empty Plugin</name>
                  <properties>
                    <jenkins.version>2.492.1</jenkins.version>
                  </properties>
                  <dependencies>
                    <dependency>
                      <groupId>org.apache.commons</groupId>
                      <artifactId>commons-compress</artifactId>
                      <version>1.27.1</version>
                    </dependency>
                    <dependency>
                      <groupId>org.apache.commons</groupId>
                      <artifactId>commons-lang3</artifactId>
                      <version>3.17.0</version>
                    </dependency>
                  </dependencies>
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
                """,
                        """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
                  <modelVersion>4.0.0</modelVersion>
                  <parent>
                    <groupId>org.jenkins-ci.plugins</groupId>
                    <artifactId>plugin</artifactId>
                    <version>5.13</version>
                    <relativePath />
                  </parent>
                  <groupId>io.jenkins.plugins</groupId>
                  <artifactId>empty</artifactId>
                  <version>1.0.0-SNAPSHOT</version>
                  <packaging>hpi</packaging>
                  <name>Empty Plugin</name>
                  <properties>
                    <jenkins.version>2.492.1</jenkins.version>
                  </properties>
                  <dependencies>
                    <dependency>
                      <groupId>io.jenkins.plugins</groupId>
                      <artifactId>commons-compress-api</artifactId>
                      <version>%s</version>
                    </dependency>
                  </dependencies>
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
                """
                                .formatted(Settings.getPluginVersion("commons-compress-api"))));
    }

    @Test
    void migrateToJUnit5() {
        rewriteRun(
                spec -> {
                    var parser = JavaParser.fromJavaVersion().logCompilationWarningsAndErrors(true);
                    collectRewriteTestDependencies().forEach(parser::addClasspathEntry);
                    spec.recipeFromResource(
                                    "/META-INF/rewrite/recipes.yml",
                                    "io.jenkins.tools.pluginmodernizer.MigrateToJUnit5")
                            .parser(parser)
                            .expectedCyclesThatMakeChanges(1)
                            .cycles(1);
                },
                // language=java
                java(
                        """
                import org.junit.Before;
                import org.junit.After;
                import org.junit.Test;
                import org.junit.Rule;
                import org.jvnet.hudson.test.JenkinsRule;
                import org.junit.Ignore;
                import org.junit.Assert;
                import org.hamcrest.Matchers;
                import org.junit.rules.TemporaryFolder;
                import java.io.File;

                public class MyTest {
                    @Rule
                    public JenkinsRule j = new JenkinsRule();
                    private TemporaryFolder tempFolder;

                    @Test
                    public void useJenkinsRule(String str) {
                        j.before();
                    }

                    @Before
                    public void setUp() throws Exception {
                        tempFolder = new TemporaryFolder();
                        tempFolder.create();
                    }

                    @After
                    public void tearDown() {
                        tempFolder.delete();
                    }

                    @Test
                    public void testSomething() throws Exception {
                        File tempFile = tempFolder.newFile("test.txt");
                        Assert.assertTrue("File should exist", tempFile.exists());
                        Assert.assertEquals(0, tempFile.length());
                    }

                    @Test
                    public void testInstanceOf() {
                        Object obj = new String();
                        Assert.assertTrue(obj instanceof java.lang.String);
                    }

                    @Test(expected = IllegalArgumentException.class)
                    public void testException() {
                        throw new IllegalArgumentException("Expected");
                    }

                    @Test
                    public void testNoException() {
                        try {
                            //someMethodThatShouldNotThrow();
                        } catch (Exception e) {
                            Assert.fail("Should not throw exception");
                        }
                    }

                    @Test
                    public void testEquality() {
                        String actual = "hello";
                        String expected = "hello";
                        Assert.assertThat(actual, Matchers.equalTo(expected));
                    }

                    @Ignore
                    @Test
                    public void ignoredTest() {
                        Assert.fail("This should not run");
                    }
                }
                """,
                        """
                import org.junit.jupiter.api.*;
                import org.jvnet.hudson.test.JenkinsRule;
                import org.jvnet.hudson.test.junit.jupiter.WithJenkins;
                import org.hamcrest.Matchers;
                import java.io.File;

                import static org.hamcrest.MatcherAssert.assertThat;
                import static org.junit.jupiter.api.Assertions.*;

                @WithJenkins

                class MyTest {
                    private File tempFolder;

                    @Test
                    public void useJenkinsRule(String str, JenkinsRule j) {
                        j.before();
                    }

                    @BeforeEach
                    void setUp() throws Exception {
                        tempFolder = new File();
                    }

                    @AfterEach
                    void tearDown() {
                        tempFolder.delete();
                    }

                    @Test
                    void testSomething() throws Exception {
                        File tempFile = File.createTempFile("test.txt", null, tempFolder);
                        assertTrue(tempFile.exists(), "File should exist");
                        assertEquals(0, tempFile.length());
                    }

                    @Test
                    void testInstanceOf() {
                        Object obj = new String();
                        assertInstanceOf(java.lang.String.class, obj);
                    }

                    @Test
                    void testException() {
                        assertThrows(IllegalArgumentException.class, () -> {
                            throw new IllegalArgumentException("Expected");
                        });
                    }

                    @Test
                    void testNoException() {
                        assertDoesNotThrow(() -> {
                            //someMethodThatShouldNotThrow();
                        }, "Should not throw exception");
                    }

                    @Test
                    void testEquality() {
                        String actual = "hello";
                        String expected = "hello";
                        assertThat(actual, Matchers.equalTo(expected));
                    }

                    @Disabled
                    @Test
                    void ignoredTest() {
                        fail("This should not run");
                    }
                }
                """));
    }

    @Test
    void migrateToJenkinsBaseLinePropertyTest() {
        rewriteRun(
                spec -> spec.recipeFromResource(
                        "/META-INF/rewrite/recipes.yml",
                        "io.jenkins.tools.pluginmodernizer.MigrateToJenkinsBaseLineProperty"),
                // language=xml
                pomXml(
                        """
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
                    <jenkins.version>2.440.3</jenkins.version>
                  </properties>
                  <dependencyManagement>
                    <dependencies>
                      <dependency>
                        <groupId>io.jenkins.tools.bom</groupId>
                        <artifactId>bom-2.440.x</artifactId>
                        <version>3435.v238d66a_043fb_</version>
                        <type>pom</type>
                        <scope>import</scope>
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
                """,
                        """
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
                    <!-- https://www.jenkins.io/doc/developer/plugin-development/choosing-jenkins-baseline/ -->
                    <jenkins.baseline>2.440</jenkins.baseline>
                    <jenkins.version>${jenkins.baseline}.3</jenkins.version>
                  </properties>
                  <dependencyManagement>
                    <dependencies>
                      <dependency>
                        <groupId>io.jenkins.tools.bom</groupId>
                        <artifactId>bom-${jenkins.baseline}.x</artifactId>
                        <version>3435.v238d66a_043fb_</version>
                        <type>pom</type>
                        <scope>import</scope>
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
    void ensureEnsureRelativePath() {
        rewriteRun(
                spec -> spec.recipeFromResource(
                        "/META-INF/rewrite/recipes.yml", "io.jenkins.tools.pluginmodernizer.EnsureRelativePath"),
                // language=xml
                pomXml(
                        """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
                  <modelVersion>4.0.0</modelVersion>
                  <parent>
                    <groupId>org.jenkins-ci.plugins</groupId>
                    <artifactId>plugin</artifactId>
                    <version>4.88</version>
                  </parent>
                  <groupId>io.jenkins.plugins</groupId>
                  <artifactId>empty</artifactId>
                  <version>1.0.0-SNAPSHOT</version>
                  <packaging>hpi</packaging>
                  <name>Empty Plugin</name>
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
                """,
                        """
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
    void shouldRemoveReleaseDrafterIfContinuousDeliveryEnabled() {
        rewriteRun(
                spec -> spec.recipeFromResource(
                        "/META-INF/rewrite/recipes.yml", "io.jenkins.tools.pluginmodernizer.RemoveReleaseDrafter"),
                // language=yaml
                yaml("{}", sourceSpecs -> {
                    sourceSpecs.path(ArchetypeCommonFile.WORKFLOW_CD.getPath());
                }),
                yaml("{}", null, sourceSpecs -> {
                    sourceSpecs.path(ArchetypeCommonFile.RELEASE_DRAFTER.getPath());
                }));
    }

    /**
     * Note this test need to be adapted to fix the dependabot config
     * (For example to reduce frequency or increase frequency for API plugins)
     */
    @Test
    void shouldNotAddDependabotIfRenovateConfigured() {
        rewriteRun(
                spec -> spec.recipeFromResource(
                        "/META-INF/rewrite/recipes.yml", "io.jenkins.tools.pluginmodernizer.SetupDependabot"),
                text(""), // Need one minimum file to trigger the recipe
                text("{}", sourceSpecs -> {
                    sourceSpecs.path(ArchetypeCommonFile.RENOVATE.getPath());
                }));
    }

    /**
     * Note this test need to be adapted to fix the dependabot config
     * (For example to reduce frequency or increase frequency for API plugins)
     */
    @Test
    void shouldNotChangeDependabotIfAlreadyExists() {
        rewriteRun(
                spec -> spec.recipeFromResource(
                        "/META-INF/rewrite/recipes.yml", "io.jenkins.tools.pluginmodernizer.SetupDependabot"),
                text(""), // Need one minimum file to trigger the recipe
                // language=yaml
                yaml(
                        """
                    ---
                    version: 2
                    """,
                        sourceSpecs -> {
                            sourceSpecs.path(ArchetypeCommonFile.DEPENDABOT.getPath());
                        }));
    }

    @Test
    void shouldAddDependabot() {
        rewriteRun(
                spec -> spec.recipeFromResource(
                        "/META-INF/rewrite/recipes.yml", "io.jenkins.tools.pluginmodernizer.SetupDependabot"),
                text(""), // Need one minimum file to trigger the recipe
                // language=yaml
                yaml(
                        null,
                        """
                    version: 2
                    updates:
                    - package-ecosystem: maven
                      directory: /
                      schedule:
                        interval: monthly
                    - package-ecosystem: github-actions
                      directory: /
                      schedule:
                        interval: monthly
                    """,
                        sourceSpecs -> {
                            sourceSpecs.path(ArchetypeCommonFile.DEPENDABOT.getPath());
                        }));
    }

    /**
     * Note this test need to be adapted to fix the Jenkinsfile to use latest archetype
     */
    @Test
    void shouldNotAddJenkinsfileIfAlreadyPresent() {
        rewriteRun(
                spec -> spec.recipeFromResource(
                        "/META-INF/rewrite/recipes.yml", "io.jenkins.tools.pluginmodernizer.SetupJenkinsfile"),
                groovy("buildPlugin()", sourceSpecs -> {
                    sourceSpecs.path(ArchetypeCommonFile.JENKINSFILE.getPath());
                }));
    }

    @Test
    void shouldAddJenkinsfile() {
        rewriteRun(
                spec -> spec.recipeFromResource(
                        "/META-INF/rewrite/recipes.yml", "io.jenkins.tools.pluginmodernizer.SetupJenkinsfile"),
                // language=xml
                pomXml(
                        """
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
                      <artifactId>plugin</artifactId>
                      <version>0.0.1-SNAPSHOT</version>
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

    /**
     * Note this test need to be adapted to fix the .gitignore to ensure entries are merged
     */
    @Test
    void shouldNotAddGitIgnoreIfAlreadyPresent() {
        rewriteRun(
                spec -> spec.recipeFromResource(
                        "/META-INF/rewrite/recipes.yml", "io.jenkins.tools.pluginmodernizer.SetupGitIgnore"),
                text("target", sourceSpecs -> {
                    sourceSpecs.path(ArchetypeCommonFile.GITIGNORE.getPath());
                }));
    }

    @Test
    void shouldAddGitIgnore() {
        rewriteRun(
                spec -> spec.recipeFromResource(
                        "/META-INF/rewrite/recipes.yml", "io.jenkins.tools.pluginmodernizer.SetupGitIgnore"),
                text(""), // Need one minimum file to trigger the recipe
                text(
                        null,
                        """
                     target
                    \s
                     # mvn hpi:run
                     work

                     # IntelliJ IDEA project files
                     *.iml
                     *.iws
                     *.ipr
                     .idea
                    \s
                     # Eclipse project files
                     .settings
                     .classpath
                     .project
                    """,
                        sourceSpecs -> {
                            sourceSpecs.path(ArchetypeCommonFile.GITIGNORE.getPath());
                        }));
    }

    /**
     * Note1: this test need to be adapted to fix the security to ensure entries are merged
     * Note2: OpenRewrite provide a way to merge YAML files
     */
    @Test
    void shouldNotAddSecurityScanIfAlreadyPresent() {
        rewriteRun(
                spec -> spec.recipeFromResource(
                        "/META-INF/rewrite/recipes.yml", "io.jenkins.tools.pluginmodernizer.SetupSecurityScan"),
                yaml("{}", sourceSpecs -> {
                    sourceSpecs.path(ArchetypeCommonFile.WORKFLOW_SECURITY.getPath());
                }));
    }

    @Test
    void shouldAddSecurityScan() {
        rewriteRun(
                spec -> spec.recipeFromResource(
                        "/META-INF/rewrite/recipes.yml", "io.jenkins.tools.pluginmodernizer.SetupSecurityScan"),
                text(""), // Need one minimum file to trigger the recipe
                // language=yaml
                yaml(
                        null,
                        """
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
                          java-cache: 'maven'  # Optionally enable use of a build dependency cache. Specify 'maven' or 'gradle' as appropriate.
                          # java-version: 21  # Optionally specify what version of Java to set up for the build, or remove to use a recent default.
                    """,
                        sourceSpecs -> {
                            sourceSpecs.path(ArchetypeCommonFile.WORKFLOW_SECURITY.getPath());
                        }));
    }

    /**
     * Collect rewrite test dependencies from target/openrewrite-classpath directory
     *
     * @return List of Path
     */
    public static List<Path> collectRewriteTestDependencies() {
        try {
            List<Path> entries = Files.list(Path.of("target/openrewrite-jars"))
                    .filter(p -> p.toString().endsWith(".jar"))
                    .toList();
            LOG.debug("Collected rewrite test dependencies: {}", entries);
            return entries;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String getApiPluginVersion(String apiPlugin) throws IOException {
        return YamlPath.from(getClass().getResourceAsStream("/META-INF/rewrite/recipes.yml"))
                .readSingle(
                        "recipeList.'io.jenkins.tools.pluginmodernizer.core.recipes.ReplaceLibrariesWithApiPlugin'.(pluginArtifactId == %s).pluginVersion"
                                .formatted(apiPlugin));
    }
}
