package io.jenkins.tools.pluginmodernizer.core.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import io.jenkins.tools.pluginmodernizer.core.extractor.PluginMetadata;
import io.jenkins.tools.pluginmodernizer.core.model.Plugin;
import io.jenkins.tools.pluginmodernizer.core.model.Recipe;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

@Execution(ExecutionMode.CONCURRENT)
public class TemplateUtilsTest {

    @Test
    public void testDefaultPrTitle() {

        // Mocks
        Plugin plugin = mock(Plugin.class);
        PluginMetadata metadata = mock(PluginMetadata.class);
        Recipe recipe = mock(Recipe.class);

        doReturn(metadata).when(plugin).getMetadata();
        doReturn("io.jenkins.tools.pluginmodernizer.FakeRecipe").when(recipe).getName();

        // Test
        String result = TemplateUtils.renderPullRequestTitle(plugin, recipe);

        // Assert
        assertEquals("Applied recipe FakeRecipe", result);
    }

    @Test
    public void testDefaultCommit() {

        // Mocks
        Plugin plugin = mock(Plugin.class);
        PluginMetadata metadata = mock(PluginMetadata.class);
        Recipe recipe = mock(Recipe.class);

        doReturn(metadata).when(plugin).getMetadata();
        doReturn("io.jenkins.tools.pluginmodernizer.FakeRecipe").when(recipe).getName();

        // Test
        String result = TemplateUtils.renderCommitMessage(plugin, recipe);

        // Assert
        assertEquals("Applied recipe FakeRecipe", result);
    }

    @Test
    public void testDefaultBranchName() {

        // Mocks
        Plugin plugin = mock(Plugin.class);
        PluginMetadata metadata = mock(PluginMetadata.class);
        Recipe recipe = mock(Recipe.class);

        doReturn(metadata).when(plugin).getMetadata();
        doReturn("io.jenkins.tools.pluginmodernizer.FakeRecipe").when(recipe).getName();

        // Test
        String result = TemplateUtils.renderBranchName(plugin, recipe);

        // Assert
        assertEquals("plugin-modernizer/fakerecipe", result);
    }

    @Test
    public void testFriendlyPrTitleUpgradeBomVersion() {

        // Mocks
        Plugin plugin = mock(Plugin.class);
        PluginMetadata metadata = mock(PluginMetadata.class);
        Recipe recipe = mock(Recipe.class);

        doReturn(metadata).when(plugin).getMetadata();
        doReturn("3208.vb_21177d4b_cd9").when(metadata).getBomVersion();
        doReturn("io.jenkins.tools.pluginmodernizer.UpgradeBomVersion")
                .when(recipe)
                .getName();

        // Test
        String result = TemplateUtils.renderPullRequestTitle(plugin, recipe);

        // Assert
        assertEquals("Bump bom to 3208.vb_21177d4b_cd9", result);
    }

    @Test
    public void testFriendlyCommitUpgradeBomVersion() {

        // Mocks
        Plugin plugin = mock(Plugin.class);
        PluginMetadata metadata = mock(PluginMetadata.class);
        Recipe recipe = mock(Recipe.class);

        doReturn(metadata).when(plugin).getMetadata();
        doReturn("3208.vb_21177d4b_cd9").when(metadata).getBomVersion();
        doReturn("io.jenkins.tools.pluginmodernizer.UpgradeBomVersion")
                .when(recipe)
                .getName();

        // Test
        String result = TemplateUtils.renderCommitMessage(plugin, recipe);

        // Assert
        assertEquals("Bump bom to 3208.vb_21177d4b_cd9", result);
    }

    @Test
    public void testFriendlyPrBodyUpgradeBomVersion() {

        // Mocks
        Plugin plugin = mock(Plugin.class);
        PluginMetadata metadata = mock(PluginMetadata.class);
        Recipe recipe = mock(Recipe.class);

        doReturn(metadata).when(plugin).getMetadata();
        doReturn("3208.vb_21177d4b_cd9").when(metadata).getBomVersion();
        doReturn("io.jenkins.tools.pluginmodernizer.UpgradeBomVersion")
                .when(recipe)
                .getName();

        // Test
        String result = TemplateUtils.renderPullRequestBody(plugin, recipe);

        // Assert
        assertTrue(result.contains("Why is this important?"), "Missing 'Why is this important?' section");
    }

    @Test
    public void testFriendlyPrTitleUpgradeParentVersion() {

        // Mocks
        Plugin plugin = mock(Plugin.class);
        PluginMetadata metadata = mock(PluginMetadata.class);
        Recipe recipe = mock(Recipe.class);

        doReturn(metadata).when(plugin).getMetadata();
        doReturn("4.88").when(metadata).getParentVersion();
        doReturn("io.jenkins.tools.pluginmodernizer.UpgradeParentVersion")
                .when(recipe)
                .getName();

        // Test
        String result = TemplateUtils.renderPullRequestTitle(plugin, recipe);

        // Assert
        assertEquals("Bump parent pom to 4.88", result);
    }

    @Test
    public void testFriendlyPrBodyUpgradeParentVersion() {

        // Mocks
        Plugin plugin = mock(Plugin.class);
        PluginMetadata metadata = mock(PluginMetadata.class);
        Recipe recipe = mock(Recipe.class);

        doReturn(metadata).when(plugin).getMetadata();
        doReturn("4.88").when(metadata).getParentVersion();
        doReturn("io.jenkins.tools.pluginmodernizer.UpgradeParentVersion")
                .when(recipe)
                .getName();

        // Test
        String result = TemplateUtils.renderPullRequestBody(plugin, recipe);

        // Assert
        assertTrue(result.contains("Why is this important?"), "Missing 'Why is this important?' section");
        assertTrue(
                result.contains("https://www.jenkins.io/doc/developer/tutorial-improve/update-parent-pom/"),
                "Missing or invalid link");
    }

    @Test
    public void testFriendlyPrTitleUpdateScmUrl() {

        // Mocks
        Plugin plugin = mock(Plugin.class);
        PluginMetadata metadata = mock(PluginMetadata.class);
        Recipe recipe = mock(Recipe.class);

        doReturn(metadata).when(plugin).getMetadata();
        doReturn("2.452.4").when(metadata).getJenkinsVersion();
        doReturn("io.jenkins.tools.pluginmodernizer.UpdateScmUrl").when(recipe).getName();

        // Test
        String result = TemplateUtils.renderPullRequestTitle(plugin, recipe);

        // Assert
        assertEquals("Updates SCM URLs in POM files from git:// to https:// protocol.", result);
    }

    @Test
    public void testFriendlyPrBodyUpdateScmUrl() {

        // Mocks
        Plugin plugin = mock(Plugin.class);
        PluginMetadata metadata = mock(PluginMetadata.class);
        Recipe recipe = mock(Recipe.class);

        doReturn(metadata).when(plugin).getMetadata();
        doReturn("2.452.4").when(metadata).getJenkinsVersion();
        doReturn("io.jenkins.tools.pluginmodernizer.UpdateScmUrl").when(recipe).getName();

        // Test
        String result = TemplateUtils.renderPullRequestBody(plugin, recipe);

        // Assert
        assertTrue(result.contains("Why is this important?"), "Missing 'Why is this important?' section");
        assertTrue(
                result.contains("https://www.jenkins.io/doc/developer/tutorial-improve/update-scm-url/"),
                "Missing or invalid link");
    }

    @Test
    public void testFriendlyPrTitleUpgradeToRecommendCoreVersion() {

        // Mocks
        Plugin plugin = mock(Plugin.class);
        PluginMetadata metadata = mock(PluginMetadata.class);
        Recipe recipe = mock(Recipe.class);

        doReturn(metadata).when(plugin).getMetadata();
        doReturn("2.452.4").when(metadata).getJenkinsVersion();
        doReturn("io.jenkins.tools.pluginmodernizer.UpgradeToRecommendCoreVersion")
                .when(recipe)
                .getName();

        // Test
        String result = TemplateUtils.renderPullRequestTitle(plugin, recipe);

        // Assert
        assertEquals("Require 2.452.4", result);
    }

    @Test
    public void testFriendlyPrTitleUpgradeToLatestJava11CoreVersion() {

        // Mocks
        Plugin plugin = mock(Plugin.class);
        PluginMetadata metadata = mock(PluginMetadata.class);
        Recipe recipe = mock(Recipe.class);

        doReturn(metadata).when(plugin).getMetadata();
        doReturn("2.462.3").when(metadata).getJenkinsVersion();
        doReturn("io.jenkins.tools.pluginmodernizer.UpgradeToLatestJava11CoreVersion")
                .when(recipe)
                .getName();

        // Test
        String result = TemplateUtils.renderPullRequestTitle(plugin, recipe);

        // Assert
        assertEquals("feat: upgrade to Jenkins LTS Core 2.462.3 for Java 11 support", result);
    }

    @Test
    public void testFriendlyPrTitleUpgradeToLatestJava8CoreVersion() {

        // Mocks
        Plugin plugin = mock(Plugin.class);
        PluginMetadata metadata = mock(PluginMetadata.class);
        Recipe recipe = mock(Recipe.class);

        doReturn(metadata).when(plugin).getMetadata();
        doReturn("2.346.3").when(metadata).getJenkinsVersion();
        doReturn("io.jenkins.tools.pluginmodernizer.UpgradeToLatestJava8CoreVersion")
                .when(recipe)
                .getName();

        // Test
        String result = TemplateUtils.renderPullRequestTitle(plugin, recipe);

        // Assert
        assertEquals("Require 2.346.3", result);
    }

    @Test
    public void testFriendlyPrTitleUpgradeNextMajorParentVersion() {

        // Mocks
        Plugin plugin = mock(Plugin.class);
        PluginMetadata metadata = mock(PluginMetadata.class);
        Recipe recipe = mock(Recipe.class);

        doReturn(metadata).when(plugin).getMetadata();
        doReturn("2.479.1").when(metadata).getJenkinsVersion();
        doReturn("io.jenkins.tools.pluginmodernizer.UpgradeNextMajorParentVersion")
                .when(recipe)
                .getName();

        // Test
        String result = TemplateUtils.renderPullRequestTitle(plugin, recipe);

        // Assert
        assertEquals("Require 2.479.1 and Java 17", result);
    }

    @Test
    public void testFriendlyPrTitleMigrateToJenkinsBaseLineProperty() {

        // Mocks
        Plugin plugin = mock(Plugin.class);
        PluginMetadata metadata = mock(PluginMetadata.class);
        Recipe recipe = mock(Recipe.class);

        doReturn(metadata).when(plugin).getMetadata();
        doReturn("2.479.1").when(metadata).getJenkinsVersion();
        doReturn("io.jenkins.tools.pluginmodernizer.MigrateToJenkinsBaseLineProperty")
                .when(recipe)
                .getName();

        // Test
        String result = TemplateUtils.renderPullRequestTitle(plugin, recipe);

        // Assert
        assertEquals(
                "Update pom.xml to match archetype and use `jenkins.baseline` property to keep bom in sync", result);
    }

    @Test
    public void testFriendlyPrBodyMigrateToJenkinsBaseLineProperty() {

        // Mocks
        Plugin plugin = mock(Plugin.class);
        PluginMetadata metadata = mock(PluginMetadata.class);
        Recipe recipe = mock(Recipe.class);

        doReturn(metadata).when(plugin).getMetadata();
        doReturn("2.479.1").when(metadata).getJenkinsVersion();
        doReturn("io.jenkins.tools.pluginmodernizer.MigrateToJenkinsBaseLineProperty")
                .when(recipe)
                .getName();

        // Test
        String result = TemplateUtils.renderPullRequestBody(plugin, recipe);

        // Just ensure it's using some key overall text
        assertTrue(result.contains("Why is this important?"), "Missing 'Why is this important?' section");
        assertTrue(result.contains("https://github.com/jenkinsci/archetypes/pull/737"), "Missing or invalid link");
    }

    @Test
    public void testFriendlyPrTitleSetupDependabot() {

        // Mocks
        Plugin plugin = mock(Plugin.class);
        PluginMetadata metadata = mock(PluginMetadata.class);
        Recipe recipe = mock(Recipe.class);

        doReturn(metadata).when(plugin).getMetadata();
        doReturn("io.jenkins.tools.pluginmodernizer.SetupDependabot")
                .when(recipe)
                .getName();

        // Test
        String result = TemplateUtils.renderPullRequestTitle(plugin, recipe);

        // Assert
        assertEquals("Automate dependency updates with Dependabot", result);
    }

    @Test
    public void testFriendlyPrTitleRemoveReleaseDrafter() {

        // Mocks
        Plugin plugin = mock(Plugin.class);
        PluginMetadata metadata = mock(PluginMetadata.class);
        Recipe recipe = mock(Recipe.class);

        doReturn(metadata).when(plugin).getMetadata();
        doReturn("io.jenkins.tools.pluginmodernizer.RemoveReleaseDrafter")
                .when(recipe)
                .getName();

        // Test
        String result = TemplateUtils.renderPullRequestTitle(plugin, recipe);

        // Assert
        assertEquals("Remove release drafter due to enabled cd", result);
    }

    @Test
    public void testFriendlyPrBodyRemoveReleaseDrafter() {

        // Mocks
        Plugin plugin = mock(Plugin.class);
        PluginMetadata metadata = mock(PluginMetadata.class);
        Recipe recipe = mock(Recipe.class);

        doReturn(metadata).when(plugin).getMetadata();
        doReturn("io.jenkins.tools.pluginmodernizer.RemoveReleaseDrafter")
                .when(recipe)
                .getName();

        // Test
        String result = TemplateUtils.renderPullRequestBody(plugin, recipe);

        // Assert
        assertTrue(result.contains("Why is this important?"), "Missing 'Why is this important?' section");
        assertTrue(
                result.contains(
                        "https://www.jenkins.io/doc/developer/publishing/releasing-cd/#configure-release-drafter"),
                "Missing or invalid link");
    }

    @Test
    public void testFriendlyPrTitleEnsureRelativePath() {

        // Mocks
        Plugin plugin = mock(Plugin.class);
        PluginMetadata metadata = mock(PluginMetadata.class);
        Recipe recipe = mock(Recipe.class);

        doReturn(metadata).when(plugin).getMetadata();
        doReturn("io.jenkins.tools.pluginmodernizer.EnsureRelativePath")
                .when(recipe)
                .getName();

        // Test
        String result = TemplateUtils.renderPullRequestTitle(plugin, recipe);

        // Assert
        assertEquals("Disable local resolution of parent pom", result);
    }

    @Test
    public void testFriendlyPrTitleSetupGitIgnore() {

        // Mocks
        Plugin plugin = mock(Plugin.class);
        PluginMetadata metadata = mock(PluginMetadata.class);
        Recipe recipe = mock(Recipe.class);

        doReturn(metadata).when(plugin).getMetadata();
        doReturn("io.jenkins.tools.pluginmodernizer.SetupGitIgnore")
                .when(recipe)
                .getName();

        // Test
        String result = TemplateUtils.renderPullRequestTitle(plugin, recipe);

        // Assert
        assertEquals("Setup .gitignore file", result);
    }

    @Test
    public void testPrTitleForMergeGitIgnoreRecipe() {
        // Mocks
        Plugin plugin = mock(Plugin.class);
        PluginMetadata metadata = mock(PluginMetadata.class);
        Recipe recipe = mock(Recipe.class);

        doReturn(metadata).when(plugin).getMetadata();
        doReturn("io.jenkins.tools.pluginmodernizer.MergeGitIgnoreRecipe")
                .when(recipe)
                .getName();

        // Test
        String result = TemplateUtils.renderPullRequestTitle(plugin, recipe);

        // Assert
        assertEquals("Merges .gitignore entries from archetype with existing .gitignore file.", result);
    }
}
