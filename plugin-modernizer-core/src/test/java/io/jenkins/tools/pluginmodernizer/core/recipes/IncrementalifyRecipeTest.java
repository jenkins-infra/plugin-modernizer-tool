package io.jenkins.tools.pluginmodernizer.core.recipes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
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

    /**
     * Sets an environment variable for testing purposes.
     *
     * @param key the environment variable name
     * @param value the environment variable value (null to remove)
     */
    @SuppressWarnings("unchecked")
    private void setEnv(String key, String value) {
        try {
            Map<String, String> env = System.getenv();
            Field field = env.getClass().getDeclaredField("m");
            field.setAccessible(true);
            Map<String, String> writableEnv = (Map<String, String>) field.get(env);
            if (value == null) {
                writableEnv.remove(key);
            } else {
                writableEnv.put(key, value);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to set environment variable", e);
        }
    }

    @Test
    void testIncrementalifyRecipe() {
        // Set M2_HOME environment variable for the test
        String originalM2Home = System.getenv("M2_HOME");
        try {
            // Create a mock M2_HOME directory
            File mockM2Home = new File(tempDir.toFile(), "mock-maven-home");
            mockM2Home.mkdirs();

            // Set the environment variable using reflection
            setEnv("M2_HOME", mockM2Home.getAbsolutePath());

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
        } finally {
            // Restore the original M2_HOME if there was one
            if (originalM2Home != null) {
                setEnv("M2_HOME", originalM2Home);
            } else {
                // Remove the environment variable if it wasn't set originally
                setEnv("M2_HOME", null);
            }
        }
        // assertThat(results.getResults()).isNotEmpty();
        // assertThat(results.getResults().get(0).getAfter().printAll()).contains("incrementals-maven-plugin");
    }
}
