package io.jenkins.tools.pluginmodernizer.core.utils;

import io.jenkins.tools.pluginmodernizer.core.config.SettingsEnvTest;

public class Utils {

    /**
     * Return if this class is running in IDE
     * @return True if running in IDE
     */
    public static boolean runningInIde() {
        try {
            return SettingsEnvTest.class.getClassLoader().loadClass("com.intellij.rt.execution.application.AppMainV2")
                    != null;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}
