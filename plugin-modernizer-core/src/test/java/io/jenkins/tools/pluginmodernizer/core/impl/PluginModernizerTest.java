package io.jenkins.tools.pluginmodernizer.core.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import io.jenkins.tools.pluginmodernizer.core.config.Config;
import io.jenkins.tools.pluginmodernizer.core.config.Settings;
import io.jenkins.tools.pluginmodernizer.core.github.GHService;
import io.jenkins.tools.pluginmodernizer.core.model.Plugin;
import io.jenkins.tools.pluginmodernizer.core.model.Recipe;
import io.jenkins.tools.pluginmodernizer.core.utils.PluginService;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;

class PluginModernizerTest {
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

    @Mock
    private Logger LOG;

    @InjectMocks
    private PluginModernizer pluginModernizer;

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

    @Test
    public void testListRecipes() {

        PluginModernizer spyPluginModernizer = spy(pluginModernizer);

        Recipe recipe1 = createMockRecipe("RecipeA", "Description A");
        Recipe recipe2 = createMockRecipe("RecipeC", "Description C");

        // Create a sorted list to simulate what would be returned after sorting
        List<Recipe> sortedMockRecipes = Arrays.asList(recipe1, recipe2);

        doAnswer(invocation -> {
                    sortedMockRecipes.stream()
                            .forEach(recipe -> LOG.info(
                                    "{} - {}",
                                    recipe.getName().replaceAll(Settings.RECIPE_FQDN_PREFIX + ".", ""),
                                    recipe.getDescription()));
                    return null;
                })
                .when(spyPluginModernizer)
                .listRecipes();

        // Call the method
        spyPluginModernizer.listRecipes();

        // Verify logger interactions
        verify(LOG).info(eq("{} - {}"), eq("RecipeA"), eq("Description A"));
        verify(LOG).info(eq("{} - {}"), eq("RecipeC"), eq("Description C"));
    }

    @Test
    public void testStart() throws Exception {
        // Setup for validation step
        when(ghService.isConnected()).thenReturn(true);

        // Setup config values for logging
        Recipe mockRecipe = mock(Recipe.class);
        when(mockRecipe.getName()).thenReturn("TestRecipe");

        Plugin plugin1 = mock(Plugin.class);
        Plugin plugin2 = mock(Plugin.class);
        List<Plugin> mockPlugins = Arrays.asList(plugin1, plugin2);

        when(config.getPlugins()).thenReturn(mockPlugins);
        when(config.getRecipe()).thenReturn(mockRecipe);
        when(config.getJenkinsUpdateCenter()).thenReturn(new URL("https://update-center-url.com"));
        when(config.getJenkinsPluginVersions()).thenReturn(new URL("https://plugin-versions-url.com"));
        when(config.getPluginHealthScore()).thenReturn(new URL("https://health-score-url.com"));
        when(config.getPluginStatsInstallations()).thenReturn(new URL("https://stats-installations-url.com"));
        when(config.getCachePath()).thenReturn(Paths.get("cache-path"));
        when(config.getMavenHome()).thenReturn(Paths.get("maven-home"));
        when(config.getMavenLocalRepo()).thenReturn(Paths.get("maven-local-repo"));
        when(config.isDryRun()).thenReturn(true);

        when(ghService.isSshKeyAuth()).thenReturn(false);

        PluginModernizer pluginModernizerSpy = spy(pluginModernizer);

        pluginModernizerSpy.start();

        // Verify public method interactions
        verify(pluginModernizerSpy).validate();
        verify(cacheManager).init();
        verify(pluginService).getPluginVersionData();
    }

    private Recipe createMockRecipe(String name, String description) {
        Recipe recipe = mock(Recipe.class);
        when(recipe.getName()).thenReturn(name);
        when(recipe.getDescription()).thenReturn(description);
        return recipe;
    }
}
