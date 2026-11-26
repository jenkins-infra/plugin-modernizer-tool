package io.jenkins.tools.pluginmodernizer.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.jenkins.tools.pluginmodernizer.cli.options.GlobalOptions;
import java.nio.file.Path;
import java.util.logging.LogRecord;
import org.jboss.logmanager.ExtLogRecord;
import org.jboss.logmanager.Level;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MarkerFactory;

public class PluginLoggerDiscriminatorTest {

    private static final Path CACHE_PATH =
            Path.of(System.getProperty("user.home"), ".cache", "jenkins-plugin-modernizer-cli");

    private final PluginLoggerDiscriminator discriminator = new PluginLoggerDiscriminator();

    @BeforeEach
    void resetSingleton() {
        GlobalOptions.reset();
    }

    @Test
    void testGetDiscriminatingValueJulRecord() {
        LogRecord record = new LogRecord(Level.INFO, "message");
        assertEquals(CACHE_PATH.resolve("modernizer.logs").toString(), discriminator.getDiscriminatingValue(record));
    }

    @Test
    void testGetDiscriminatingValueNoMarker() {
        ExtLogRecord record = new ExtLogRecord(Level.INFO, "message", getClass().getName());
        assertEquals(CACHE_PATH.resolve("modernizer.logs").toString(), discriminator.getDiscriminatingValue(record));
    }

    @Test
    void testGetDiscriminatingValueWithMarker() {
        ExtLogRecord record = new ExtLogRecord(Level.INFO, "message", getClass().getName());
        record.setMarker(MarkerFactory.getMarker("testMarker"));
        assertEquals(
                CACHE_PATH
                        .resolve("testMarker")
                        .resolve("logs")
                        .resolve("invoker.logs")
                        .toString(),
                discriminator.getDiscriminatingValue(record));
    }
}
