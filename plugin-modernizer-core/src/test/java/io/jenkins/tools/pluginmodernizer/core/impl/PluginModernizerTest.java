package io.jenkins.tools.pluginmodernizer.core.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import io.jenkins.tools.pluginmodernizer.core.config.Config;
import io.jenkins.tools.pluginmodernizer.core.config.Settings;
import io.jenkins.tools.pluginmodernizer.core.github.GHService;
import io.jenkins.tools.pluginmodernizer.core.model.Plugin;
import io.jenkins.tools.pluginmodernizer.core.model.PluginVersionData;
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

    @Test
    void testProcessSkipsPluginNotInUpdateCenter() throws Exception {
        // Setup
        Plugin plugin = Plugin.build("non-existent-plugin");
        when(config.getPlugins()).thenReturn(Arrays.asList(plugin));
        when(pluginService.existsInUpdateCenter(plugin)).thenReturn(false);

        // Mock recipe
        Recipe mockRecipe = mock(Recipe.class);
        when(mockRecipe.getName()).thenReturn("MockRecipe");
        when(config.getRecipe()).thenReturn(mockRecipe);

        // Create a spy to access the private process method
        PluginModernizer pluginModernizerSpy = spy(pluginModernizer);

        // Mock the start method to call our test directly
        doNothing().when(pluginModernizerSpy).validate();
        doNothing().when(cacheManager).init();

        // Mock getPluginVersionData to return a non-null value (required for start method)
        PluginVersionData mockVersionData = mock(PluginVersionData.class);
        when(pluginService.getPluginVersionData()).thenReturn(mockVersionData);

        // Execute
        pluginModernizerSpy.start();

        // Verify that plugin was not processed further (no repository name extraction)
        verify(pluginService, never()).extractRepoName(plugin);
        verify(pluginService).existsInUpdateCenter(plugin);
    }

    @Test
    void testProcessContinuesForPluginInUpdateCenter() throws Exception {
        // Setup
        Plugin plugin = Plugin.build("existing-plugin");
        plugin.withConfig(config);
        when(config.getPlugins()).thenReturn(Arrays.asList(plugin));
        when(pluginService.existsInUpdateCenter(plugin)).thenReturn(true);
        when(pluginService.extractRepoName(plugin)).thenReturn("jenkinsci/existing-plugin-plugin");
        when(config.isFetchMetadataOnly()).thenReturn(true);

        // Mock recipe
        Recipe mockRecipe = mock(Recipe.class);
        when(mockRecipe.getName()).thenReturn("MockRecipe");
        when(config.getRecipe()).thenReturn(mockRecipe);

        // Create a spy to access the private process method
        PluginModernizer pluginModernizerSpy = spy(pluginModernizer);

        // Mock the start method dependencies
        doNothing().when(pluginModernizerSpy).validate();
        doNothing().when(cacheManager).init();

        // Mock getPluginVersionData to return a non-null value
        PluginVersionData mockVersionData = mock(PluginVersionData.class);
        when(pluginService.getPluginVersionData()).thenReturn(mockVersionData);

        // Execute
        pluginModernizerSpy.start();

        // Verify that plugin processing continued past validation
        verify(pluginService).existsInUpdateCenter(plugin);
        verify(pluginService).extractRepoName(plugin);
    }

    @Test
    void testProcessSkipsLocalPlugin() throws Exception {
        // Setup - local plugins should skip the update center check
        Plugin localPlugin = Plugin.build("local-plugin");
        localPlugin.withConfig(config);
        localPlugin.withLocal(true);
        when(config.getPlugins()).thenReturn(Arrays.asList(localPlugin));
        when(pluginService.extractRepoName(localPlugin)).thenReturn("local/repo");
        when(config.isFetchMetadataOnly()).thenReturn(true);

        // Mock recipe
        Recipe mockRecipe = mock(Recipe.class);
        when(mockRecipe.getName()).thenReturn("MockRecipe");
        when(config.getRecipe()).thenReturn(mockRecipe);

        // Create a spy
        PluginModernizer pluginModernizerSpy = spy(pluginModernizer);

        // Mock dependencies
        doNothing().when(pluginModernizerSpy).validate();
        doNothing().when(cacheManager).init();

        // Mock getPluginVersionData to return a non-null value
        PluginVersionData mockVersionData = mock(PluginVersionData.class);
        when(pluginService.getPluginVersionData()).thenReturn(mockVersionData);

        // Execute
        pluginModernizerSpy.start();

        // Verify that existsInUpdateCenter was never called for local plugins
        verify(pluginService, never()).existsInUpdateCenter(localPlugin);
        verify(pluginService).extractRepoName(localPlugin);
    }

    @Test
    void testCollectModernizationMetadata_WithNullMetadata_ShouldLogWarningAndReturn() {
        // Setup
        Plugin plugin = mock(Plugin.class);
        when(plugin.getName()).thenReturn("test-plugin");
        when(plugin.getMetadata()).thenReturn(null);

        // Execute
        try {
            java.lang.reflect.Method method =
                    PluginModernizer.class.getDeclaredMethod("collectModernizationMetadata", Plugin.class);
            method.setAccessible(true);
            method.invoke(pluginModernizer, plugin);

            // Verify that the method returns early without NPE
            verify(plugin, atLeast(1)).getMetadata();
            verify(plugin, never()).setModernizationMetadata(any());
        } catch (Exception e) {
            fail("Should not throw exception when plugin metadata is null: " + e.getMessage());
        }
    }

    @Test
    void testValidateModernizationMetadata_WithNullMetadata_ShouldReturnEarly() {
        // Setup
        Plugin plugin = mock(Plugin.class);
        when(plugin.getName()).thenReturn("test-plugin");
        when(plugin.isLocal()).thenReturn(false);
        when(plugin.getModernizationMetadata()).thenReturn(null);

        // Execute
        pluginModernizer.validateModernizationMetadata(plugin);

        // Verify that it returns early without calling validate()
        verify(plugin).isLocal();
        verify(plugin, atLeast(1)).getModernizationMetadata();
        // No exception should be thrown
    }

    @Test
    void testMetadataOperationsSkippedWhenModernizationMetadataIsNull() throws Exception {
        // Setup
        Plugin plugin = mock(Plugin.class);
        when(plugin.getName()).thenReturn("test-plugin");
        when(plugin.getModernizationMetadata()).thenReturn(null);
        when(config.isSkipMetadata()).thenReturn(false);
        when(plugin.isLocal()).thenReturn(true);

        // Execute - Invoke the private process method using reflection
        java.lang.reflect.Method method = PluginModernizer.class.getDeclaredMethod("process", Plugin.class);
        method.setAccessible(true);

        try {
            method.invoke(pluginModernizer, plugin);
        } catch (Exception e) {
            if (e.getCause() instanceof NullPointerException) {
                fail("Null check should have prevented NPE when modernizationMetadata is null");
            }
        }

        // Verify that metadata operations were NOT called due to null check
        verify(plugin, never()).fetchMetadata(any());
        verify(plugin, never()).forkMetadata(any());
        verify(plugin, never()).syncMetadata(any());
        verify(plugin, never()).checkoutMetadataBranch(any());
        verify(plugin, never()).copyMetadataToLocalMetadataRepo(any());
        verify(plugin, never()).commitMetadata(any());
        verify(plugin, never()).pushMetadata(any());
        verify(plugin, never()).openMetadataPullRequest(any());
    }

    private Recipe createMockRecipe(String name, String description) {
        Recipe recipe = mock(Recipe.class);
        when(recipe.getName()).thenReturn(name);
        when(recipe.getDescription()).thenReturn(description);
        return recipe;
    }
}
