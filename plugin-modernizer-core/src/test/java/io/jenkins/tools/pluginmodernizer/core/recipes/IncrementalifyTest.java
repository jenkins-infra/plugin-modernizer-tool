package io.jenkins.tools.pluginmodernizer.core.recipes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import org.apache.maven.shared.invoker.DefaultInvoker;
import org.apache.maven.shared.invoker.InvocationRequest;
import org.apache.maven.shared.invoker.InvocationResult;
import org.apache.maven.shared.invoker.MavenInvocationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

public class IncrementalifyTest {

    @TempDir
    Path tempDir;

    @Test
    void testIncrementalifyRecipe() throws MavenInvocationException {
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

        // Create and configure the recipe
        Incrementalify recipe = new Incrementalify();
        // Skip M2_HOME check for testing
        recipe.setSkipM2HomeCheckForTesting(true);

        // Create mock invoker and configure it
        DefaultInvoker mockInvoker = Mockito.mock(DefaultInvoker.class);
        recipe.setInvokerForTesting(mockInvoker);

        InvocationResult mockResult = Mockito.mock(InvocationResult.class);
        Mockito.when(mockResult.getExitCode()).thenReturn(0);
        Mockito.when(mockInvoker.execute(Mockito.any())).thenReturn(mockResult);

        // Execute the recipe directly on the POM file
        recipe.executeOnPomFile(pomPath.toFile());

        // Verify that the invoker was called with the correct arguments
        ArgumentCaptor<InvocationRequest> requestCaptor = ArgumentCaptor.forClass(InvocationRequest.class);
        Mockito.verify(mockInvoker).execute(requestCaptor.capture());
        assertThat(requestCaptor.getValue().getGoals()).contains("incrementals:incrementalify");
        // assertThat(results.getResults()).isNotEmpty();
        // assertThat(results.getResults().get(0).getAfter().printAll()).contains("incrementals-maven-plugin");
    }
}
