package io.jenkins.tools.pluginmodernizer.core.recipes;

import static org.openrewrite.maven.Assertions.pomXml;

import io.jenkins.tools.pluginmodernizer.core.utils.Utils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.openrewrite.config.Environment;
import org.openrewrite.test.RewriteTest;

/**
 * {@code UpgradeToJava17} must raise the Jenkins core, not only the compiler property.
 * <p>
 * {@code maven-hpi-plugin:validate} replaces {@code maven.compiler.release} with the
 * class-file version of {@code jenkins-core}. A core such as 2.440.3 is Java 11
 * bytecode, so text blocks introduced by the language migration fail the next
 * {@code rewrite:run} (metadata collection) with a build error and no metadata file.
 * 2.479.3 is the first LTS patch built with Java 17 that the 2.479.x BOM accepts.
 */
@Execution(ExecutionMode.CONCURRENT)
public class UpgradeToJava17Test implements RewriteTest {

    @Test
    void raisesJenkinsCoreSoHpiKeepsJava17Release() {
        rewriteRun(
                spec -> spec.executionContext(Utils.getMavenExecutionContext())
                        .recipe(Environment.builder()
                                .scanRuntimeClasspath()
                                .build()
                                .activateRecipes("io.jenkins.tools.pluginmodernizer.UpgradeToJava17")),
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
                          <artifactId>javadoc</artifactId>
                          <version>1.0.0-SNAPSHOT</version>
                          <packaging>hpi</packaging>
                          <properties>
                            <jenkins.baseline>2.440</jenkins.baseline>
                            <jenkins.version>${jenkins.baseline}.3</jenkins.version>
                          </properties>
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
                          <artifactId>javadoc</artifactId>
                          <version>1.0.0-SNAPSHOT</version>
                          <packaging>hpi</packaging>
                          <properties>
                            <jenkins.baseline>2.479</jenkins.baseline>
                            <jenkins.version>${jenkins.baseline}.3</jenkins.version>
                            <maven.compiler.release>17</maven.compiler.release>
                          </properties>
                        </project>
                        """));
    }
}
