package io.jenkins.tools.pluginmodernizer.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.quarkus.test.junit.main.Launch;
import io.quarkus.test.junit.main.LaunchResult;
import io.quarkus.test.junit.main.QuarkusMainTest;
import org.junit.jupiter.api.Test;

@QuarkusMainTest
public class MainTest {

    @Test
    @Launch("--version")
    public void testVersion(LaunchResult result) {
        assertTrue(hasLine(result, "plugin modernizer [a-zA-Z0-9.\\-_]+ \\(.*\\)"), result.getOutput());
    }

    @Test
    @Launch({"version", "--short"})
    public void testShortVersion(LaunchResult result) {
        assertTrue(hasLine(result, "[0-9][a-zA-Z0-9.\\-_]*"), result.getOutput());
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

    // The integration test launcher enables Quarkus startup logs, so other lines can surround the CLI output
    private static boolean hasLine(LaunchResult result, String regex) {
        return result.getOutputStream().stream().anyMatch(line -> line.strip().matches(regex));
    }
}
