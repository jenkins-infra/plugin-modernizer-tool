package io.jenkins.tools.pluginmodernizer.core.utils;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.jenkins.tools.pluginmodernizer.core.config.Config;
import io.jenkins.tools.pluginmodernizer.core.config.Settings;
import io.jenkins.tools.pluginmodernizer.core.impl.CacheManager;
import io.jenkins.tools.pluginmodernizer.core.model.HealthScoreData;
import io.jenkins.tools.pluginmodernizer.core.model.ModernizerException;
import io.jenkins.tools.pluginmodernizer.core.model.OptOutPluginsData;
import io.jenkins.tools.pluginmodernizer.core.model.Plugin;
import io.jenkins.tools.pluginmodernizer.core.model.PluginInstallationStatsData;
import io.jenkins.tools.pluginmodernizer.core.model.PluginVersionData;
import io.jenkins.tools.pluginmodernizer.core.model.UpdateCenterData;
import jakarta.inject.Inject;
import java.nio.file.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for Jenkins plugin center
 */
public class PluginService {

    private static final Logger LOG = LoggerFactory.getLogger(PluginService.class);

    @Inject
    private Config config;

    @Inject
    private CacheManager cacheManager;

    /**
     * Extract the repository name for a plugin
     * @param plugin Plugin
     * @return Repository name
     */
    @SuppressFBWarnings("NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE")
    public String extractRepoName(Plugin plugin) {

        if (plugin.isLocal() && plugin.getLocalRepository() != null) {
            StaticPomParser parser = new StaticPomParser(
                    plugin.getLocalRepository().resolve("pom.xml").toString());
            String githubRepo = parser.getGithubRepoProperty();
            // Use SCM connection property and fallback to folder which might not be accurate
            if (githubRepo == null || githubRepo.isEmpty()) {

                String scmConnection = parser.getScmConnectionProperty();
                if (scmConnection != null && !scmConnection.isEmpty()) {
                    String localRepo = scmConnection.substring(scmConnection.lastIndexOf("/") + 1);
                    return localRepo.replaceAll(".git$", "");
                }

                Path localRepo = plugin.getLocalRepository().getFileName();
                // Take the name of the parent
                if (localRepo.toString().equals(".")) {
                    localRepo = localRepo.toAbsolutePath().getParent().getFileName();
                    LOG.debug("Parent folder is the repo name: {}", localRepo);
                }
                LOG.debug(
                        "No GitHub repo found in POM file for plugin. Assuming current folder is the repo name: {}",
                        localRepo);
                return localRepo.toString();
            }
            return githubRepo.replaceAll(Settings.ORGANIZATION + "/", "");
        }
        UpdateCenterData updateCenterData = getUpdateCenterData();
        UpdateCenterData.UpdateCenterPlugin updateCenterPlugin =
                updateCenterData.getPlugins().get(plugin.getName());
        if (updateCenterPlugin == null) {
            plugin.addError("Plugin not found in update center");
            plugin.raiseLastError();
            return null;
        }
        String scmUrl = updateCenterPlugin.scm();
        int lastSlashIndex = scmUrl.lastIndexOf('/');
        if (lastSlashIndex != -1 && lastSlashIndex < scmUrl.length() - 1) {
            return scmUrl.substring(lastSlashIndex + 1).replaceAll(".git$", "");
        } else {
            plugin.addError("Invalid SCM URL format");
            plugin.raiseLastError();
        }
        throw new ModernizerException("Invalid SCM URL format: " + scmUrl);
    }

    /**
     * Check if a plugin exists in the update center
     * @param plugin Plugin
     * @return True if plugin exists in the update center
     */
    public boolean existsInUpdateCenter(Plugin plugin) {
        UpdateCenterData updateCenterData = getUpdateCenterData();
        return updateCenterData.getPlugins().containsKey(plugin.getName());
    }

    /**
     * Check if a plugin is deprecated
     * @param plugin Plugin
     * @return True if deprecated
     */
    public boolean isDeprecated(Plugin plugin) {
        // Some old plugin are under a deprecations list
        UpdateCenterData updateCenterData = getUpdateCenterData();
        if (updateCenterData.getDeprecations().containsKey(plugin.getName())) {
            return true;
        }
        // More recent deprecated plugins are marked with a label
        UpdateCenterData.UpdateCenterPlugin updateCenterPlugin =
                updateCenterData.getPlugins().get(plugin.getName());
        return updateCenterPlugin != null
                && updateCenterPlugin.labels() != null
                && updateCenterPlugin.labels().contains("deprecated");
    }

    /**
     * Retgurn if a plugin is for adoption
     * @param plugin Plugin
     * @return True if for adoption
     */
    public boolean isForAdoption(Plugin plugin) {
        UpdateCenterData updateCenterData = getUpdateCenterData();
        UpdateCenterData.UpdateCenterPlugin updateCenterPlugin =
                updateCenterData.getPlugins().get(plugin.getName());
        return updateCenterPlugin != null
                && updateCenterPlugin.labels() != null
                && updateCenterPlugin.labels().contains("adopt-this-plugin");
    }

    /**
     * Check if a plugin is an API plugin
     * @param plugin Plugin
     * @return True if API plugin
     */
    public boolean isApiPlugin(Plugin plugin) {
        UpdateCenterData updateCenterData = getUpdateCenterData();
        UpdateCenterData.UpdateCenterPlugin updateCenterPlugin =
                updateCenterData.getPlugins().get(plugin.getName());

        // Let's consider only recent convention that API plugins have a labels and end with -api
        return updateCenterPlugin != null
                && updateCenterPlugin.labels() != null
                && updateCenterPlugin.labels().contains("api-plugin")
                && plugin.getName().endsWith("-api");
    }

    /**
     * Extract the version for a plugin in the update center
     * @param plugin Plugin
     * @return Version
     */
    public String extractVersion(Plugin plugin) {
        if (plugin.isLocal()) {
            return null;
        }
        UpdateCenterData updateCenterData = getUpdateCenterData();
        UpdateCenterData.UpdateCenterPlugin updateCenterPlugin =
                updateCenterData.getPlugins().get(plugin.getName());
        if (updateCenterPlugin == null) {
            plugin.addError("Plugin not found in update center");
            plugin.raiseLastError();
            return null;
        }
        return updateCenterPlugin.version();
    }

    /**
     * Retrieve update center data from the given URL or from cache if it exists
     * @return Update center data
     */
    public UpdateCenterData getUpdateCenterData() {
        UpdateCenterData updateCenterData =
                cacheManager.get(cacheManager.root(), CacheManager.UPDATE_CENTER_CACHE_KEY, UpdateCenterData.class);
        // Download and update cache
        if (updateCenterData == null) {
            updateCenterData = downloadUpdateCenterData();
            updateCenterData.setKey(CacheManager.UPDATE_CENTER_CACHE_KEY);
            updateCenterData.setPath(cacheManager.root());
            cacheManager.put(updateCenterData);
        }
        return updateCenterData;
    }

    /**
     * Retrieve health score data from the given URL or from cache if it exists
     * @return Health score data
     */
    public HealthScoreData getHealthScoreData() {
        HealthScoreData healthScoreData =
                cacheManager.get(cacheManager.root(), CacheManager.HEALTH_SCORE_KEY, HealthScoreData.class);
        // Download and update cache
        if (healthScoreData == null) {
            healthScoreData = downloadHealthScoreData();
            healthScoreData.setKey(CacheManager.HEALTH_SCORE_KEY);
            healthScoreData.setPath(cacheManager.root());
            cacheManager.put(healthScoreData);
        }
        return healthScoreData;
    }

    /**
     * Retrieve opt out plugins data from the given URL or from cache if it exists
     * @return Opt out plugins data
     */
    public OptOutPluginsData getOptOutPluginsData() {
        OptOutPluginsData optOutPluginsData =
                cacheManager.get(cacheManager.root(), CacheManager.OPT_OUT_PLUGINS_CACHE_KEY, OptOutPluginsData.class);
        // Download and update cache
        if (optOutPluginsData == null) {
            optOutPluginsData = downloadOptOutPluginsData();
            optOutPluginsData.setKey(CacheManager.OPT_OUT_PLUGINS_CACHE_KEY);
            optOutPluginsData.setPath(cacheManager.root());
            cacheManager.put(optOutPluginsData);
        }
        return optOutPluginsData;
    }

    /**
     * Download refreshed update center data from the remote service
     * @return Update center data
     */
    public UpdateCenterData downloadUpdateCenterData() {
        return JsonUtils.fromUrl(config.getJenkinsUpdateCenter(), UpdateCenterData.class);
    }

    /**
     * Download refreshed health score data from the remote service
     * @return Health score data
     */
    public HealthScoreData downloadHealthScoreData() {
        return JsonUtils.fromUrl(config.getPluginHealthScore(), HealthScoreData.class);
    }

    /**
     * Download refreshed opt out plugins data from the metadata repository
     * @return Opt out plugins data
     */
    public OptOutPluginsData downloadOptOutPluginsData() {
        return JsonUtils.fromUrl(config.getOptOutPlugins(), OptOutPluginsData.class);
    }

    /**
     * Retrieve installation stats data from the given URL
     */
    public PluginInstallationStatsData downloadInstallationStatsData() {
        String data = CSVUtils.fromUrl(config.getPluginStatsInstallations());
        PluginInstallationStatsData pluginInstallationStatsData = new PluginInstallationStatsData(cacheManager);
        pluginInstallationStatsData.setPlugins(CSVUtils.parseStats(data));
        return pluginInstallationStatsData;
    }

    /**
     * Extract the installation stats for a plugin
     * @param plugin Plugin
     * @return Installation stats
     */
    public Integer extractInstallationStats(Plugin plugin) {
        PluginInstallationStatsData pluginInstallationStatsData = getPluginInstallationStatsData();
        return pluginInstallationStatsData.getPlugins().get(plugin.getName());
    }

    /**
     * Extract the score for a plugin. Null if not found
     * @param plugin Plugin
     * @return Score
     */
    public Double extractScore(Plugin plugin) {
        if (plugin.isLocal()) {
            return null;
        }
        HealthScoreData healthScoreData = getHealthScoreData();
        HealthScoreData.HealthScorePlugin healthScorePlugin =
                healthScoreData.getPlugins().get(plugin.getName());
        if (healthScorePlugin == null) {
            return null;
        }
        return healthScorePlugin.value();
    }

    /**
     * Check if a plugin has a max score (100 %)
     * @param plugin Plugin
     * @return True if max score
     */
    public boolean hasMaxScore(Plugin plugin) {
        Double score = extractScore(plugin);
        return score != null && score.equals(100.0);
    }

    /**
     * Check if a plugin has a low score
     * @param plugin Plugin
     * @return True if low score
     */
    public boolean hasLowScore(Plugin plugin) {
        Double score = extractScore(plugin);
        return score != null && score < Settings.PLUGIN_LOW_SCORE_THRESHOLD;
    }

    /**
     * Check if a plugin has no known installations
     * @param plugin Plugin
     * @return True if no known installation
     */
    public boolean hasNoKnownInstallations(Plugin plugin) {
        Integer installations = extractInstallationStats(plugin);
        return installations == null || installations == 0;
    }

    /**
     * Retrieve plugin version data from the given URL or from cache if it exists
     * @return Plugin version data
     */
    public PluginVersionData getPluginVersionData() {
        PluginVersionData pluginVersionData =
                cacheManager.get(cacheManager.root(), CacheManager.PLUGIN_VERSIONS_CACHE_KEY, PluginVersionData.class);
        // Download and update cache
        if (pluginVersionData == null) {
            pluginVersionData = downloadPluginVersionData(config);
            pluginVersionData.setKey(CacheManager.PLUGIN_VERSIONS_CACHE_KEY);
            pluginVersionData.setPath(cacheManager.root());
            cacheManager.put(pluginVersionData);
        }
        return pluginVersionData;
    }

    /**
     * Retrieve plugin installation stats data from the given URL or from cache if it exists
     * @return Plugin installation stats data
     */
    public PluginInstallationStatsData getPluginInstallationStatsData() {
        PluginInstallationStatsData pluginInstallationStatsData = cacheManager.get(
                cacheManager.root(), CacheManager.INSTALLATION_STATS_KEY, PluginInstallationStatsData.class);
        // Download and update cache
        if (pluginInstallationStatsData == null) {
            pluginInstallationStatsData = downloadInstallationStatsData();
            pluginInstallationStatsData.setKey(CacheManager.INSTALLATION_STATS_KEY);
            pluginInstallationStatsData.setPath(cacheManager.root());
            cacheManager.put(pluginInstallationStatsData);
        }
        return pluginInstallationStatsData;
    }

    /**
     * Download refreshed update center data from the remote service
     * @param config Configuration
     * @return Update center data
     */
    public PluginVersionData downloadPluginVersionData(Config config) {
        return JsonUtils.fromUrl(config.getJenkinsPluginVersions(), PluginVersionData.class);
    }
}
