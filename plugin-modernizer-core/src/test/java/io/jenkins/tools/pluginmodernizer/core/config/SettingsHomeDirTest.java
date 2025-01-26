package io.jenkins.tools.pluginmodernizer.core.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import io.jenkins.tools.pluginmodernizer.core.utils.Utils;
import java.nio.file.Paths;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import uk.org.webcompere.systemstubs.jupiter.SystemStub;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;
import uk.org.webcompere.systemstubs.properties.SystemProperties;

@ExtendWith(SystemStubsExtension.class)
@Execution(ExecutionMode.CONCURRENT)
public class SettingsHomeDirTest {

    @SystemStub
    private SystemProperties properties = new SystemProperties("user.home", "/home/foobar");

    @BeforeEach
    public void beforeEach() {
        assumeTrue(
                System.getenv("RUN_IN_MAVEN") != null && !Utils.runningInIde(),
                "Skipping test from IDE: not running via Maven due to static mocking of env vars");
    }

    @Test
    public void test() throws Exception {
        assertEquals(Paths.get("/home/foobar/.cache/jenkins-plugin-modernizer-cli"), Settings.DEFAULT_CACHE_PATH);
    }
}
