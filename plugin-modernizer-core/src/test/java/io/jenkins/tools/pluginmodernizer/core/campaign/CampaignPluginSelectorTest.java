package io.jenkins.tools.pluginmodernizer.core.campaign;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.jenkins.tools.pluginmodernizer.core.model.ModernizerException;
import io.jenkins.tools.pluginmodernizer.core.model.Plugin;
import io.jenkins.tools.pluginmodernizer.core.model.PluginInstallationStatsData;
import io.jenkins.tools.pluginmodernizer.core.utils.PluginService;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CampaignPluginSelectorTest {

    @TempDir
    private Path tempDir;

    @Test
    void shouldThrowWhenAllPluginsFilteredOut() {
        PluginInstallationStatsData statsData = new PluginInstallationStatsData(null);
        statsData.setPlugins(Map.of("tiny-plugin", 50));
        FakePluginService pluginService = new FakePluginService(
                statsData,
                Map.of("tiny-plugin", 50),
                Map.of("tiny-plugin", 30d),
                Set.of(),
                Set.of(),
                Set.of(),
                Set.of());

        CampaignDefinition definition = new CampaignDefinition();
        CampaignPluginSource pluginSource = new CampaignPluginSource();
        pluginSource.setNames(List.of("tiny-plugin"));
        CampaignFilters filters = new CampaignFilters();
        filters.setMinInstallations(10_000);
        pluginSource.setFilters(filters);
        definition.setPlugins(pluginSource);

        CampaignPluginSelector selector = new CampaignPluginSelector(pluginService);
        assertThrows(ModernizerException.class,
                () -> selector.selectPlugins(definition, tempDir.resolve("campaign.yaml")));
    }

    @Test
    void shouldResolveNamesFilesLocalPathsAndTopPluginsWithFilters() throws Exception {
        PluginInstallationStatsData statsData = new PluginInstallationStatsData(null);
        statsData.setPlugins(Map.of("healthy-plugin", 2_000, "deprecated-plugin", 1_500, "unhealthy-plugin", 1_000));
        FakePluginService pluginService = new FakePluginService(
                statsData,
                Map.of(
                        "healthy-plugin", 2_000,
                        "deprecated-plugin", 1_500,
                        "explicit-plugin", 1_200,
                        "file-plugin", 1_200),
                Map.of(
                        "healthy-plugin", 60d,
                        "deprecated-plugin", 50d,
                        "explicit-plugin", 70d,
                        "file-plugin", 70d),
                Set.of("deprecated-plugin"),
                Set.of(),
                Set.of(),
                Set.of());

        Path pluginFile = tempDir.resolve("plugins.txt");
        Files.writeString(pluginFile, "file-plugin\n");

        Path localPlugin = tempDir.resolve("local-plugin");
        Files.createDirectories(localPlugin);
        Files.writeString(
                localPlugin.resolve("pom.xml"),
                """
                <project>
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>test</groupId>
                  <artifactId>local-plugin</artifactId>
                  <version>1.0</version>
                  <packaging>hpi</packaging>
                </project>
                """);

        CampaignDefinition definition = new CampaignDefinition();
        CampaignPluginSource pluginSource = new CampaignPluginSource();
        pluginSource.setNames(List.of("explicit-plugin"));
        pluginSource.setFile(pluginFile.getFileName().toString());
        pluginSource.setLocalPaths(List.of(localPlugin.getFileName().toString()));
        pluginSource.setTopPlugins(2);

        CampaignFilters filters = new CampaignFilters();
        filters.setMinInstallations(1_000);
        filters.setMaxHealthScore(80d);
        filters.setExcludeDeprecated(true);
        pluginSource.setFilters(filters);
        definition.setPlugins(pluginSource);

        CampaignPluginSelector selector = new CampaignPluginSelector(pluginService);
        List<Plugin> selected = selector.selectPlugins(definition, tempDir.resolve("campaign.yaml"));

        assertEquals(4, selected.size());
        assertTrue(selected.stream().anyMatch(plugin -> plugin.getName().equals("explicit-plugin")));
        assertTrue(selected.stream().anyMatch(plugin -> plugin.getName().equals("file-plugin")));
        assertTrue(selected.stream().anyMatch(plugin -> plugin.getName().equals("healthy-plugin")));
        assertTrue(selected.stream().anyMatch(plugin -> plugin.getName().equals("local-plugin") && plugin.isLocal()));
    }

    private static final class FakePluginService extends PluginService {

        private final PluginInstallationStatsData installationStatsData;
        private final Map<String, Integer> installations;
        private final Map<String, Double> scores;
        private final Set<String> deprecatedPlugins;
        private final Set<String> apiPlugins;
        private final Set<String> adoptablePlugins;
        private final Set<String> noInstallationsPlugins;

        private FakePluginService(
                PluginInstallationStatsData installationStatsData,
                Map<String, Integer> installations,
                Map<String, Double> scores,
                Set<String> deprecatedPlugins,
                Set<String> apiPlugins,
                Set<String> adoptablePlugins,
                Set<String> noInstallationsPlugins) {
            this.installationStatsData = installationStatsData;
            this.installations = installations;
            this.scores = scores;
            this.deprecatedPlugins = deprecatedPlugins;
            this.apiPlugins = apiPlugins;
            this.adoptablePlugins = adoptablePlugins;
            this.noInstallationsPlugins = noInstallationsPlugins;
        }

        @Override
        public PluginInstallationStatsData getPluginInstallationStatsData() {
            return installationStatsData;
        }

        @Override
        public Integer extractInstallationStats(Plugin plugin) {
            return installations.get(plugin.getName());
        }

        @Override
        public Double extractScore(Plugin plugin) {
            return scores.get(plugin.getName());
        }

        @Override
        public boolean isDeprecated(Plugin plugin) {
            return deprecatedPlugins.contains(plugin.getName());
        }

        @Override
        public boolean isApiPlugin(Plugin plugin) {
            return apiPlugins.contains(plugin.getName());
        }

        @Override
        public boolean isForAdoption(Plugin plugin) {
            return adoptablePlugins.contains(plugin.getName());
        }

        @Override
        public boolean hasNoKnownInstallations(Plugin plugin) {
            return noInstallationsPlugins.contains(plugin.getName());
        }
    }
}
