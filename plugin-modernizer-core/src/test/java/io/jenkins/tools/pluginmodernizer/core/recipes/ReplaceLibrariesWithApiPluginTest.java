package io.jenkins.tools.pluginmodernizer.core.recipes;

import static org.openrewrite.maven.Assertions.pomXml;

import io.jenkins.tools.pluginmodernizer.core.config.Settings;
import io.jenkins.tools.pluginmodernizer.core.utils.Utils;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.openrewrite.test.RewriteTest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Test for {@link ReplaceLibrariesWithApiPlugin}.
 */
@Execution(ExecutionMode.CONCURRENT)
public class ReplaceLibrariesWithApiPluginTest implements RewriteTest {

    /**
     * LOGGER.
     */
    private static final Logger LOG = LoggerFactory.getLogger(ReplaceLibrariesWithApiPluginTest.class);

    @Test
    void replaceAsmByApiPluginWithBom() {
        rewriteRun(
                spec -> spec.executionContext(Utils.getMavenExecutionContext())
                        .recipe(new ReplaceLibrariesWithApiPlugin(
                                "io.jenkins.plugins",
                                "asm-api",
                                "9.8-135.vb_2239d08ee90",
                                Set.of(
                                        new ReplaceLibrariesWithApiPlugin.Library("org.ow2.asm", "asm"),
                                        new ReplaceLibrariesWithApiPlugin.Library("org.ow2.asm", "asm-analysis"),
                                        new ReplaceLibrariesWithApiPlugin.Library("org.ow2.asm", "asm-commons"),
                                        new ReplaceLibrariesWithApiPlugin.Library("org.ow2.asm", "asm-tree"),
                                        new ReplaceLibrariesWithApiPlugin.Library("org.ow2.asm", "asm-util")))),

                // language=xml
                pomXml("""
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/maven-v4_0_0.xsd">
                  <modelVersion>4.0.0</modelVersion>
                  <parent>
                    <groupId>org.jenkins-ci.plugins</groupId>
                    <artifactId>plugin</artifactId>
                    <version>6.2138.v03274d462c13</version>
                    <relativePath />
                  </parent>
                  <artifactId>antexec</artifactId>
                  <version>${changelist}</version>
                  <packaging>hpi</packaging>
                  <properties>
                    <!-- https://www.jenkins.io/doc/developer/plugin-development/choosing-jenkins-baseline/ -->
                    <jenkins.baseline>2.555</jenkins.baseline>
                    <jenkins.version>${jenkins.baseline}.1</jenkins.version>
                  </properties>
                  <dependencyManagement>
                    <dependencies>
                      <dependency>
                        <groupId>io.jenkins.tools.bom</groupId>
                        <artifactId>bom-${jenkins.baseline}.x</artifactId>
                        <version>%s</version>
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
                """.formatted(Settings.getRecommendedBomVersion()), """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/maven-v4_0_0.xsd">
                  <modelVersion>4.0.0</modelVersion>
                  <parent>
                    <groupId>org.jenkins-ci.plugins</groupId>
                    <artifactId>plugin</artifactId>
                    <version>6.2138.v03274d462c13</version>
                    <relativePath />
                  </parent>
                  <artifactId>antexec</artifactId>
                  <version>${changelist}</version>
                  <packaging>hpi</packaging>
                  <properties>
                    <!-- https://www.jenkins.io/doc/developer/plugin-development/choosing-jenkins-baseline/ -->
                    <jenkins.baseline>2.555</jenkins.baseline>
                    <jenkins.version>${jenkins.baseline}.1</jenkins.version>
                  </properties>
                  <dependencyManagement>
                    <dependencies>
                      <dependency>
                        <groupId>io.jenkins.tools.bom</groupId>
                        <artifactId>bom-${jenkins.baseline}.x</artifactId>
                        <version>%s</version>
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
                """.formatted(Settings.getBomVersion())));
    }

    @Test
    void replaceJsonByApiPluginWithBom() {
        rewriteRun(
                spec -> spec.executionContext(Utils.getMavenExecutionContext())
                        .recipe(new ReplaceLibrariesWithApiPlugin(
                                "io.jenkins.plugins",
                                "json-api",
                                "20250107-125.v28b_a_ffa_eb_f01",
                                Set.of(new ReplaceLibrariesWithApiPlugin.Library("org.json", "json")))),

                // language=xml
                pomXml("""
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
                      <groupId>org.json</groupId>
                      <artifactId>json</artifactId>
                      <version>20240303</version>
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
                """, """
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
                      <artifactId>json-api</artifactId>
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
    void replaceJacksonByApiPluginWithBom() {
        rewriteRun(
                spec -> spec.executionContext(Utils.getMavenExecutionContext())
                        .recipe(new ReplaceLibrariesWithApiPlugin(
                                "org.jenkins-ci.plugins",
                                "jackson2-api",
                                "2.22.3-447.v6a_93ce7d2c82",
                                Set.of(new ReplaceLibrariesWithApiPlugin.Library(
                                        "com.fasterxml.jackson.core", "jackson-databind")))),

                // language=xml
                pomXml("""
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/maven-v4_0_0.xsd">
                  <modelVersion>4.0.0</modelVersion>
                  <parent>
                    <groupId>org.jenkins-ci.plugins</groupId>
                    <artifactId>plugin</artifactId>
                    <version>4.88</version>
                    <relativePath />
                  </parent>
                  <artifactId>example</artifactId>
                  <version>${changelist}</version>
                  <packaging>hpi</packaging>
                  <properties>
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
                    </dependencies>
                  </dependencyManagement>
                  <dependencies>
                    <dependency>
                      <groupId>com.fasterxml.jackson.core</groupId>
                      <artifactId>jackson-databind</artifactId>
                      <version>2.18.2</version>
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
                """, """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/maven-v4_0_0.xsd">
                  <modelVersion>4.0.0</modelVersion>
                  <parent>
                    <groupId>org.jenkins-ci.plugins</groupId>
                    <artifactId>plugin</artifactId>
                    <version>4.88</version>
                    <relativePath />
                  </parent>
                  <artifactId>example</artifactId>
                  <version>${changelist}</version>
                  <packaging>hpi</packaging>
                  <properties>
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
                    </dependencies>
                  </dependencyManagement>
                  <dependencies>
                    <dependency>
                      <groupId>org.jenkins-ci.plugins</groupId>
                      <artifactId>jackson2-api</artifactId>
                      <version>2.22.3-447.v6a_93ce7d2c82</version>
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
    void replaceHttpClient4ByApiPluginWithBom() {
        rewriteRun(
                spec -> spec.executionContext(Utils.getMavenExecutionContext())
                        .recipe(new ReplaceLibrariesWithApiPlugin(
                                "org.jenkins-ci.plugins",
                                "apache-httpcomponents-client-4-api",
                                "4.5.14-269.vfa_2321039a_83",
                                Set.of(new ReplaceLibrariesWithApiPlugin.Library(
                                        "org.apache.httpcomponents", "httpclient")))),

                // language=xml
                pomXml("""
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/maven-v4_0_0.xsd">
                  <modelVersion>4.0.0</modelVersion>
                  <parent>
                    <groupId>org.jenkins-ci.plugins</groupId>
                    <artifactId>plugin</artifactId>
                    <version>4.88</version>
                    <relativePath />
                  </parent>
                  <artifactId>example</artifactId>
                  <version>${changelist}</version>
                  <packaging>hpi</packaging>
                  <properties>
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
                    </dependencies>
                  </dependencyManagement>
                  <dependencies>
                    <dependency>
                      <groupId>org.apache.httpcomponents</groupId>
                      <artifactId>httpclient</artifactId>
                      <version>4.5.14</version>
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
                """, """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/maven-v4_0_0.xsd">
                  <modelVersion>4.0.0</modelVersion>
                  <parent>
                    <groupId>org.jenkins-ci.plugins</groupId>
                    <artifactId>plugin</artifactId>
                    <version>4.88</version>
                    <relativePath />
                  </parent>
                  <artifactId>example</artifactId>
                  <version>${changelist}</version>
                  <packaging>hpi</packaging>
                  <properties>
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
                    </dependencies>
                  </dependencyManagement>
                  <dependencies>
                    <dependency>
                      <groupId>org.jenkins-ci.plugins</groupId>
                      <artifactId>apache-httpcomponents-client-4-api</artifactId>
                      <version>4.5.14-269.vfa_2321039a_83</version>
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
    void replaceSnakeyamlByApiPluginWithBom() {
        rewriteRun(
                spec -> spec.executionContext(Utils.getMavenExecutionContext())
                        .recipe(new ReplaceLibrariesWithApiPlugin(
                                "io.jenkins.plugins",
                                "snakeyaml-api",
                                "2.7-152.vc672728cf536",
                                Set.of(new ReplaceLibrariesWithApiPlugin.Library("org.yaml", "snakeyaml")))),

                // language=xml
                pomXml("""
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/maven-v4_0_0.xsd">
                  <modelVersion>4.0.0</modelVersion>
                  <parent>
                    <groupId>org.jenkins-ci.plugins</groupId>
                    <artifactId>plugin</artifactId>
                    <version>4.88</version>
                    <relativePath />
                  </parent>
                  <artifactId>example</artifactId>
                  <version>${changelist}</version>
                  <packaging>hpi</packaging>
                  <properties>
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
                    </dependencies>
                  </dependencyManagement>
                  <dependencies>
                    <dependency>
                      <groupId>org.yaml</groupId>
                      <artifactId>snakeyaml</artifactId>
                      <version>2.3</version>
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
                """, """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/maven-v4_0_0.xsd">
                  <modelVersion>4.0.0</modelVersion>
                  <parent>
                    <groupId>org.jenkins-ci.plugins</groupId>
                    <artifactId>plugin</artifactId>
                    <version>4.88</version>
                    <relativePath />
                  </parent>
                  <artifactId>example</artifactId>
                  <version>${changelist}</version>
                  <packaging>hpi</packaging>
                  <properties>
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
                    </dependencies>
                  </dependencyManagement>
                  <dependencies>
                    <dependency>
                      <groupId>io.jenkins.plugins</groupId>
                      <artifactId>snakeyaml-api</artifactId>
                      <version>2.7-152.vc672728cf536</version>
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
    void replaceOkhttpByApiPluginWithBom() {
        rewriteRun(
                spec -> spec.executionContext(Utils.getMavenExecutionContext())
                        .recipe(new ReplaceLibrariesWithApiPlugin(
                                "io.jenkins.plugins",
                                "okhttp-api",
                                "5.5.0-242.v1482d48d3ea_3",
                                Set.of(new ReplaceLibrariesWithApiPlugin.Library("com.squareup.okhttp3", "okhttp")))),

                // language=xml
                pomXml("""
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/maven-v4_0_0.xsd">
                  <modelVersion>4.0.0</modelVersion>
                  <parent>
                    <groupId>org.jenkins-ci.plugins</groupId>
                    <artifactId>plugin</artifactId>
                    <version>4.88</version>
                    <relativePath />
                  </parent>
                  <artifactId>example</artifactId>
                  <version>${changelist}</version>
                  <packaging>hpi</packaging>
                  <properties>
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
                    </dependencies>
                  </dependencyManagement>
                  <dependencies>
                    <dependency>
                      <groupId>com.squareup.okhttp3</groupId>
                      <artifactId>okhttp</artifactId>
                      <version>4.12.0</version>
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
                """, """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/maven-v4_0_0.xsd">
                  <modelVersion>4.0.0</modelVersion>
                  <parent>
                    <groupId>org.jenkins-ci.plugins</groupId>
                    <artifactId>plugin</artifactId>
                    <version>4.88</version>
                    <relativePath />
                  </parent>
                  <artifactId>example</artifactId>
                  <version>${changelist}</version>
                  <packaging>hpi</packaging>
                  <properties>
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
                    </dependencies>
                  </dependencyManagement>
                  <dependencies>
                    <dependency>
                      <groupId>io.jenkins.plugins</groupId>
                      <artifactId>okhttp-api</artifactId>
                      <version>5.5.0-242.v1482d48d3ea_3</version>
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
}
