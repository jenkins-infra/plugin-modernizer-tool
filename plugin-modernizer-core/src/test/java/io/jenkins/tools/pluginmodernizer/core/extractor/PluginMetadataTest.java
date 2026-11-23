package io.jenkins.tools.pluginmodernizer.core.extractor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

/**
 * Unit tests for {@link PluginMetadata}.
 */
@Execution(ExecutionMode.CONCURRENT)
public class PluginMetadataTest {

    @Test
    public void testGetFileWhenCommonFilesIsNull() {
        PluginMetadata metadata = new PluginMetadata();
        assertNull(metadata.getFile(Path.of("Jenkinsfile")));
    }

    @Test
    public void testHasFileByPathWhenCommonFilesIsNull() {
        PluginMetadata metadata = new PluginMetadata();
        assertFalse(metadata.hasFile(Path.of("Jenkinsfile")));
    }

    @Test
    public void testHasFileByArchetypeCommonFileWhenCommonFilesIsNull() {
        PluginMetadata metadata = new PluginMetadata();
        assertFalse(metadata.hasFile(ArchetypeCommonFile.JENKINSFILE));
    }

    @Test
    public void testGetFileWhenCommonFilesIsPresent() {
        PluginMetadata metadata = new PluginMetadata();
        metadata.setCommonFiles(List.of(ArchetypeCommonFile.JENKINSFILE));
        assertEquals(ArchetypeCommonFile.JENKINSFILE, metadata.getFile(Path.of("Jenkinsfile")));
        assertNull(metadata.getFile(Path.of("nonexistent")));
    }

    @Test
    public void testHasFileByPathWhenCommonFilesIsPresent() {
        PluginMetadata metadata = new PluginMetadata();
        metadata.setCommonFiles(List.of(ArchetypeCommonFile.JENKINSFILE));
        assertTrue(metadata.hasFile(Path.of("Jenkinsfile")));
        assertFalse(metadata.hasFile(Path.of("nonexistent")));
    }

    @Test
    public void testHasFileByArchetypeCommonFileWhenCommonFilesIsPresent() {
        PluginMetadata metadata = new PluginMetadata();
        metadata.setCommonFiles(List.of(ArchetypeCommonFile.JENKINSFILE));
        assertTrue(metadata.hasFile(ArchetypeCommonFile.JENKINSFILE));
        assertFalse(metadata.hasFile(ArchetypeCommonFile.POM));
    }
}
