package io.jenkins.tools.pluginmodernizer.core.campaign;

import io.jenkins.tools.pluginmodernizer.core.model.ModernizerException;
import io.jenkins.tools.pluginmodernizer.core.model.Plugin;
import io.jenkins.tools.pluginmodernizer.core.utils.PluginPathResolver;
import io.jenkins.tools.pluginmodernizer.core.utils.PluginService;
import jakarta.inject.Inject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Resolves campaign plugin sources to concrete plugins and applies remote filters.
 */
public class CampaignPluginSelector {

    private final PluginService pluginService;
    private final PluginPathResolver pluginPathResolver = new PluginPathResolver();

    @Inject
    public CampaignPluginSelector(PluginService pluginService) {
        this.pluginService = pluginService;
    }

    public List<Plugin> selectPlugins(CampaignDefinition definition, Path campaignFile) {
        CampaignPluginSource source = definition.getPlugins();
        Path baseDir = campaignFile.toAbsolutePath().normalize().getParent();
        LinkedHashMap<String, Plugin> plugins = new LinkedHashMap<>();

        addNamedPlugins(plugins, source.getNames());
        addPluginFile(plugins, source.getFile(), baseDir);
        addLocalPaths(plugins, source.getLocalPaths(), baseDir);
        addTopPlugins(plugins, source.getTopPlugins());

        List<Plugin> selectedPlugins = plugins.values().stream()
                .filter(plugin -> keepPlugin(plugin, source.getFilters()))
                .toList();

        if (selectedPlugins.isEmpty()) {
            throw new ModernizerException("Campaign selection returned no plugins after applying filters");
        }
        return selectedPlugins;
    }

    private void addNamedPlugins(Map<String, Plugin> plugins, List<String> names) {
        if (names == null) {
            return;
        }
        for (String name : names) {
            if (name != null && !name.isBlank()) {
                Plugin plugin = Plugin.build(name.trim());
                plugins.putIfAbsent(key(plugin), plugin);
            }
        }
    }

    private void addPluginFile(Map<String, Plugin> plugins, String pluginFile, Path baseDir) {
        if (pluginFile == null || pluginFile.isBlank()) {
            return;
        }
        Path file = resolveRelative(baseDir, pluginFile);
        try (Stream<String> lines = Files.lines(file)) {
            lines.filter(line -> !line.trim().isEmpty())
                    .map(line -> line.split(":")[0].trim())
                    .filter(line -> !line.isEmpty())
                    .map(Plugin::build)
                    .forEach(plugin -> plugins.putIfAbsent(key(plugin), plugin));
        } catch (IOException e) {
            throw new ModernizerException("Failed to read campaign plugin file: " + file, e);
        }
    }

    private void addLocalPaths(Map<String, Plugin> plugins, List<String> localPaths, Path baseDir) {
        if (localPaths == null) {
            return;
        }
        for (String localPath : localPaths) {
            if (localPath == null || localPath.isBlank()) {
                continue;
            }
            Path path = resolveRelative(baseDir, localPath);
            try {
                Plugin plugin = pluginPathResolver.resolve(path);
                plugins.putIfAbsent(key(plugin), plugin);
            } catch (IOException e) {
                throw new ModernizerException("Failed to resolve local campaign plugin path: " + path, e);
            }
        }
    }

    private void addTopPlugins(Map<String, Plugin> plugins, Integer topPlugins) {
        if (topPlugins == null) {
            return;
        }
        pluginService.getPluginInstallationStatsData().getPlugins().entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue(Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(topPlugins)
                .map(Map.Entry::getKey)
                .map(Plugin::build)
                .forEach(plugin -> plugins.putIfAbsent(key(plugin), plugin));
    }

    private boolean keepPlugin(Plugin plugin, CampaignFilters filters) {
        if (filters == null || plugin.isLocal()) {
            return true;
        }
        if (filters.isExcludeDeprecated() && pluginService.isDeprecated(plugin)) {
            return false;
        }
        if (filters.isExcludeApiPlugins() && pluginService.isApiPlugin(plugin)) {
            return false;
        }
        if (filters.isRequireAdoptThisPlugin() && !pluginService.isForAdoption(plugin)) {
            return false;
        }
        if (filters.isExcludeNoKnownInstallations() && pluginService.hasNoKnownInstallations(plugin)) {
            return false;
        }
        if (filters.getMinInstallations() != null) {
            Integer installations = pluginService.extractInstallationStats(plugin);
            if (installations == null || installations < filters.getMinInstallations()) {
                return false;
            }
        }
        if (filters.getMaxHealthScore() != null) {
            Double score = pluginService.extractScore(plugin);
            if (score == null || score > filters.getMaxHealthScore()) {
                return false;
            }
        }
        return true;
    }

    private Path resolveRelative(Path baseDir, String value) {
        Path path = Path.of(value);
        if (path.isAbsolute()) {
            return path.normalize();
        }
        return baseDir.resolve(path).normalize();
    }

    private String key(Plugin plugin) {
        if (plugin.isLocal()) {
            return "local:" + plugin.getLocalRepository().toAbsolutePath().normalize();
        }
        return "remote:" + plugin.getName();
    }
}
