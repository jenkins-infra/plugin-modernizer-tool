package io.jenkins.tools.pluginmodernizer.cli;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.jenkins.tools.pluginmodernizer.core.config.Config;
import org.jboss.logmanager.ExtLogRecord;
import org.jboss.logmanager.Level;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MarkerFactory;

public class ConsoleLogFilterTest {

    private final ConsoleLogFilter consoleLogFilter = new ConsoleLogFilter();

    @AfterEach
    public void tearDown() {
        Config.setDebug(false);
    }

    @Test
    public void testInfoIsLoggable() {
        Config.setDebug(false);
        assertTrue(consoleLogFilter.isLoggable(record(Level.INFO)));
    }

    @Test
    public void testDebugIsNotLoggableByDefault() {
        Config.setDebug(false);
        assertFalse(consoleLogFilter.isLoggable(record(Level.DEBUG)));
    }

    @Test
    public void testDebugIsLoggableWhenDebugEnabled() {
        Config.setDebug(true);
        assertTrue(consoleLogFilter.isLoggable(record(Level.DEBUG)));
        assertFalse(consoleLogFilter.isLoggable(record(Level.TRACE)));
    }

    @Test
    public void testRecordWithMarkerIsNotLoggable() {
        ExtLogRecord record = record(Level.ERROR);
        record.setMarker(MarkerFactory.getMarker("my-plugin"));
        assertFalse(consoleLogFilter.isLoggable(record));
    }

    private static ExtLogRecord record(java.util.logging.Level level) {
        return new ExtLogRecord(level, "message", ConsoleLogFilterTest.class.getName());
    }
}
