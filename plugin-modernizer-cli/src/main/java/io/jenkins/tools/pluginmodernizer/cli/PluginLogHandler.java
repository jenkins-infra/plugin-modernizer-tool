package io.jenkins.tools.pluginmodernizer.cli;

import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.logging.ErrorManager;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.stream.Stream;
import org.jboss.logmanager.ExtHandler;
import org.jboss.logmanager.ExtLogRecord;
import org.jboss.logmanager.formatters.PatternFormatter;
import org.jboss.logmanager.handlers.PeriodicSizeRotatingFileHandler;

@Singleton
public class PluginLogHandler extends ExtHandler {

    private static final String PATTERN = "%z{UTC}%d{yyyy-MM-dd'T'HH:mm:ss.SSS'Z'} [%p] [Thread=%t] - %c{1.} # %s%e%n";
    private static final long MAX_FILE_SIZE = 5L * 1024 * 1024;
    private static final int MAX_BACKUPS = 7;
    private static final Duration MAX_HISTORY = Duration.ofDays(7);
    private static final int MAX_OPEN_FILES = 16;

    private final Function<LogRecord, String> fileResolver;
    private final Map<String, PeriodicSizeRotatingFileHandler> handlers;

    @Inject
    public PluginLogHandler() {
        this(new PluginLoggerDiscriminator()::getDiscriminatingValue, MAX_OPEN_FILES);
    }

    // Plugins are processed one after the other, so only recently used log files are kept open
    PluginLogHandler(Function<LogRecord, String> fileResolver, int maxOpenFiles) {
        this.fileResolver = fileResolver;
        this.handlers = new LinkedHashMap<>(maxOpenFiles, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, PeriodicSizeRotatingFileHandler> eldest) {
                if (size() > maxOpenFiles) {
                    eldest.getValue().close();
                    return true;
                }
                return false;
            }
        };
    }

    void onStart(@Observes StartupEvent event) {
        Logger.getLogger("").addHandler(this);
    }

    void onStop(@Observes ShutdownEvent event) {
        Logger.getLogger("").removeHandler(this);
        close();
    }

    @Override
    protected synchronized void doPublish(ExtLogRecord record) {
        String file = fileResolver.apply(record);
        PeriodicSizeRotatingFileHandler handler = handlers.get(file);
        if (handler == null) {
            handler = createHandler(file);
            if (handler == null) {
                return;
            }
            handlers.put(file, handler);
        }
        handler.publish(record);
    }

    @Override
    public synchronized void flush() {
        handlers.values().forEach(PeriodicSizeRotatingFileHandler::flush);
    }

    @Override
    public synchronized void close() {
        handlers.values().forEach(PeriodicSizeRotatingFileHandler::close);
        handlers.clear();
    }

    synchronized int openFiles() {
        return handlers.size();
    }

    // Null when the file cannot be opened so its records are dropped instead of failing the caller
    private PeriodicSizeRotatingFileHandler createHandler(String file) {
        try {
            Path path = Path.of(file);
            Path directory = path.getParent();
            Path fileName = path.getFileName();
            if (directory != null && fileName != null) {
                Files.createDirectories(directory);
                deleteExpiredArchives(directory, fileName.toString());
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

    // The size based rotation only bounds the archives of the current day
    private static void deleteExpiredArchives(Path directory, String fileName) throws IOException {
        String prefix = fileName + ".";
        Instant expiry = Instant.now().minus(MAX_HISTORY);
        try (Stream<Path> siblings = Files.list(directory)) {
            for (Path archive : siblings.toList()) {
                String name = String.valueOf(archive.getFileName());
                if (name.startsWith(prefix)
                        && name.endsWith(".gz")
                        && Files.getLastModifiedTime(archive).toInstant().isBefore(expiry)) {
                    Files.deleteIfExists(archive);
                }
            }
        }
    }
}
