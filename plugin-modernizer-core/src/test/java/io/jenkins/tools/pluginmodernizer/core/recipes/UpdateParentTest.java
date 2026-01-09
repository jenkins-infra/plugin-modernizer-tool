package io.jenkins.tools.pluginmodernizer.core.recipes;

import static org.openrewrite.maven.Assertions.pomXml;

import io.jenkins.tools.pluginmodernizer.core.config.Settings;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.openrewrite.Issue;
import org.openrewrite.maven.MavenParser;
import org.openrewrite.test.RewriteTest;

/**
 * Test for {@link UpdateParent}.
 */
@Execution(ExecutionMode.CONCURRENT)
public class UpdateParentTest implements RewriteTest {

    @Test
    void shouldSkipIfNoParent() {
        rewriteRun(
                spec -> spec.recipe(new UpdateParent()),
                // language=xml
                pomXml("""
                 <?xml version="1.0" encoding="UTF-8"?>
                 <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
                   <modelVersion>4.0.0</modelVersion>
                   <groupId>io.jenkins.plugins</groupId>
                   <artifactId>empty</artifactId>
                   <version>1.0.0-SNAPSHOT</version>
                   <packaging>hpi</packaging>
                   <name>Empty Plugin</name>
                 </project>
                 """));
    }

    @Test
    void shouldUpdateToLatestReleasedWithoutMavenConfig() {
        rewriteRun(
                spec -> spec.recipe(new UpdateParent()),
                // language=xml
                pomXml("""
                 <?xml version="1.0" encoding="UTF-8"?>
                 <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
                   <modelVersion>4.0.0</modelVersion>
                   <groupId>io.jenkins.plugins</groupId>
                   <artifactId>empty</artifactId>
                   <version>1.0.0-SNAPSHOT</version>
                   <parent>
                        <groupId>org.jenkins-ci.plugins</groupId>
                        <artifactId>plugin</artifactId>
                        <version>5.1</version>
                        <relativePath />
                   </parent>
                   <packaging>hpi</packaging>
                   <name>Empty Plugin</name>
                    <repositories>
                      <repository>
                        <id>repo.jenkins-ci.org</id>
                        <url>https://repo.jenkins-ci.org/public/</url>
                      </repository>
                    </repositories>
                 </project>
                 """, """
                 <?xml version="1.0" encoding="UTF-8"?>
                 <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
                   <modelVersion>4.0.0</modelVersion>
                   <groupId>io.jenkins.plugins</groupId>
                   <artifactId>empty</artifactId>
                   <version>1.0.0-SNAPSHOT</version>
                   <parent>
                        <groupId>org.jenkins-ci.plugins</groupId>
                        <artifactId>plugin</artifactId>
                        <version>%s</version>
                        <relativePath />
                   </parent>
                   <packaging>hpi</packaging>
                   <name>Empty Plugin</name>
                    <repositories>
                      <repository>
                        <id>repo.jenkins-ci.org</id>
                        <url>https://repo.jenkins-ci.org/public/</url>
                      </repository>
                    </repositories>
                 </project>
                 """.formatted(Settings.getJenkinsParentVersion())));
    }

    @Test
    void shouldUpdateToLatestReleasedWithoutMavenConfigAndFilter() {
        rewriteRun(
                spec -> spec.recipe(new UpdateParent(5)),
                // language=xml
                pomXml("""
                 <?xml version="1.0" encoding="UTF-8"?>
                 <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
                   <modelVersion>4.0.0</modelVersion>
                   <groupId>io.jenkins.plugins</groupId>
                   <artifactId>empty</artifactId>
                   <version>1.0.0-SNAPSHOT</version>
                   <parent>
                        <groupId>org.jenkins-ci.plugins</groupId>
                        <artifactId>plugin</artifactId>
                        <version>5.1</version>
                        <relativePath />
                   </parent>
                   <packaging>hpi</packaging>
                   <name>Empty Plugin</name>
                    <repositories>
                      <repository>
                        <id>repo.jenkins-ci.org</id>
                        <url>https://repo.jenkins-ci.org/public/</url>
                      </repository>
                    </repositories>
                 </project>
                 """, """
                 <?xml version="1.0" encoding="UTF-8"?>
                 <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
                   <modelVersion>4.0.0</modelVersion>
                   <groupId>io.jenkins.plugins</groupId>
                   <artifactId>empty</artifactId>
                   <version>1.0.0-SNAPSHOT</version>
                   <parent>
                        <groupId>org.jenkins-ci.plugins</groupId>
                        <artifactId>plugin</artifactId>
                        <version>5.2102.v5f5fe09fccf1</version>
                        <relativePath />
                   </parent>
                   <packaging>hpi</packaging>
                   <name>Empty Plugin</name>
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
                    spec.recipe(new UpdateParent());
                },
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
                    <repositories>
                      <repository>
                        <id>repo.jenkins-ci.org</id>
                        <url>https://repo.jenkins-ci.org/public/</url>
                      </repository>
                    </repositories>
                 </project>
                 """, """
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
                    <repositories>
                      <repository>
                        <id>repo.jenkins-ci.org</id>
                        <url>https://repo.jenkins-ci.org/public/</url>
                      </repository>
                    </repositories>
                 </project>
                 """.formatted(Settings.getJenkinsParentVersion())));
    }
}
