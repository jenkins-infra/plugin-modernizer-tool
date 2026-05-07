package io.jenkins.tools.pluginmodernizer.core.utils;

import io.jenkins.tools.pluginmodernizer.core.config.SettingsEnvTest;
import java.nio.file.Paths;
import org.openrewrite.ExecutionContext;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.maven.MavenExecutionContextView;
import org.openrewrite.maven.MavenSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Utils {

    /**
     * Logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(Utils.class);

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

    /**
     * Get the maven settings location from the environment variable MAVEN_SETTINGS
     * @return The maven settings location set by CI
     */
    public static ExecutionContext getMavenExecutionContext() {
        ExecutionContext context = new InMemoryExecutionContext();
        MavenExecutionContextView view = MavenExecutionContextView.view(context);
        String settings = System.getenv("MAVEN_SETTINGS");
        if (settings != null) {
            view.setMavenSettings(MavenSettings.parse(Paths.get(settings), context));
            LOG.info("Using maven settings from MAVEN_SETTINGS environment variable: {}", settings);
            return view;
        } else {
            LOG.info("No MAVEN_SETTINGS environment variable found, using default maven settings");
            return view;
        }
    }
}
