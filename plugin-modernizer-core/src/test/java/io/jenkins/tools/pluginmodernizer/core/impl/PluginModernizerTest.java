package io.jenkins.tools.pluginmodernizer.core.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.jenkins.tools.pluginmodernizer.core.config.Config;
import io.jenkins.tools.pluginmodernizer.core.config.Settings;
import io.jenkins.tools.pluginmodernizer.core.github.GHService;
import io.jenkins.tools.pluginmodernizer.core.model.Plugin;
import io.jenkins.tools.pluginmodernizer.core.utils.PluginService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ExtendWith(MockitoExtension.class)
public class PluginModernizerTest {

    @Mock
    private Config config;

    @Mock
    private MavenInvoker mavenInvoker;

    @Mock
    private GHService ghService;

    @Mock
    private PluginService pluginService;

    @Mock
    private CacheManager cacheManager;

    @InjectMocks
    private PluginModernizer pluginModernizer;

    @BeforeEach
    void setUp() {
        pluginModernizer = new PluginModernizer();
        pluginModernizer.config = config;
        pluginModernizer.mavenInvoker = mavenInvoker;
        pluginModernizer.ghService = ghService;
        pluginModernizer.pluginService = pluginService;
        pluginModernizer.cacheManager = cacheManager;
    }

    @Test
    void testValidate() {
        pluginModernizer.validate();
        verify(mavenInvoker).validateMaven();
        verify(mavenInvoker).validateMavenVersion();
        verify(ghService).connect();
        verify(ghService).validate();
    }

    @Test
    void testListRecipes() {
        Logger logger = LoggerFactory.getLogger(PluginModernizer.class);
        pluginModernizer.listRecipes();
        Settings.AVAILABLE_RECIPES.stream()
                .sorted()
                .forEach(recipe -> verify(logger).info(
                        "{} - {}",
                        recipe.getName().replaceAll(Settings.RECIPE_FQDN_PREFIX + ".", ""),
                        recipe.getDescription()));
    }

    @Test
    void testIsDryRun() {
        when(config.isDryRun()).thenReturn(true);
        assertTrue(pluginModernizer.isDryRun());
    }

    @Test
    void testGetGithubOwner() {
        when(ghService.getGithubOwner()).thenReturn("test-owner");
        assertEquals("test-owner", pluginModernizer.getGithubOwner());
    }

    @Test
    void testGetSshPrivateKeyPath() {
        when(config.getSshPrivateKey()).thenReturn(Path.of("/path/to/key"));
        assertEquals("/path/to/key", pluginModernizer.getSshPrivateKeyPath());
    }

    @Test
    void testGetMavenVersion() {
        when(mavenInvoker.getMavenVersion()).thenReturn(new ComparableVersion("3.6.3"));
        assertEquals("3.6.3", pluginModernizer.getMavenVersion());
    }

    @Test
    void testGetMavenHome() {
        when(config.getMavenHome()).thenReturn(Path.of("/path/to/maven"));
        assertEquals("/path/to/maven", pluginModernizer.getMavenHome());
    }

    @Test
    void testGetMavenLocalRepo() {
        when(config.getMavenLocalRepo()).thenReturn(Path.of("/path/to/repo"));
        assertEquals("/path/to/repo", pluginModernizer.getMavenLocalRepo());
    }

    @Test
    void testGetCachePath() {
        when(config.getCachePath()).thenReturn(Path.of("/path/to/cache"));
        assertEquals("/path/to/cache", pluginModernizer.getCachePath());
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

    @Test
    void testStart() {
        Plugin plugin = mock(Plugin.class);
        when(config.getPlugins()).thenReturn(List.of(plugin));
        pluginModernizer.start();
        verify(plugin).withConfig(config);
        verify(plugin).withRepositoryName(anyString());
        verify(plugin).fork(ghService);
        verify(plugin).sync(ghService);
        verify(plugin).fetch(ghService);
        verify(plugin).checkoutBranch(ghService);
        verify(plugin).runOpenRewrite(mavenInvoker);
        verify(plugin).commit(ghService);
        verify(plugin).push(ghService);
        verify(plugin).openPullRequest(ghService);
    }

    @Test
    void testProcess() {
        Plugin plugin = mock(Plugin.class);
        pluginModernizer.process(plugin);
        verify(plugin).withConfig(config);
        verify(plugin).withRepositoryName(anyString());
        verify(plugin).fork(ghService);
        verify(plugin).sync(ghService);
        verify(plugin).fetch(ghService);
        verify(plugin).checkoutBranch(ghService);
        verify(plugin).runOpenRewrite(mavenInvoker);
        verify(plugin).commit(ghService);
        verify(plugin).push(ghService);
        verify(plugin).openPullRequest(ghService);
    }

    @Test
    void testCollectMetadata() {
        Plugin plugin = mock(Plugin.class);
        pluginModernizer.collectMetadata(plugin, true);
        verify(plugin).withJDK(JDK.JAVA_17);
        verify(plugin).collectMetadata(mavenInvoker);
        verify(plugin).copyMetadata(cacheManager);
        verify(plugin).loadMetadata(cacheManager);
        verify(plugin).enrichMetadata(pluginService);
    }

    @Test
    void testCompilePlugin() {
        Plugin plugin = mock(Plugin.class);
        PluginMetadata metadata = mock(PluginMetadata.class);
        when(plugin.getMetadata()).thenReturn(metadata);
        when(metadata.getJdks()).thenReturn(Set.of(JDK.JAVA_8));
        when(metadata.getJenkinsVersion()).thenReturn("2.164.1");
        JDK jdk = pluginModernizer.compilePlugin(plugin);
        assertEquals(JDK.JAVA_8, jdk);
        verify(plugin).withJDK(JDK.JAVA_8);
        verify(plugin).clean(mavenInvoker);
        verify(plugin).compile(mavenInvoker);
    }

    @Test
    void testVerifyPlugin() {
        Plugin plugin = mock(Plugin.class);
        PluginMetadata metadata = mock(PluginMetadata.class);
        when(plugin.getMetadata()).thenReturn(metadata);
        when(metadata.getJdks()).thenReturn(Set.of(JDK.JAVA_8));
        when(metadata.getJenkinsVersion()).thenReturn("2.164.1");
        JDK jdk = pluginModernizer.verifyPlugin(plugin);
        assertEquals(JDK.JAVA_8, jdk);
        verify(plugin).withJDK(JDK.JAVA_8);
        verify(plugin).clean(mavenInvoker);
        verify(plugin).format(mavenInvoker);
        verify(plugin).verify(mavenInvoker);
    }

    @Test
    void testPrintResults() {
        Plugin plugin = mock(Plugin.class);
        when(plugin.getName()).thenReturn("example");
        pluginModernizer.printResults(List.of(plugin));
        verify(plugin).getName();
    }

    @Test
    void testPrintModifiedFiles() {
        Plugin plugin = mock(Plugin.class);
        when(plugin.getModifiedFiles()).thenReturn(List.of("file1", "file2"));
        pluginModernizer.printModifiedFiles(plugin);
        verify(plugin).getModifiedFiles();
    }
}
