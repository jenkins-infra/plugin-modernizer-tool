package io.jenkins.tools.pluginmodernizer.cli.options;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.jenkins.tools.pluginmodernizer.core.config.Config;
import io.jenkins.tools.pluginmodernizer.core.config.Settings;
import java.lang.reflect.Field;
import java.net.URI;
import org.junit.jupiter.api.Test;
import org.junit.platform.commons.util.ReflectionUtils;

public class EnvOptionsTest {

    @Test
    public void testEnvOptionsWithoutDefault() {
        Config.Builder builder = Config.builder();
        EnvOptions envOptions = new EnvOptions();
        envOptions.config(builder);

        // Check defaults
        Config config = builder.build();
        assertEquals(
                Settings.DEFAULT_UPDATE_CENTER_URL,
                config.getJenkinsUpdateCenter(),
                "Jenkins update center should be the default");
        assertEquals(
                Settings.DEFAULT_PLUGIN_VERSIONS,
                config.getJenkinsPluginVersions(),
                "Jenkins plugin versions should be the default");
        assertEquals(
                Settings.DEFAULT_HEALTH_SCORE_URL,
                config.getPluginHealthScore(),
                "Plugin health score should be the default");
        assertEquals(
                Settings.DEFAULT_PLUGINS_STATS_INSTALLATIONS_URL,
                config.getPluginStatsInstallations(),
                "Jenkins stats top plugins URL should be the default");
        assertEquals(Settings.GITHUB_API_URL, config.getGithubApiUrl(), "GitHub API URL should be the default");
    }

    @Test
    public void testWithConfig() throws Exception {
        Config.Builder builder = Config.builder();
        EnvOptions envOptions = new EnvOptions();

        // Set update center URL
        Field updateCenterField = ReflectionUtils.findFields(
                        EnvOptions.class,
                        f -> f.getName().equals("jenkinsUpdateCenter"),
                        ReflectionUtils.HierarchyTraversalMode.TOP_DOWN)
                .get(0);
        updateCenterField.setAccessible(true);
        updateCenterField.set(
                envOptions,
                URI.create("http://localhost:8080/update-center.json").toURL());

        // Set plugin health URL
        Field pluginHealthField = ReflectionUtils.findFields(
                        EnvOptions.class,
                        f -> f.getName().equals("pluginHealthScore"),
                        ReflectionUtils.HierarchyTraversalMode.TOP_DOWN)
                .get(0);
        pluginHealthField.setAccessible(true);
        pluginHealthField.set(
                envOptions,
                URI.create("http://localhost:8080/plugin-health.json").toURL());

        // Set plugin stats URL
        Field pluginStatsField = ReflectionUtils.findFields(
                        EnvOptions.class,
                        f -> f.getName().equals("jenkinsPluginsStatsInstallationsUrl"),
                        ReflectionUtils.HierarchyTraversalMode.TOP_DOWN)
                .get(0);
        pluginStatsField.setAccessible(true);
        pluginStatsField.set(
                envOptions,
                URI.create("http://localhost:8080/plugin-stats.json").toURL());

        // Set github API URL
        Field githubApiField = ReflectionUtils.findFields(
                        EnvOptions.class,
                        f -> f.getName().equals("githubApiUrl"),
                        ReflectionUtils.HierarchyTraversalMode.TOP_DOWN)
                .get(0);
        githubApiField.setAccessible(true);
        githubApiField.set(
                envOptions, URI.create("http://localhost:8080/github-api").toURL());

        // Assertions
        envOptions.config(builder);
        Config config = builder.build();
        assertEquals(
                URI.create("http://localhost:8080/update-center.json").toURL(),
                config.getJenkinsUpdateCenter(),
                "Different update center URL");
        assertEquals(
                URI.create("http://localhost:8080/plugin-health.json").toURL(),
                config.getPluginHealthScore(),
                "Different plugin health URL");
        assertEquals(
                URI.create("http://localhost:8080/plugin-stats.json").toURL(),
                config.getPluginStatsInstallations(),
                "Different plugin stats URL");
        assertEquals(
                URI.create("http://localhost:8080/github-api").toURL(),
                config.getGithubApiUrl(),
                "Different GitHub API URL");
    }
}
