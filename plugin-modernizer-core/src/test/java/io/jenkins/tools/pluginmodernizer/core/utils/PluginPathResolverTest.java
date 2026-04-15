package io.jenkins.tools.pluginmodernizer.core.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.jenkins.tools.pluginmodernizer.core.model.Plugin;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PluginPathResolverTest {

    @TempDir
    private Path tempDir;

    private final PluginPathResolver resolver = new PluginPathResolver();

    @Test
    void shouldResolveSingleModulePlugin() throws Exception {
        Files.writeString(
                tempDir.resolve("pom.xml"),
                """
                <project>
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>test</groupId>
                  <artifactId>my-plugin</artifactId>
                  <version>1.0</version>
                  <packaging>hpi</packaging>
                </project>
                """);
        Plugin plugin = resolver.resolve(tempDir);
        assertEquals("my-plugin", plugin.getName());
        assertTrue(plugin.isLocal());
    }

    @Test
    void shouldResolveMultiModulePlugin() throws Exception {
        Files.writeString(
                tempDir.resolve("pom.xml"),
                """
                <project>
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>test</groupId>
                  <artifactId>parent</artifactId>
                  <version>1.0</version>
                  <packaging>pom</packaging>
                </project>
                """);
        Path sub = tempDir.resolve("my-plugin");
        Files.createDirectories(sub);
        Files.writeString(
                sub.resolve("pom.xml"),
                """
                <project>
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>test</groupId>
                  <artifactId>my-plugin</artifactId>
                  <version>1.0</version>
                  <packaging>hpi</packaging>
                </project>
                """);
        Plugin plugin = resolver.resolve(tempDir);
        assertEquals("my-plugin", plugin.getName());
        assertTrue(plugin.isLocal());
    }

    @Test
    void shouldThrowForNonExistentDirectory() {
        assertThrows(IllegalArgumentException.class, () -> resolver.resolve(tempDir.resolve("missing")));
    }

    @Test
    void shouldThrowForMissingPom() {
        assertThrows(IllegalArgumentException.class, () -> resolver.resolve(tempDir));
    }
}
