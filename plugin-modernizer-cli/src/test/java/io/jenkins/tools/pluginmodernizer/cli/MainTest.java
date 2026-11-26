package io.jenkins.tools.pluginmodernizer.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.quarkus.test.junit.main.Launch;
import io.quarkus.test.junit.main.LaunchResult;
import io.quarkus.test.junit.main.QuarkusMainTest;
import org.junit.jupiter.api.Test;

/**
 * Launch the Quarkus application in-process
 */
@QuarkusMainTest
public class MainTest {

    @Test
    @Launch("--version")
    public void testVersion(LaunchResult result) {
        assertTrue(
                result.getOutput().matches("(?s)plugin modernizer [a-zA-Z0-9.\\-_]+ \\(.*\\).*"), result.getOutput());
    }

    @Test
    @Launch({"version", "--short"})
    public void testShortVersion(LaunchResult result) {
        assertTrue(result.getOutput().strip().matches("[a-zA-Z0-9.\\-_]+"), result.getOutput());
    }

    @Test
    @Launch("--help")
    public void testHelp(LaunchResult result) {
        assertTrue(result.getOutput().contains("Usage:"), result.getOutput());
        assertTrue(result.getOutput().contains("dry-run"), result.getOutput());
    }

    @Test
    @Launch(value = "unknown-command", exitCode = 2)
    public void testUnknownCommand(LaunchResult result) {
        assertEquals(2, result.exitCode());
        assertTrue(result.getErrorOutput().contains("Unmatched argument"), result.getErrorOutput());
    }
}
