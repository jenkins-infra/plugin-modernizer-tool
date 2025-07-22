package io.jenkins.tools.pluginmodernizer.core.recipes;

import static org.openrewrite.maven.Assertions.pomXml;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.openrewrite.test.RewriteTest;

/**
 * Test for {@link MigrateTomakehurstToWiremock}.
 */
@Execution(ExecutionMode.CONCURRENT)
public class MigrateTomakehurstToWiremockTest implements RewriteTest {
    @Test
    void testMigrateWireMockJre8Standalone() {
        rewriteRun(
                spec -> spec.recipe(new MigrateTomakehurstToWiremock()),
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
                          <dependencies>
                            <dependency>
                              <groupId>com.github.tomakehurst</groupId>
                              <artifactId>wiremock-jre8-standalone</artifactId>
                              <version>2.35.2</version>
                              <scope>test</scope>
                            </dependency>
                          </dependencies>
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
                          <dependencies>
                            <dependency>
                              <groupId>org.wiremock</groupId>
                              <artifactId>wiremock-standalone</artifactId>
                              <version>3.13.0</version>
                              <scope>test</scope>
                            </dependency>
                          </dependencies>
                        </project>
                        """));
    }

    @Test
    void testMigrateWireMock() {
        rewriteRun(
                spec -> spec.recipe(new MigrateTomakehurstToWiremock()),
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
                          <dependencies>
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
                        <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
                          <modelVersion>4.0.0</modelVersion>
                          <groupId>io.jenkins.plugins</groupId>
                          <artifactId>empty</artifactId>
                          <version>1.0.0-SNAPSHOT</version>
                          <packaging>hpi</packaging>
                          <name>Empty Plugin</name>
                          <dependencies>
                            <dependency>
                              <groupId>org.wiremock</groupId>
                              <artifactId>wiremock</artifactId>
                              <version>3.13.0</version>
                              <scope>test</scope>
                            </dependency>
                          </dependencies>
                        </project>
                        """));
    }
}
