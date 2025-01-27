package io.jenkins.tools.pluginmodernizer.core.config;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import io.jenkins.tools.pluginmodernizer.core.model.Recipe;
import io.jenkins.tools.pluginmodernizer.core.utils.Utils;
import java.nio.file.Paths;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;
import uk.org.webcompere.systemstubs.jupiter.SystemStub;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;

@ExtendWith(SystemStubsExtension.class)
@Execution(ExecutionMode.CONCURRENT)
public class SettingsEnvTest {

    @SystemStub
    private EnvironmentVariables envVars = new EnvironmentVariables()
            .set("CACHE_DIR", ".my-cache")
            .remove("GH_TOKEN")
            .set("GITHUB_TOKEN", "fake-token")
            .set("GH_OWNER", "fake-org")
            .set("JENKINS_UC", "https://example-com")
            .remove("MAVEN_HOME")
            .set("M2_HOME", "/opt/maven-test");

    @BeforeEach
    public void beforeEach() {
        assumeTrue(
                System.getenv("RUN_IN_MAVEN") != null && !Utils.runningInIde(),
                "Skipping test from IDE: not running via Maven due to static mocking of env vars");
    }

    @Test
    public void testCustomCache() throws Exception {
        assertEquals(Paths.get(".my-cache", Settings.CACHE_SUBDIR), Settings.DEFAULT_CACHE_PATH);
    }

    @Test
    public void testCustomM2Home() throws Exception {
        assertEquals(Paths.get("/opt/maven-test"), Settings.DEFAULT_MAVEN_HOME);
    }

    @Test
    public void testGithubToken() throws Exception {
        assertEquals("fake-token", Settings.GITHUB_TOKEN);
    }

    @Test
    public void testUpdateCenter() throws Exception {
        assertEquals("https://example-com", Settings.DEFAULT_UPDATE_CENTER_URL.toString());
    }

    @Test
    public void testGithubOwner() throws Exception {
        assertEquals("fake-org", Settings.GITHUB_OWNER);
    }

    @Test
    public void ensureAllRecipesHaveAttributes() {
        for (Recipe recipe : Settings.AVAILABLE_RECIPES) {
            assertNotNull(recipe.getName(), "Recipe name is null");
            assertNotNull(recipe.getDisplayName(), "Recipe display name is null for " + recipe.getName());
            assertNotNull(recipe.getDescription(), "Recipe description is null for " + recipe.getName());
            assertNotNull(recipe.getTags(), "Recipe tags are null for " + recipe.getName());
            assertFalse(recipe.getTags().isEmpty(), "Recipe tags are empty for " + recipe.getName());
        }
    }
}
