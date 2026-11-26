package io.jenkins.tools.pluginmodernizer.cli;

import io.jenkins.tools.pluginmodernizer.core.config.Config;
import io.quarkus.logging.LoggingFilter;
import java.util.logging.Filter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import org.jboss.logmanager.ExtLogRecord;

/**
 * Records with a plugin marker only go to the plugin log file. Records below INFO are shown only with --debug.
 */
@LoggingFilter(name = "console-filter")
public class ConsoleLogFilter implements Filter {

    @Override
    public boolean isLoggable(LogRecord record) {
        if (record instanceof ExtLogRecord extLogRecord && extLogRecord.getMarker() != null) {
            return false;
        }
        Level threshold = Config.isDebug() ? org.jboss.logmanager.Level.DEBUG : org.jboss.logmanager.Level.INFO;
        return record.getLevel().intValue() >= threshold.intValue();
    }
}
