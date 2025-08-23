package io.jenkins.tools.pluginmodernizer.core.model;

import java.util.Arrays;

public enum OptOutPlugins {
    // List of plugins that have opted out from the modernization by plugin-modernizer-tool
    SAFE_BATCH_ENVIRONMENT_FILTER("safe-batch-environment-filter"),
    ENVIRONMENT_FILTER_UTILS("environment-filter-utils"),
    GENERIC_ENVIRONMENT_FILTERS("generic-environment-filters");

    private final String pluginName;

    OptOutPlugins(String pluginName) {
        this.pluginName = pluginName;
    }

    public String getPluginName() {
        return pluginName;
    }

    public static boolean isPluginOptedOut(String pluginName) {
        return Arrays.stream(values()).anyMatch(optOut -> optOut.getPluginName().equals(pluginName));
    }
}
