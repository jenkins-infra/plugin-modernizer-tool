package io.jenkins.tools.pluginmodernizer.core.campaign;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.jenkins.tools.pluginmodernizer.core.model.ModernizerException;
import io.jenkins.tools.pluginmodernizer.core.utils.RecipeResolver;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CampaignParserTest {

    @TempDir
    private Path tempDir;

    @Test
    void shouldParseValidCampaignFile() throws Exception {
        Path campaignFile = tempDir.resolve("campaign.yaml");
        Files.writeString(campaignFile, """
                plugins:
                  names:
                    - git
                    - git-client
                  topPlugins: 5
                  filters:
                    minInstallations: 100
                    maxHealthScore: 80
                    excludeDeprecated: true
                stages:
                  - recipe: SetupDependabot
                  - name: Upgrade parent
                    recipe: UpgradeParent6Version
                    skipMetadata: true
                execution:
                  concurrency: 2
                  continueOnFailure: false
                  skipMetadata: true
                output:
                  reportJson: reports/campaign.json
                """);

        CampaignParser parser = new CampaignParser(new RecipeResolver());
        CampaignDefinition definition = parser.parse(campaignFile);

        assertEquals(2, definition.getStages().size());
        assertEquals("git", definition.getPlugins().getNames().get(0));
        assertEquals(5, definition.getPlugins().getTopPlugins());
        assertTrue(definition.getPlugins().getFilters().isExcludeDeprecated());
        assertEquals(2, definition.getExecution().getConcurrency());
        assertEquals(false, definition.getExecution().isContinueOnFailure());
        assertEquals("reports/campaign.json", definition.getOutput().getReportJson());
    }

    @Test
    void shouldRejectCampaignWithoutStages() throws Exception {
        Path campaignFile = tempDir.resolve("invalid-campaign.yaml");
        Files.writeString(campaignFile, """
                plugins:
                  names:
                    - git
                """);

        CampaignParser parser = new CampaignParser(new RecipeResolver());
        ModernizerException exception = assertThrows(ModernizerException.class, () -> parser.parse(campaignFile));

        assertTrue(exception.getMessage().contains("at least one stage"));
    }

    @Test
    void shouldRejectCampaignWithNoPluginSource() throws Exception {
        Path campaignFile = tempDir.resolve("no-source.yaml");
        Files.writeString(campaignFile, """
                stages:
                  - recipe: SetupDependabot
                """);

        CampaignParser parser = new CampaignParser(new RecipeResolver());
        ModernizerException exception = assertThrows(ModernizerException.class, () -> parser.parse(campaignFile));

        assertTrue(exception.getMessage().contains("at least one plugin source"));
    }

    @Test
    void shouldRejectCampaignWithZeroTopPlugins() throws Exception {
        Path campaignFile = tempDir.resolve("bad-top.yaml");
        Files.writeString(campaignFile, """
                plugins:
                  topPlugins: 0
                stages:
                  - recipe: SetupDependabot
                """);

        CampaignParser parser = new CampaignParser(new RecipeResolver());
        ModernizerException exception = assertThrows(ModernizerException.class, () -> parser.parse(campaignFile));

        assertTrue(exception.getMessage().contains("topPlugins must be greater than zero"));
    }

    @Test
    void shouldRejectCampaignWithUnknownRecipe() throws Exception {
        Path campaignFile = tempDir.resolve("bad-recipe.yaml");
        Files.writeString(campaignFile, """
                plugins:
                  names:
                    - git
                stages:
                  - recipe: NotARealRecipe
                """);

        CampaignParser parser = new CampaignParser(new RecipeResolver());
        assertThrows(IllegalArgumentException.class, () -> parser.parse(campaignFile));
    }
}
