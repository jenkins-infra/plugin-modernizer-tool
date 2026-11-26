package io.jenkins.tools.pluginmodernizer.cli;

import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Singleton;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.ErrorManager;
import java.util.logging.Logger;
import org.jboss.logmanager.ExtHandler;
import org.jboss.logmanager.ExtLogRecord;
import org.jboss.logmanager.formatters.PatternFormatter;
import org.jboss.logmanager.handlers.PeriodicSizeRotatingFileHandler;

@Singleton
public class PluginLogHandler extends ExtHandler {

    private static final String PATTERN = "%z{UTC}%d{yyyy-MM-dd'T'HH:mm:ss.SSS'Z'} [%p] [Thread=%t] - %c{1.} # %s%e%n";
    private static final long MAX_FILE_SIZE = 5L * 1024 * 1024;
    private static final int MAX_BACKUPS = 7;

    private final PluginLoggerDiscriminator discriminator = new PluginLoggerDiscriminator();
    private final Map<String, PeriodicSizeRotatingFileHandler> handlers = new ConcurrentHashMap<>();

    void onStart(@Observes StartupEvent event) {
        Logger.getLogger("").addHandler(this);
    }

    void onStop(@Observes ShutdownEvent event) {
        Logger.getLogger("").removeHandler(this);
        close();
    }

    @Override
    protected void doPublish(ExtLogRecord record) {
        PeriodicSizeRotatingFileHandler handler =
                handlers.computeIfAbsent(discriminator.getDiscriminatingValue(record), this::createHandler);
        if (handler != null) {
            handler.publish(record);
        }
    }

    @Override
    public void flush() {
        handlers.values().forEach(PeriodicSizeRotatingFileHandler::flush);
    }

    @Override
    public void close() {
        handlers.values().forEach(PeriodicSizeRotatingFileHandler::close);
        handlers.clear();
    }

    // Null when the file cannot be opened so its records are dropped instead of failing the caller
    private PeriodicSizeRotatingFileHandler createHandler(String file) {
        try {
            Path path = Path.of(file);
            if (path.getParent() != null) {
                Files.createDirectories(path.getParent());
            }
            PeriodicSizeRotatingFileHandler handler = new PeriodicSizeRotatingFileHandler(
                    path.toFile(), ".yyyy-MM-dd.gz", MAX_FILE_SIZE, MAX_BACKUPS, true);
            handler.setFormatter(new PatternFormatter(PATTERN));
            handler.setAutoFlush(true);
            return handler;
        } catch (IOException e) {
            reportError("Unable to create log file " + file, e, ErrorManager.OPEN_FAILURE);
            return null;
        }
    }
}
