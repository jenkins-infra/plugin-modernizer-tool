package io.jenkins.tools.pluginmodernizer.core.recipes;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import io.jenkins.tools.pluginmodernizer.core.impl.MavenInvoker;
import io.jenkins.tools.pluginmodernizer.core.model.Plugin;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

public class IncrementalifyTest {

    @TempDir
    Path tempDir;

    @Test
    void testIncrementalifyRecipe() throws IOException {
        // Create a test directory structure
        Path pluginDir = tempDir.resolve("test-plugin");
        Files.createDirectories(pluginDir);

        @Language("xml")
        String pomXml =
                """
                        <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
                          <modelVersion>4.0.0</modelVersion>
                          <groupId>com.example</groupId>
                          <artifactId>example</artifactId>
                          <version>1.0-SNAPSHOT</version>
                        </project>
                        """;

        // Create the pom.xml file in the plugin directory
        Path pomPath = pluginDir.resolve("pom.xml");
        Files.writeString(pomPath, pomXml);

        // Create and configure the recipe
        Incrementalify recipe = new Incrementalify();

        // Create mock MavenInvoker
        MavenInvoker mockMavenInvoker = Mockito.mock(MavenInvoker.class);

        // Use reflection to set the mavenInvoker field
        try {
            java.lang.reflect.Field field = Incrementalify.class.getDeclaredField("mavenInvoker");
            field.setAccessible(true);
            field.set(recipe, mockMavenInvoker);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set mavenInvoker field", e);
        }

        // Execute the recipe on the POM file
        org.openrewrite.xml.XmlParser xmlParser = new org.openrewrite.xml.XmlParser();
        org.openrewrite.SourceFile sourceFile =
                xmlParser.parse(pomXml, pomPath.toString(), null).toList().get(0);
        recipe.getVisitor()
                .visit(
                        (org.openrewrite.xml.tree.Xml.Document) sourceFile,
                        new org.openrewrite.InMemoryExecutionContext());

        // Verify that the MavenInvoker was called with the correct arguments
        Mockito.verify(mockMavenInvoker)
                .invokeGoal(Mockito.any(Plugin.class), Mockito.eq("incrementals:incrementalify"));
    }

    @Test
    void testIncrementalifyRecipeWithExistingPlugin() {
        @Language("xml")
        String pomXml =
                """
                        <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
                          <modelVersion>4.0.0</modelVersion>
                          <groupId>com.example</groupId>
                          <artifactId>example</artifactId>
                          <version>1.0-SNAPSHOT</version>
                          <build>
                            <plugins>
                              <plugin>
                                <groupId>io.jenkins.tools.incrementals</groupId>
                                <artifactId>incrementals-maven-plugin</artifactId>
                                <version>1.8</version>
                                <configuration>
                                  <includes>
                                    <include>org.jenkins-ci.*</include>
                                    <include>io.jenkins.*</include>
                                  </includes>
                                  <generateBackupPoms>false</generateBackupPoms>
                                  <updateNonincremental>false</updateNonincremental>
                                </configuration>
                              </plugin>
                            </plugins>
                          </build>
                        </project>
                        """;
        Path pomPath = null;
        try {
            pomPath = File.createTempFile("pom", "xml", tempDir.toFile()).toPath();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        Path finalPomPath = pomPath;
        assertDoesNotThrow(() -> java.nio.file.Files.writeString(finalPomPath, pomXml));

        // Create and configure the recipe
        Incrementalify recipe = new Incrementalify();

        // Create mock MavenInvoker
        MavenInvoker mockMavenInvoker = Mockito.mock(MavenInvoker.class);

        // Use reflection to set the mavenInvoker field
        try {
            java.lang.reflect.Field field = Incrementalify.class.getDeclaredField("mavenInvoker");
            field.setAccessible(true);
            field.set(recipe, mockMavenInvoker);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set mavenInvoker field", e);
        }

        // Execute the recipe on the POM file
        org.openrewrite.xml.XmlParser xmlParser = new org.openrewrite.xml.XmlParser();
        org.openrewrite.SourceFile sourceFile =
                xmlParser.parse(pomXml, pomPath.toString(), null).toList().get(0);
        recipe.getVisitor()
                .visit(
                        (org.openrewrite.xml.tree.Xml.Document) sourceFile,
                        new org.openrewrite.InMemoryExecutionContext());

        // Verify that the MavenInvoker was called with the correct arguments
        Mockito.verify(mockMavenInvoker)
                .invokeGoal(Mockito.any(Plugin.class), Mockito.eq("incrementals:incrementalify"));
    }
}
