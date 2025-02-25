package io.jenkins.tools.pluginmodernizer.core.recipes;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.openrewrite.ExecutionContext;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.Result;
import org.openrewrite.maven.MavenParser;
import org.openrewrite.xml.tree.Xml;

import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class IncrementalifyRecipeTest {

    @TempDir
    Path tempDir;

    @Test
    void testIncrementalifyRecipe() {
        String pomXml = """
                <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>com.example</groupId>
                  <artifactId>example</artifactId>
                  <version>1.0-SNAPSHOT</version>
                </project>
                """;

        Path pomPath = tempDir.resolve("pom.xml");
        try {
            java.nio.file.Files.writeString(pomPath, pomXml);
        } catch (java.io.IOException e) {
            throw new RuntimeException(e);
        }

        MavenParser parser = MavenParser.builder().build();
        List<Xml.Document> mavenDocuments = parser.parse(List.of(pomPath), tempDir, new InMemoryExecutionContext());

        Recipe recipe = new IncrementalifyRecipe();
        ExecutionContext ctx = new InMemoryExecutionContext();
        List<Result> results = recipe.run(mavenDocuments, ctx).getResults();

        assertThat(results).isNotEmpty();
        assertThat(results.get(0).getAfter().printAll()).contains("incrementals-maven-plugin");
    }
}
