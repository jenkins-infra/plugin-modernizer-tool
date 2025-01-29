package io.jenkins.tools.pluginmodernizer.core.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doReturn;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.google.inject.Guice;
import io.jenkins.tools.pluginmodernizer.core.GuiceModule;
import io.jenkins.tools.pluginmodernizer.core.config.Config;
import io.jenkins.tools.pluginmodernizer.core.impl.CacheManager;
import io.jenkins.tools.pluginmodernizer.core.model.HealthScoreData;
import io.jenkins.tools.pluginmodernizer.core.model.ModernizerException;
import io.jenkins.tools.pluginmodernizer.core.model.Plugin;
import io.jenkins.tools.pluginmodernizer.core.model.PluginInstallationStatsData;
import io.jenkins.tools.pluginmodernizer.core.model.UpdateCenterData;
import java.lang.reflect.Field;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.platform.commons.util.ReflectionUtils;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith({MockitoExtension.class})
@Execution(ExecutionMode.CONCURRENT)
class PluginServiceTest {

    @TempDir
    private Path tempDir;

    @Test
    public void shouldExtractRepoName() throws Exception {
        CacheManager cacheManager = Mockito.mock(CacheManager.class);
        Path cacheRoot = Mockito.mock(Path.class);
        Config config = Mockito.mock(Config.class);
        UpdateCenterData updateCenterData =
                setup(config, cacheManager, cacheRoot).getLeft();
        setupUpdateCenterMocks(updateCenterData, cacheManager, cacheRoot);
        PluginService service = getService(config, cacheManager);
        String result = service.extractRepoName(Plugin.build("valid-plugin").withConfig(config));
        assertEquals("valid-url", result);
    }

    @Test
    public void shouldExtractRepoNameForLocalDefaultPluginWithGitHubRepo() throws Exception {
        CacheManager cacheManager = Mockito.mock(CacheManager.class);
        Path cacheRoot = Mockito.mock(Path.class);
        Config config = Mockito.mock(Config.class);
        setup(config, cacheManager, cacheRoot);
        PluginService service = getService(config, cacheManager);

        // language=xml
        String pom =
                """
                    <project>
                        <properties>
                            <gitHubRepo>jenkinsci/foobar</gitHubRepo>
                        </properties>
                    </project>
                    """;

        Plugin plugin = Plugin.build("valid-plugin").withConfig(config);
        plugin.withLocal(true);
        plugin.withLocalRepository(tempDir);
        Files.writeString(tempDir.resolve("pom.xml"), pom);
        String result = service.extractRepoName(plugin);
        assertEquals("foobar", result);
    }

    @Test
    public void shouldExtractRepoNameForLocalDefaultPluginWithScmRepoRepo() throws Exception {
        CacheManager cacheManager = Mockito.mock(CacheManager.class);
        Path cacheRoot = Mockito.mock(Path.class);
        Config config = Mockito.mock(Config.class);
        setup(config, cacheManager, cacheRoot);
        PluginService service = getService(config, cacheManager);

        // language=xml
        String pom =
                """
                    <project>
                        <properties>
                            <scm>
                               <connection>scm:git:https://github.com/jenkinsci/FOO_Bar-plugin.git</connection>
                            </scm>
                        </properties>
                    </project>
                    """;

        Plugin plugin = Plugin.build("valid-plugin").withConfig(config);
        plugin.withLocal(true);
        plugin.withLocalRepository(tempDir);
        Files.writeString(tempDir.resolve("pom.xml"), pom);
        String result = service.extractRepoName(plugin);
        assertEquals("FOO_Bar-plugin", result);
    }

    @Test
    public void shouldExtractRepoNameForLocalDefaultPluginFallbackFolder() throws Exception {
        CacheManager cacheManager = Mockito.mock(CacheManager.class);
        Path cacheRoot = Mockito.mock(Path.class);
        Config config = Mockito.mock(Config.class);
        setup(config, cacheManager, cacheRoot);
        PluginService service = getService(config, cacheManager);

        // language=xml
        String pom =
                """
                    <project>
                        <properties/>
                    </project>
                    """;

        Plugin plugin = Plugin.build("valid-plugin").withConfig(config);
        plugin.withLocal(true);
        plugin.withLocalRepository(tempDir);
        Files.writeString(tempDir.resolve("pom.xml"), pom);
        String result = service.extractRepoName(plugin);
        assertEquals(tempDir.getFileName().toString(), result);
    }

    @Test
    public void shouldExtractRepoNameForLocalDefaultPluginFallbackFolderParent() throws Exception {
        CacheManager cacheManager = Mockito.mock(CacheManager.class);
        Path cacheRoot = Mockito.mock(Path.class);
        Config config = Mockito.mock(Config.class);
        setup(config, cacheManager, cacheRoot);
        PluginService service = getService(config, cacheManager);
        Plugin plugin = Plugin.build("valid-plugin").withConfig(config);
        plugin.withLocal(true);
        plugin.withLocalRepository(Path.of(".").toAbsolutePath());
        String result = service.extractRepoName(plugin);
        assertEquals("plugin-modernizer-core", result);
    }

    @Test
    public void shouldExtractRepoNameWithGitSuffix() throws Exception {
        CacheManager cacheManager = Mockito.mock(CacheManager.class);
        Path cacheRoot = Mockito.mock(Path.class);
        Config config = Mockito.mock(Config.class);
        UpdateCenterData updateCenterData =
                setup(config, cacheManager, cacheRoot).getLeft();
        setupUpdateCenterMocks(updateCenterData, cacheManager, cacheRoot);
        PluginService service = getService(config, cacheManager);
        String result = service.extractRepoName(Plugin.build("valid-plugin-2").withConfig(config));
        assertEquals("valid-git-repo", result);
    }

    @Test
    public void shouldDownloadPluginVersionDataUpdateCenterData() throws Exception {

        WireMockServer server = new WireMockServer(40465);
        server.start();
        WireMock wireMock = new WireMock(40465);

        CacheManager cacheManager = Mockito.mock(CacheManager.class);
        Path cacheRoot = Mockito.mock(Path.class);
        Config config = Mockito.mock(Config.class);
        UpdateCenterData updateCenterData =
                setup(config, cacheManager, cacheRoot).getLeft();

        wireMock.register(WireMock.get(WireMock.urlEqualTo("/update-center.json"))
                .willReturn(WireMock.okJson(JsonUtils.toJson(updateCenterData))));

        // No found from cache
        doReturn(new URL("http://localhost:40465/update-center.json"))
                .when(config)
                .getJenkinsUpdateCenter();

        // Get result
        PluginService service = getService(config, cacheManager);
        UpdateCenterData result = service.downloadUpdateCenterData();
        assertEquals(result.getPlugins().size(), updateCenterData.getPlugins().size());
    }

    @Test
    public void shouldThrowExceptionIfNotFound() throws Exception {

        CacheManager cacheManager = Mockito.mock(CacheManager.class);
        Path cacheRoot = Mockito.mock(Path.class);
        Config config = Mockito.mock(Config.class);
        UpdateCenterData updateCenterData =
                setup(config, cacheManager, cacheRoot).getLeft();

        setupUpdateCenterMocks(updateCenterData, cacheManager, cacheRoot);

        PluginService service = getService(config, cacheManager);
        Exception exception = assertThrows(ModernizerException.class, () -> {
            service.extractRepoName(Plugin.build("not-present").withConfig(config));
        });
        assertEquals("Plugin not found in update center", exception.getMessage());
    }

    @Test
    public void shouldFailIfSCMFormatIsInvalid() throws Exception {

        CacheManager cacheManager = Mockito.mock(CacheManager.class);
        Path cacheRoot = Mockito.mock(Path.class);
        Config config = Mockito.mock(Config.class);
        UpdateCenterData updateCenterData =
                setup(config, cacheManager, cacheRoot).getLeft();

        setupUpdateCenterMocks(updateCenterData, cacheManager, cacheRoot);

        PluginService service = getService(config, cacheManager);
        Exception exception = assertThrows(ModernizerException.class, () -> {
            service.extractRepoName(Plugin.build("invalid-plugin").withConfig(config));
        });
        assertEquals("Invalid SCM URL format", exception.getMessage());
    }

    @Test
    public void shouldDownloadPluginVersionDataPluginHealthScore() throws Exception {

        WireMockServer server = new WireMockServer(40466);
        server.start();
        WireMock wireMock = new WireMock(40466);

        CacheManager cacheManager = Mockito.mock(CacheManager.class);
        Path cacheRoot = Mockito.mock(Path.class);
        Config config = Mockito.mock(Config.class);
        HealthScoreData healthScoreData = setup(config, cacheManager, cacheRoot).getMiddle();

        wireMock.register(WireMock.get(WireMock.urlEqualTo("/api/scores"))
                .willReturn(WireMock.okJson(JsonUtils.toJson(healthScoreData))));

        // No found from cache
        doReturn(new URL("http://localhost:40466/api/scores")).when(config).getPluginHealthScore();

        // Get result
        PluginService service = getService(config, cacheManager);
        HealthScoreData result = service.downloadHealthScoreData();
        assertEquals(result.getPlugins().size(), healthScoreData.getPlugins().size());
    }

    @Test
    public void shouldDownloadPluginInstallationsData() throws Exception {

        WireMockServer server = new WireMockServer(40467);
        server.start();
        WireMock wireMock = new WireMock(40467);

        CacheManager cacheManager = Mockito.mock(CacheManager.class);
        Path cacheRoot = Mockito.mock(Path.class);
        Config config = Mockito.mock(Config.class);
        Triple<UpdateCenterData, HealthScoreData, PluginInstallationStatsData> apis =
                setup(config, cacheManager, cacheRoot);
        PluginInstallationStatsData pluginInstallationStatsData = apis.getRight();

        wireMock.register(
                WireMock.get(WireMock.urlEqualTo("/api/scores")).willReturn(WireMock.okJson(JsonUtils.toJson("{}"))));
        wireMock.register(WireMock.get(WireMock.urlEqualTo("/jenkins-stats/svg/202406-plugins.csv"))
                .willReturn(WireMock.ok("\"valid-plugin\",\"1\"\n" + "\"valid-plugin2\",\"1\"")));

        URL url = new URL("http://localhost:40467/jenkins-stats/svg/202406-plugins.csv");

        // No found from cache
        doReturn(url).when(config).getPluginStatsInstallations();

        // Get result
        PluginService service = getService(config, cacheManager);
        PluginInstallationStatsData result = service.downloadInstallationStatsData();
        assertEquals(
                result.getPlugins().size(),
                pluginInstallationStatsData.getPlugins().size());
    }

    /**
     * Get the update center service to test
     * @param cacheManager Cache manager
     * @return Update center service
     * @throws Exception If an error occurs
     */
    private PluginService getService(Config config, CacheManager cacheManager) throws Exception {
        PluginService service = Guice.createInjector(new GuiceModule(config)).getInstance(PluginService.class);
        Field field = ReflectionUtils.findFields(
                        PluginService.class,
                        f -> f.getName().equals("cacheManager"),
                        ReflectionUtils.HierarchyTraversalMode.TOP_DOWN)
                .get(0);
        field.setAccessible(true);
        field.set(service, cacheManager);
        return service;
    }

    private void setupUpdateCenterMocks(UpdateCenterData updateCenterData, CacheManager cacheManager, Path cacheRoot) {
        doReturn(updateCenterData)
                .when(cacheManager)
                .get(cacheRoot, CacheManager.UPDATE_CENTER_CACHE_KEY, UpdateCenterData.class);
        doReturn(cacheRoot).when(cacheManager).root();
    }

    private void setupHealthScoreMocks(HealthScoreData healthScoreData, CacheManager cacheManager, Path cacheRoot) {
        doReturn(healthScoreData)
                .when(cacheManager)
                .get(cacheRoot, CacheManager.HEALTH_SCORE_KEY, HealthScoreData.class);
        doReturn(cacheRoot).when(cacheManager).root();
    }

    private Triple<UpdateCenterData, HealthScoreData, PluginInstallationStatsData> setup(
            Config config, CacheManager cacheManager, Path cacheRoot) throws Exception {

        UpdateCenterData updateCenterData = new UpdateCenterData(cacheManager);
        HealthScoreData healthScoreData = new HealthScoreData(cacheManager);
        PluginInstallationStatsData pluginInstallationStatsData = new PluginInstallationStatsData(cacheManager);

        Map<String, UpdateCenterData.UpdateCenterPlugin> updateCenterPlugins = new HashMap<>();
        updateCenterPlugins.put(
                "valid-plugin",
                new UpdateCenterData.UpdateCenterPlugin(
                        "valid-plugin", "1.0", "https://github.com/jenkinsci/valid-url", "main", "gav", null));
        updateCenterPlugins.put(
                "valid-plugin-2",
                new UpdateCenterData.UpdateCenterPlugin(
                        "valid-plugin", "1.0", "git@github.com/jenkinsci/valid-git-repo.git", "main", "gav", null));
        updateCenterPlugins.put(
                "invalid-plugin",
                new UpdateCenterData.UpdateCenterPlugin(
                        "invalid-plugin", "1.0", "invalid-scm-url", "main", "gav", null));
        updateCenterPlugins.put(
                "invalid-plugin-2",
                new UpdateCenterData.UpdateCenterPlugin("invalid-plugin-2", "1.0", "/", "main", "gav", null));

        // Add health plugin
        Map<String, HealthScoreData.HealthScorePlugin> healthPlugins = new HashMap<>();
        healthPlugins.put("valid-plugin", new HealthScoreData.HealthScorePlugin(100d));
        healthPlugins.put("valid-plugin2", new HealthScoreData.HealthScorePlugin(50d));

        // Add installations
        Map<String, Integer> installations = new HashMap<>();
        installations.put("valid-plugin", 1000);
        installations.put("valid-plugin2", 500);

        // Set plugins
        Field updateCenterPluginField = ReflectionUtils.findFields(
                        UpdateCenterData.class,
                        f -> f.getName().equals("plugins"),
                        ReflectionUtils.HierarchyTraversalMode.TOP_DOWN)
                .get(0);
        updateCenterPluginField.setAccessible(true);
        updateCenterPluginField.set(updateCenterData, updateCenterPlugins);

        doReturn(cacheRoot).when(config).getCachePath();

        Field healthScorePluginField = ReflectionUtils.findFields(
                        HealthScoreData.class,
                        f -> f.getName().equals("plugins"),
                        ReflectionUtils.HierarchyTraversalMode.TOP_DOWN)
                .get(0);
        healthScorePluginField.setAccessible(true);
        healthScorePluginField.set(healthScoreData, healthPlugins);

        Field pluginInstallationDataField = ReflectionUtils.findFields(
                        PluginInstallationStatsData.class,
                        f -> f.getName().equals("plugins"),
                        ReflectionUtils.HierarchyTraversalMode.TOP_DOWN)
                .get(0);
        pluginInstallationDataField.setAccessible(true);
        pluginInstallationDataField.set(pluginInstallationStatsData, healthPlugins);

        return Triple.of(updateCenterData, healthScoreData, pluginInstallationStatsData);
    }
}
