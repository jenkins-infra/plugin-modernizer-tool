package io.jenkins.tools.pluginmodernizer.core.recipes;

import static org.openrewrite.maven.Assertions.pomXml;

import io.jenkins.tools.pluginmodernizer.core.config.Settings;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.openrewrite.test.RewriteTest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Test for {@link UpgradeJenkinsVersion}.
 */
@Execution(ExecutionMode.CONCURRENT)
public class UpgradeJenkinsVersionTest implements RewriteTest {

    /**
     * LOGGER.
     */
    private static final Logger LOG = LoggerFactory.getLogger(UpgradeJenkinsVersionTest.class);

    @Test
    void testPerformUpgradeWithoutBom() {
        rewriteRun(
                spec -> spec.recipe(new UpgradeJenkinsVersion(Settings.getJenkinsMinimumVersion())),
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
                    <version>4.87</version>
                    <relativePath />
                  </parent>
                  <groupId>io.jenkins.plugins</groupId>
                  <artifactId>empty</artifactId>
                  <version>1.0.0-SNAPSHOT</version>
                  <packaging>hpi</packaging>
                  <name>Empty Plugin</name>
                  <properties>
                    <jenkins.version>%s</jenkins.version>
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
    void testPerformUpgradeWithBaselineWithoutBom() {
        rewriteRun(
                spec -> spec.recipe(new UpgradeJenkinsVersion(Settings.getJenkinsMinimumVersion())),
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
                    <jenkins.baseline>2.440</jenkins.baseline>
                    <jenkins.version>${jenkins.baseline}.3</jenkins.version>
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
                     <version>4.87</version>
                     <relativePath />
                   </parent>
                   <groupId>io.jenkins.plugins</groupId>
                   <artifactId>empty</artifactId>
                   <version>1.0.0-SNAPSHOT</version>
                   <packaging>hpi</packaging>
                   <name>Empty Plugin</name>
                   <properties>
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
                 </project>
                 """
                                .formatted(
                                        Settings.getJenkinsMinimumBaseline(),
                                        Settings.getJenkinsMinimumPatchVersion())));
    }

    @Test
    void testPerformUpgradeWithoutBaselineWithBomAndUpgradeBomIfDoesntExistsForNewJenkinsVersion() {
        rewriteRun(
                spec -> spec.recipe(new UpgradeJenkinsVersion(Settings.getJenkinsMinimumVersion())),
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
                     <version>4.87</version>
                     <relativePath />
                   </parent>
                   <groupId>io.jenkins.plugins</groupId>
                   <artifactId>empty</artifactId>
                   <version>1.0.0-SNAPSHOT</version>
                   <packaging>hpi</packaging>
                   <name>Empty Plugin</name>
                   <properties>
                     <jenkins.version>%s</jenkins.version>
                   </properties>
                   <dependencyManagement>
                     <dependencies>
                       <dependency>
                         <groupId>io.jenkins.tools.bom</groupId>
                         <artifactId>%s</artifactId>
                         <version>%s</version>
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
                                .formatted(
                                        Settings.getJenkinsMinimumVersion(),
                                        Settings.getBomArtifactId(),
                                        Settings.getBomVersion())));
    }

    @Test
    void testPerformUpgradeWithBaselineWithBomAndUpgradeBomIfDoesntExistsForNewJenkinsVersion() {
        rewriteRun(
                spec -> spec.recipe(new UpgradeJenkinsVersion(Settings.getJenkinsMinimumVersion())),
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
                    <jenkins.baseline>2.440</jenkins.baseline>
                    <jenkins.version>${jenkins.baseline}.3</jenkins.version>
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
                     <version>4.87</version>
                     <relativePath />
                   </parent>
                   <groupId>io.jenkins.plugins</groupId>
                   <artifactId>empty</artifactId>
                   <version>1.0.0-SNAPSHOT</version>
                   <packaging>hpi</packaging>
                   <name>Empty Plugin</name>
                   <properties>
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
                                        Settings.getBomVersion())));
    }
}
