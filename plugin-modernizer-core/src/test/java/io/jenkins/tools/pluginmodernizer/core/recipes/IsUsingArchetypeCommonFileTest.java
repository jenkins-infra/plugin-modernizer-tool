package io.jenkins.tools.pluginmodernizer.core.recipes;

import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.maven.Assertions.pomXml;
import static org.openrewrite.yaml.Assertions.yaml;

import io.jenkins.tools.pluginmodernizer.core.extractor.ArchetypeCommonFile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.openrewrite.test.RewriteTest;

/**
 * Test for {@link IsUsingArchetypeCommonFile}.
 */
@Execution(ExecutionMode.CONCURRENT)
public class IsUsingArchetypeCommonFileTest implements RewriteTest {

    @Test
    void testNotUsingCommonFile() {
        rewriteRun(
                spec -> spec.recipe(new IsUsingArchetypeCommonFile(ArchetypeCommonFile.DEPENDABOT)),
                // language=yml
                yaml("""
                    name: Empty
                    """),
                // language=java
                java(
                        """
                    public class Foo {
                        public void foo() {}
                    }
                    """),
                // language=xml
                pomXml(
                        """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>io.jenkins.plugins</groupId>
                  <artifactId>empty</artifactId>
                  <version>1.0.0-SNAPSHOT</version>
                  <name>Empty</name>
                </project>
                """));
    }

    @Test
    void testUsingCommonFile() {
        rewriteRun(
                spec -> spec.recipe(new IsUsingArchetypeCommonFile(ArchetypeCommonFile.DEPENDABOT)),
                // For some reason the search marker is not being written to the output
                // language=yaml
                yaml(
                        """
                    ---
                    name: Empty
                    """,
                        """
                    ---
                    name: Empty
                    """,
                        sourceSpecs -> {
                            sourceSpecs.path(".github/dependabot.yml");
                        }));
    }

    @Test
    void testCompareFullPath() {
        rewriteRun(
                spec -> spec.recipe(new IsUsingArchetypeCommonFile(ArchetypeCommonFile.DEPENDABOT)),
                // language=yml
                yaml(
                        """
                    ---
                    name: Empty
                    """,
                        sourceSpecs -> {
                            sourceSpecs.path("dependabot.yml");
                        }),
                // language=xml
                pomXml(
                        """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>io.jenkins.plugins</groupId>
                  <artifactId>empty</artifactId>
                  <version>1.0.0-SNAPSHOT</version>
                  <name>Empty</name>
                </project>
                """));
    }
}
