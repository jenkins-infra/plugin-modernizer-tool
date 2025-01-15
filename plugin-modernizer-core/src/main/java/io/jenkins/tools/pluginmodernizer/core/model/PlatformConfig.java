package io.jenkins.tools.pluginmodernizer.core.model;

import java.util.List;

/**
 * Extracted platform from Jenkinsfile
 */
public record PlatformConfig(Platform name, JDK jdk, String jenkins, boolean implicit) {

    /**
     * Get the default platform configurations
     * @return the default platform configurations
     */
    public static List<PlatformConfig> getDefaults() {
        // https://raw.githubusercontent.com/jenkinsci/archetypes/refs/heads/master/common-files/Jenkinsfile
        return List.of(
                new PlatformConfig(Platform.LINUX, JDK.JAVA_21, null, false),
                new PlatformConfig(Platform.WINDOWS, JDK.JAVA_17, null, false));
    }

    /**
     * Build a platform configuration without jenkins version
     * @param platform the platform
     * @param jdk the JDK
     * @return the platform configuration
     */
    public static PlatformConfig build(Platform platform, JDK jdk) {
        return new PlatformConfig(platform, jdk, null, false);
    }

    /**
     * Build a platform configuration with jenkins version
     * @param platform the platform
     * @param jdk the JDK
     * @param jenkins the jenkins version
     * @return the platform configuration
     */
    public static PlatformConfig build(Platform platform, JDK jdk, String jenkins) {
        return new PlatformConfig(platform, jdk, jenkins, false);
    }
}
;
