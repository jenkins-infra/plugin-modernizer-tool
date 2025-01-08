package io.jenkins.tools.pluginmodernizer.cli.options;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import io.jenkins.tools.pluginmodernizer.core.config.Config;
import io.jenkins.tools.pluginmodernizer.core.config.Settings;
import java.lang.reflect.Field;
import org.junit.jupiter.api.Test;
import org.junit.platform.commons.util.ReflectionUtils;

public class GlobalOptionsTest {

    @Test
    public void testGlobalOptionsWithoutDefault() {
        Config.Builder builder = Config.builder();
        GlobalOptions globalOptions = new GlobalOptions();
        globalOptions.config(builder);

        // Check defaults
        Config config = builder.build();
        assertFalse(config.isDebug(), "Debug should be false by default");
        assertEquals(Settings.DEFAULT_CACHE_PATH, config.getCachePath(), "Cache path should be the default");
        assertEquals(Settings.DEFAULT_MAVEN_HOME, config.getMavenHome(), "Maven home should be the default");
        assertEquals(
                Settings.DEFAULT_MAVEN_LOCAL_REPO,
                config.getMavenLocalRepo(),
                "Maven local repo should be the default");
    }

    @Test
    public void testGlobalOptionsWithCustom() throws Exception {
        Config.Builder builder = Config.builder();
        GlobalOptions globalOptions = new GlobalOptions();

        // Set debug
        Field debugField = ReflectionUtils.findFields(
                        GlobalOptions.class,
                        f -> f.getName().equals("debug"),
                        ReflectionUtils.HierarchyTraversalMode.TOP_DOWN)
                .get(0);
        debugField.setAccessible(true);
        debugField.set(globalOptions, true);

        // Set cache path
        Field cachePathField = ReflectionUtils.findFields(
                        GlobalOptions.class,
                        f -> f.getName().equals("cachePath"),
                        ReflectionUtils.HierarchyTraversalMode.TOP_DOWN)
                .get(0);
        cachePathField.setAccessible(true);
        cachePathField.set(globalOptions, Settings.DEFAULT_CACHE_PATH.resolve("custom-cache"));

        globalOptions.config(builder);

        // Check custom values
        Config config = builder.build();
        assertEquals(true, config.isDebug(), "Debug should be true");
        assertEquals(
                Settings.DEFAULT_CACHE_PATH.resolve("custom-cache").resolve("jenkins-plugin-modernizer-cli"),
                config.getCachePath(),
                "Cache path should be custom");
    }
}
