package io.jenkins.tools.pluginmodernizer.core.campaign;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.jenkins.tools.pluginmodernizer.core.config.Config;
import io.jenkins.tools.pluginmodernizer.core.model.Plugin;
import io.jenkins.tools.pluginmodernizer.core.utils.RecipeResolver;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CampaignServiceTest {

    @TempDir
    private Path tempDir;

    @Test
    void shouldStopAfterFailedStageWhenContinueOnFailureIsDisabled() throws Exception {
        CampaignDefinition definition = buildDefinition(false);
        Path campaignFile = tempDir.resolve("campaign.yaml");
        Files.writeString(campaignFile, "ignored");

        Path workspace = tempDir.resolve("workspace");
        Files.createDirectories(workspace);
        AtomicInteger callCount = new AtomicInteger();
        CampaignModernizerRunner runner = (stageConfig, stage) -> {
            callCount.incrementAndGet();
            CampaignStageReport report = new CampaignStageReport();
            report.setStageName(stage.getRecipe());
            report.setRecipe(stageConfig.getRecipe().getName());
            report.setSuccess(false);
            report.setLocalRepository(workspace.toString());
            report.setModifiedFiles(List.of());
            report.setErrors(List.of("boom"));
            return report;
        };

        CampaignParser parser = new StubCampaignParser(definition);
        CampaignPluginSelector selector = new StubCampaignPluginSelector(List.of(Plugin.build("git")));
        CampaignService service = new CampaignService(baseConfig(), parser, selector, runner, new RecipeResolver());
        CampaignReport report = service.run(campaignFile);

        assertEquals(1, callCount.get());
        assertEquals(1, report.getFailedPlugins());
        assertEquals(1, report.getFailedStages());
        assertTrue(Files.exists(tempDir.resolve("reports").resolve("campaign.json")));
    }

    @Test
    void shouldReuseLocalWorkspaceAcrossStages() throws Exception {
        CampaignDefinition definition = buildDefinition(true);
        Path campaignFile = tempDir.resolve("campaign.yaml");
        Files.writeString(campaignFile, "ignored");

        Path workspace = tempDir.resolve("workspace");
        Files.createDirectories(workspace);
        AtomicInteger callCount = new AtomicInteger();
        CampaignModernizerRunner runner = (stageConfig, stage) -> {
            int invocation = callCount.incrementAndGet();
            CampaignStageReport report = new CampaignStageReport();
            report.setStageName(stage.getRecipe());
            report.setRecipe(stageConfig.getRecipe().getName());
            report.setSuccess(true);
            report.setLocalRepository(workspace.toString());
            report.setModifiedFiles(List.of("pom.xml"));
            report.setErrors(List.of());

            assertTrue(stageConfig.isDryRun());
            if (invocation == 2) {
                Plugin plugin = stageConfig.getPlugins().get(0);
                assertTrue(plugin.isLocal());
                assertEquals(
                        workspace.toAbsolutePath(), plugin.getLocalRepository().toAbsolutePath());
            }
            return report;
        };

        CampaignParser parser = new StubCampaignParser(definition);
        CampaignPluginSelector selector = new StubCampaignPluginSelector(List.of(Plugin.build("git")));
        CampaignService service = new CampaignService(baseConfig(), parser, selector, runner, new RecipeResolver());
        CampaignReport report = service.run(campaignFile);

        assertEquals(2, callCount.get());
        assertEquals(1, report.getSuccessfulPlugins());
        assertEquals(2, report.getSuccessfulStages());
        assertEquals(
                workspace.toAbsolutePath().toString(),
                report.getPlugins().get(0).getFinalLocalRepository());
    }

    @Test
    void shouldHandleFailedStageWithoutLocalRepository() throws Exception {
        CampaignDefinition definition = buildDefinition(false);
        Path campaignFile = tempDir.resolve("campaign.yaml");
        Files.writeString(campaignFile, "ignored");

        CampaignModernizerRunner runner = (stageConfig, stage) -> {
            CampaignStageReport report = new CampaignStageReport();
            report.setStageName(stage.getRecipe());
            report.setRecipe(stageConfig.getRecipe().getName());
            report.setSuccess(false);
            report.setLocalRepository(null);
            report.setModifiedFiles(List.of());
            report.setErrors(List.of("stage failed before checkout"));
            return report;
        };

        CampaignParser parser = new StubCampaignParser(definition);
        CampaignPluginSelector selector = new StubCampaignPluginSelector(List.of(Plugin.build("git")));
        CampaignService service = new CampaignService(baseConfig(), parser, selector, runner, new RecipeResolver());
        CampaignReport report = service.run(campaignFile);

        assertEquals(1, report.getFailedPlugins());
        assertNull(report.getPlugins().get(0).getFinalLocalRepository());
        assertEquals(
                "stage failed before checkout",
                report.getPlugins().get(0).getStages().get(0).getErrors().get(0));
    }

    private CampaignDefinition buildDefinition(boolean continueOnFailure) {
        CampaignStage stage1 = new CampaignStage();
        stage1.setRecipe("SetupDependabot");
        CampaignStage stage2 = new CampaignStage();
        stage2.setRecipe("SetupSecurityScan");

        CampaignExecution execution = new CampaignExecution();
        execution.setConcurrency(1);
        execution.setContinueOnFailure(continueOnFailure);

        CampaignOutput output = new CampaignOutput();
        output.setReportJson("reports/campaign.json");

        CampaignDefinition definition = new CampaignDefinition();
        definition.setStages(List.of(stage1, stage2));
        definition.setExecution(execution);
        definition.setOutput(output);
        return definition;
    }

    private Config baseConfig() throws Exception {
        Path cachePath = tempDir.resolve("cache");
        Path mavenHome = tempDir.resolve("maven-home");
        Path mavenLocalRepo = tempDir.resolve("m2");
        Files.createDirectories(cachePath);
        Files.createDirectories(mavenHome);
        Files.createDirectories(mavenLocalRepo);
        return Config.builder()
                .withVersion("999999-SNAPSHOT")
                .withCachePath(cachePath)
                .withMavenHome(mavenHome)
                .withMavenLocalRepo(mavenLocalRepo)
                .build();
    }

    private static final class StubCampaignParser extends CampaignParser {

        private final CampaignDefinition definition;

        private StubCampaignParser(CampaignDefinition definition) {
            super(new RecipeResolver());
            this.definition = definition;
        }

        @Override
        public CampaignDefinition parse(Path campaignFile) {
            return definition;
        }
    }

    private static final class StubCampaignPluginSelector extends CampaignPluginSelector {

        private final List<Plugin> plugins;

        private StubCampaignPluginSelector(List<Plugin> plugins) {
            super(new io.jenkins.tools.pluginmodernizer.core.utils.PluginService());
            this.plugins = plugins;
        }

        @Override
        public List<Plugin> selectPlugins(CampaignDefinition definition, Path campaignFile) {
            return plugins;
        }
    }
}
