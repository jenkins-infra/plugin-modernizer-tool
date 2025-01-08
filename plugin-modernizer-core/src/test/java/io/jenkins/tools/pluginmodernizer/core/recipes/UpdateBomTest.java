package io.jenkins.tools.pluginmodernizer.core.recipes;

import static org.openrewrite.maven.Assertions.pomXml;

import org.junit.jupiter.api.Test;
import org.openrewrite.Issue;
import org.openrewrite.maven.MavenParser;
import org.openrewrite.test.RewriteTest;

/**
 * Test for {@link UpdateBom}.
 */
public class UpdateBomTest implements RewriteTest {

    @Test
    void shouldSkipIfNoBom() {
        rewriteRun(
                spec -> spec.recipe(new UpdateBom()),
                // language=xml
                pomXml(
                        """
                 <?xml version="1.0" encoding="UTF-8"?>
                 <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
                   <modelVersion>4.0.0</modelVersion>
                   <groupId>io.jenkins.plugins</groupId>
                   <artifactId>empty</artifactId>
                   <version>1.0.0-SNAPSHOT</version>
                   <packaging>hpi</packaging>
                   <name>Empty Plugin</name>
                   <properties>
                        <jenkins.version>2.440</jenkins.version>
                   </properties>
                 </project>
                 """));
    }

    @Test
    void shouldUpdateToLatestReleasedWithoutMavenConfig() {
        rewriteRun(
                spec -> spec.recipe(new UpdateBom()),
                // language=xml
                pomXml(
                        """
                 <?xml version="1.0" encoding="UTF-8"?>
                 <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
                   <modelVersion>4.0.0</modelVersion>
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
                          <version>2746.vb_79a_1d3e7b_c8</version>
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
                 </project>
                 """,
                        """
                 <?xml version="1.0" encoding="UTF-8"?>
                 <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
                   <modelVersion>4.0.0</modelVersion>
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
                 </project>
                 """));
    }

    @Test
    @Issue("https://github.com/jenkins-infra/plugin-modernizer-tool/issues/534")
    void shouldUpdateToLatestReleasedWithIncrementalsEnabled() {
        rewriteRun(
                spec -> {
                    spec.parser(MavenParser.builder().activeProfiles("consume-incrementals"));
                    spec.recipe(new UpdateBom());
                },
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
                          <version>2746.vb_79a_1d3e7b_c8</version>
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
                 </project>
                 """));
    }

    @Test
    void shouldUpdateToLatestIncrementalsWithoutMavenConfig() {
        rewriteRun(
                spec -> spec.recipe(new UpdateBom()),
                // language=xml
                pomXml(
                        """
                 <?xml version="1.0" encoding="UTF-8"?>
                 <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
                   <modelVersion>4.0.0</modelVersion>
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
                          <version>2747.v42d8f18d1741</version>
                          <type>pom</type>
                          <scope>import</scope>
                        </dependency>
                      </dependencies>
                    </dependencyManagement>
                    <repositories>
                      <repository>
                        <id>repo.jenkins-ci.org</id>
                        <url>https://repo.jenkins-ci.org/incrementals/</url>
                      </repository>
                    </repositories>
                 </project>
                 """,
                        """
                 <?xml version="1.0" encoding="UTF-8"?>
                 <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
                   <modelVersion>4.0.0</modelVersion>
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
                          <version>3440.v7ed0db_c20c5e</version>
                          <type>pom</type>
                          <scope>import</scope>
                        </dependency>
                      </dependencies>
                    </dependencyManagement>
                    <repositories>
                      <repository>
                        <id>repo.jenkins-ci.org</id>
                        <url>https://repo.jenkins-ci.org/incrementals/</url>
                      </repository>
                    </repositories>
                 </project>
                 """));
    }

    @Test
    void shouldSkipIfOnParentBom() {
        rewriteRun(
                spec -> spec.recipe(new UpdateBom()),
                // language=xml
                pomXml(
                        """
                 <?xml version="1.0" encoding="UTF-8"?>
                 <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
                   <modelVersion>4.0.0</modelVersion>
                   <parent>
                     <groupId>org.jvnet.hudson.plugins</groupId>
                     <artifactId>analysis-pom</artifactId>
                     <version>10.0.0</version>
                     <relativePath />
                   </parent>
                   <groupId>io.jenkins.plugins</groupId>
                   <artifactId>checks</artifactId>
                   <version>1.0.0-SNAPSHOT</version>
                   <packaging>hpi</packaging>
                   <name>Checks Plugin</name>
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
    void shouldUpgradePropertyForVersion() {
        rewriteRun(
                spec -> spec.recipe(new UpdateBom()),
                // language=xml
                pomXml(
                        """
                 <?xml version="1.0" encoding="UTF-8"?>
                 <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
                   <modelVersion>4.0.0</modelVersion>
                   <groupId>io.jenkins.plugins</groupId>
                   <artifactId>non-standard</artifactId>
                   <version>1.0.0-SNAPSHOT</version>
                   <packaging>hpi</packaging>
                   <name>Checks Plugin</name>
                   <properties>
                     <jenkins.version>2.401.3</jenkins.version>
                     <bom.artifactId>bom-2.401.x</bom.artifactId>
                     <bom.version>2555.v3190a_8a_c60c6</bom.version>
                   </properties>
                   <dependencyManagement>
                     <dependencies>
                       <dependency>
                         <groupId>io.jenkins.tools.bom</groupId>
                         <artifactId>${bom.artifactId}</artifactId>
                         <version>${bom.version}</version>
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
                 </project>
                 """,
                        """
                 <?xml version="1.0" encoding="UTF-8"?>
                 <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
                   <modelVersion>4.0.0</modelVersion>
                   <groupId>io.jenkins.plugins</groupId>
                   <artifactId>non-standard</artifactId>
                   <version>1.0.0-SNAPSHOT</version>
                   <packaging>hpi</packaging>
                   <name>Checks Plugin</name>
                   <properties>
                     <jenkins.version>2.401.3</jenkins.version>
                     <bom.artifactId>bom-2.401.x</bom.artifactId>
                     <bom.version>2745.vc7b_fe4c876fa_</bom.version>
                   </properties>
                   <dependencyManagement>
                     <dependencies>
                       <dependency>
                         <groupId>io.jenkins.tools.bom</groupId>
                         <artifactId>${bom.artifactId}</artifactId>
                         <version>${bom.version}</version>
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
                 </project>
                 """));
    }
}
