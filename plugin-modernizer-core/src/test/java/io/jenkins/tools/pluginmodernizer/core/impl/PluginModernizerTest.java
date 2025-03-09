package io.jenkins.tools.pluginmodernizer.core.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import io.jenkins.tools.pluginmodernizer.core.config.Config;
import io.jenkins.tools.pluginmodernizer.core.github.GHService;
import io.jenkins.tools.pluginmodernizer.core.utils.PluginService;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;

class PluginModernizerTest {
    @Mock private Config config;
    @Mock private MavenInvoker mavenInvoker;
    @Mock private GHService ghService;
    @Mock private PluginService pluginService;
    @Mock private CacheManager cacheManager;
    @Mock private Logger LOG;
    @InjectMocks private PluginModernizer pluginModernizer;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testValidate() {
        when(ghService.isConnected()).thenReturn(true);
        pluginModernizer.validate();
        verify(mavenInvoker).validateMaven();
        verify(mavenInvoker).validateMavenVersion();
        verify(ghService, never()).connect();
    }

    @Test
    void testIsDryRun() {
        when(config.isDryRun()).thenReturn(true);
        assertEquals(true, pluginModernizer.isDryRun());
    }

    @Test
    void testGetGithubOwner() {
        when(ghService.getGithubOwner()).thenReturn("owner");
        assertEquals("owner", pluginModernizer.getGithubOwner());
    }

    @Test
    void testGetSshPrivateKey() {
        Path keyPath = Paths.get("/test/key");
        when(config.getSshPrivateKey()).thenReturn(keyPath);
        assertEquals(keyPath.toString(), pluginModernizer.getSshPrivateKeyPath());
    }

    @Test
    void testGetMavenVersion_withValidVersion() {
        when(mavenInvoker.getMavenVersion()).thenReturn(new ComparableVersion("3.8.1"));
        assertEquals("3.8.1", pluginModernizer.getMavenVersion());
    }

    @Test
    void testGetMavenVersion_withNullVersion() {
        when(mavenInvoker.getMavenVersion()).thenReturn(null);
        assertEquals("unknown", pluginModernizer.getMavenVersion());
    }

    @Test
    void testGetMavenHome() {
        Path mavenPath = Paths.get("/test/maven");
        when(config.getMavenHome()).thenReturn(mavenPath);
        assertEquals(mavenPath.toString(), pluginModernizer.getMavenHome());
    }

    @Test
    void testGetMavenLocalRepo() {
        Path repoPath = Paths.get("/test/repo");
        when(config.getMavenLocalRepo()).thenReturn(repoPath);
        assertEquals(repoPath.toString(), pluginModernizer.getMavenLocalRepo());
    }

    @Test
    void testGetCachePath() {
        Path cachePath = Paths.get("/test/cache");
        when(config.getCachePath()).thenReturn(cachePath);
        assertEquals(cachePath.toString(), pluginModernizer.getCachePath());
    }

    @Test
    void testGetJavaVersion() {
        assertEquals(System.getProperty("java.version"), pluginModernizer.getJavaVersion());
    }

    @Test
    void testCleanCache() {
        pluginModernizer.cleanCache();
        verify(cacheManager).wipe();
    }
}
