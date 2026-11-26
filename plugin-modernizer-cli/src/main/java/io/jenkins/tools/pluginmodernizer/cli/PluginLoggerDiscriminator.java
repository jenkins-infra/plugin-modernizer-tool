package io.jenkins.tools.pluginmodernizer.cli;

import io.jenkins.tools.pluginmodernizer.cli.options.GlobalOptions;
import io.jenkins.tools.pluginmodernizer.core.config.Config;
import io.jenkins.tools.pluginmodernizer.core.model.Plugin;
import java.nio.file.Path;
import java.util.logging.LogRecord;
import org.jboss.logmanager.ExtLogRecord;
import org.slf4j.Marker;

/**
 * Records with a plugin marker go to the plugin log file, others to the global modernizer log file.
 */
public class PluginLoggerDiscriminator {

    public String getDiscriminatingValue(LogRecord record) {
        Object marker = record instanceof ExtLogRecord extLogRecord ? extLogRecord.getMarker() : null;

        Config.Builder builder = Config.builder();
        GlobalOptions.getInstance().config(builder);
        String cachePath = builder.build().getCachePath().toString();

        if (!(marker instanceof Marker pluginMarker)) {
            return Path.of(cachePath, "modernizer.logs").toString();
        }
        Plugin plugin = Plugin.build(pluginMarker.getName());
        return Path.of(cachePath, plugin.getLogFile().toString()).toString();
    }
}
