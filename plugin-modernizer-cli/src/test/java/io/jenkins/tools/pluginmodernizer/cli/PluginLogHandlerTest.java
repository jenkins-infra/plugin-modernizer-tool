package io.jenkins.tools.pluginmodernizer.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Duration;
import java.time.Instant;
import org.jboss.logmanager.ExtLogRecord;
import org.jboss.logmanager.Level;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class PluginLogHandlerTest {

    @TempDir
    private Path logs;

    @Test
    public void testLeastRecentlyUsedFileIsClosed() throws Exception {
        PluginLogHandler handler =
                new PluginLogHandler(record -> logs.resolve(record.getMessage()).toString(), 2);
        handler.publish(record("a.log"));
        handler.publish(record("b.log"));
        handler.publish(record("a.log"));
        handler.publish(record("c.log"));
        assertEquals(2, handler.openFiles());

        // A closed file is reopened in append mode
        handler.publish(record("b.log"));
        handler.close();
        assertEquals(2, Files.readAllLines(logs.resolve("a.log")).size());
        assertEquals(2, Files.readAllLines(logs.resolve("b.log")).size());
        assertEquals(1, Files.readAllLines(logs.resolve("c.log")).size());
    }

    @Test
    public void testExpiredArchivesAreDeleted() throws Exception {
        Path expired = logs.resolve("invoker.logs.2026-01-01.1.gz");
        Path recent = logs.resolve("invoker.logs.2026-01-07.1.gz");
        Path otherFile = logs.resolve("other.logs.2026-01-01.1.gz");
        for (Path archive : new Path[] {expired, recent, otherFile}) {
            Files.writeString(archive, "archive");
        }
        FileTime old = FileTime.from(Instant.now().minus(Duration.ofDays(8)));
        Files.setLastModifiedTime(expired, old);
        Files.setLastModifiedTime(otherFile, old);

        PluginLogHandler handler =
                new PluginLogHandler(record -> logs.resolve("invoker.logs").toString(), 2);
        handler.publish(record("message"));
        handler.close();

        assertFalse(Files.exists(expired));
        assertTrue(Files.exists(recent));
        assertTrue(Files.exists(otherFile));
    }

    private static ExtLogRecord record(String message) {
        return new ExtLogRecord(Level.INFO, message, PluginLogHandlerTest.class.getName());
    }
}
