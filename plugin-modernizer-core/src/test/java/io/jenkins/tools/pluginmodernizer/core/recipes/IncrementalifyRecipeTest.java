package io.jenkins.tools.pluginmodernizer.core.recipes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.maven.shared.invoker.DefaultInvoker;
import org.apache.maven.shared.invoker.InvocationRequest;
import org.apache.maven.shared.invoker.InvocationResult;
import org.apache.maven.shared.invoker.MavenInvocationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.openrewrite.ExecutionContext;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.RecipeRun;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.maven.MavenParser;

public class IncrementalifyRecipeTest {

    @TempDir
    Path tempDir;

    @Test
    void testIncrementalifyRecipe() {
        String pomXml =
                """
                        <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
                          <modelVersion>4.0.0</modelVersion>
                          <groupId>com.example</groupId>
                          <artifactId>example</artifactId>
                          <version>1.0-SNAPSHOT</version>
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

        MavenParser parser = MavenParser.builder().build();
        ExecutionContext ctx = new InMemoryExecutionContext();

        List<? extends SourceFile> mavenDocuments =
                parser.parse(List.of(pomPath), tempDir, ctx).collect(Collectors.toList());
        InMemoryLargeSourceSet sourceSet = new InMemoryLargeSourceSet((List<SourceFile>) mavenDocuments);

        IncrementalifyRecipe recipe = new IncrementalifyRecipe();
        DefaultInvoker mockInvoker = Mockito.mock(DefaultInvoker.class);
        recipe.setInvokerForTesting(mockInvoker);

        InvocationResult mockResult = Mockito.mock(InvocationResult.class);
        Mockito.when(mockResult.getExitCode()).thenReturn(0);
        try {
            Mockito.when(mockInvoker.execute(Mockito.any())).thenReturn(mockResult);
        } catch (MavenInvocationException e) {
            throw new RuntimeException(e);
        }

        RecipeRun results = recipe.run(sourceSet, ctx);

        ArgumentCaptor<InvocationRequest> requestCaptor = ArgumentCaptor.forClass(InvocationRequest.class);
        try {
            Mockito.verify(mockInvoker).execute(requestCaptor.capture());
        } catch (MavenInvocationException e) {
            throw new RuntimeException(e);
        }
        assertThat(requestCaptor.getValue().getGoals()).contains("incrementals:incrementalify");
        // assertThat(results.getResults()).isNotEmpty();
        // assertThat(results.getResults().get(0).getAfter().printAll()).contains("incrementals-maven-plugin");
    }
}
