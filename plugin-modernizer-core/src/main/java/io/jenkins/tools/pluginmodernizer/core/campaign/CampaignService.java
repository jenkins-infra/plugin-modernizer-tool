package io.jenkins.tools.pluginmodernizer.core.campaign;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jenkins.tools.pluginmodernizer.core.config.Config;
import io.jenkins.tools.pluginmodernizer.core.model.Plugin;
import io.jenkins.tools.pluginmodernizer.core.model.Recipe;
import io.jenkins.tools.pluginmodernizer.core.utils.RecipeResolver;
import jakarta.inject.Inject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Orchestrates campaign execution on top of the existing single-recipe engine.
 */
public class CampaignService {

    private final Config baseConfig;
    private final CampaignParser campaignParser;
    private final CampaignPluginSelector campaignPluginSelector;
    private final CampaignModernizerRunner campaignModernizerRunner;
    private final RecipeResolver recipeResolver;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Inject
    public CampaignService(
            Config baseConfig,
            CampaignParser campaignParser,
            CampaignPluginSelector campaignPluginSelector,
            CampaignModernizerRunner campaignModernizerRunner,
            RecipeResolver recipeResolver) {
        this.baseConfig = baseConfig;
        this.campaignParser = campaignParser;
        this.campaignPluginSelector = campaignPluginSelector;
        this.campaignModernizerRunner = campaignModernizerRunner;
        this.recipeResolver = recipeResolver;
    }

    public CampaignReport run(Path campaignFile) {
        Path normalizedCampaignFile = campaignFile.toAbsolutePath().normalize();
        CampaignDefinition definition = campaignParser.parse(normalizedCampaignFile);
        List<Plugin> selectedPlugins = campaignPluginSelector.selectPlugins(definition, normalizedCampaignFile);

        Instant startedAt = Instant.now();
        List<CampaignPluginReport> pluginReports = executePlugins(definition, selectedPlugins);
        Instant finishedAt = Instant.now();

        CampaignReport report = buildReport(definition, normalizedCampaignFile, startedAt, finishedAt, pluginReports);
        Path reportPath = resolveReportPath(definition, normalizedCampaignFile);
        writeReport(report, reportPath);
        report.setReportJson(reportPath.toAbsolutePath().toString());
        return report;
    }

    private List<CampaignPluginReport> executePlugins(CampaignDefinition definition, List<Plugin> selectedPlugins) {
        ExecutorService executorService =
                Executors.newFixedThreadPool(definition.getExecution().getConcurrency());
        try {
            List<Future<CampaignPluginReport>> futures = selectedPlugins.stream()
                    .map(plugin -> executorService.submit(new CampaignPluginTask(definition, plugin)))
                    .toList();

            List<CampaignPluginReport> reports = new ArrayList<>();
            for (Future<CampaignPluginReport> future : futures) {
                reports.add(future.get());
            }
            return reports;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Campaign execution was interrupted", e);
        } catch (ExecutionException e) {
            throw new RuntimeException("Campaign execution failed", e.getCause());
        } finally {
            executorService.shutdownNow();
        }
    }

    private CampaignReport buildReport(
            CampaignDefinition definition,
            Path campaignFile,
            Instant startedAt,
            Instant finishedAt,
            List<CampaignPluginReport> pluginReports) {
        int totalStages = pluginReports.stream()
                .mapToInt(report -> report.getStages().size())
                .sum();
        int successfulStages = pluginReports.stream()
                .flatMap(report -> report.getStages().stream())
                .mapToInt(stage -> stage.isSuccess() ? 1 : 0)
                .sum();
        int failedStages = totalStages - successfulStages;
        int successfulPlugins = (int)
                pluginReports.stream().filter(CampaignPluginReport::isSuccess).count();

        CampaignReport report = new CampaignReport();
        report.setCampaignFile(campaignFile.toString());
        report.setDryRun(true);
        report.setConcurrency(definition.getExecution().getConcurrency());
        report.setStartedAt(startedAt.toString());
        report.setFinishedAt(finishedAt.toString());
        report.setTotalPlugins(pluginReports.size());
        report.setSuccessfulPlugins(successfulPlugins);
        report.setFailedPlugins(pluginReports.size() - successfulPlugins);
        report.setTotalStages(totalStages);
        report.setSuccessfulStages(successfulStages);
        report.setFailedStages(failedStages);
        report.setPlugins(pluginReports);
        return report;
    }

    private void writeReport(CampaignReport report, Path reportPath) {
        try {
            Path parent = reportPath.toAbsolutePath().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(reportPath.toFile(), report);
        } catch (IOException e) {
            throw new RuntimeException("Failed to write campaign report to " + reportPath, e);
        }
    }

    private Path resolveReportPath(CampaignDefinition definition, Path campaignFile) {
        String reportJson = definition.getOutput().getReportJson();
        if (reportJson == null || reportJson.isBlank()) {
            return campaignFile.getParent().resolve("campaign-report.json").normalize();
        }
        Path configuredPath = Path.of(reportJson);
        if (configuredPath.isAbsolute()) {
            return configuredPath.normalize();
        }
        return campaignFile.getParent().resolve(configuredPath).normalize();
    }

    private Config buildStageConfig(Plugin plugin, CampaignDefinition definition, CampaignStage stage) {
        Recipe recipe = recipeResolver.resolve(stage.getRecipe());
        boolean skipMetadata = stage.getSkipMetadata() != null
                ? stage.getSkipMetadata()
                : definition.getExecution().isSkipMetadata();

        Path mavenHome = baseConfig.getConfiguredMavenHome() != null
                ? baseConfig.getConfiguredMavenHome()
                : baseConfig.getDetectedMavenHome();

        return Config.builder()
                .withVersion(baseConfig.getVersion())
                .withGitHubOwner(baseConfig.getGithubOwner())
                .withGitHubAppId(baseConfig.getGithubAppId())
                .withGitHubAppSourceInstallationId(baseConfig.getGithubAppSourceInstallationId())
                .withGitHubAppTargetInstallationId(baseConfig.getGithubAppTargetInstallationId())
                .withSshPrivateKey(baseConfig.getSshPrivateKey())
                .withPlugins(List.of(plugin))
                .withRecipe(recipe)
                .withJenkinsUpdateCenter(baseConfig.getJenkinsUpdateCenter())
                .withJenkinsPluginVersions(baseConfig.getJenkinsPluginVersions())
                .withPluginHealthScore(baseConfig.getPluginHealthScore())
                .withPluginStatsInstallations(baseConfig.getPluginStatsInstallations())
                .withOptOutPlugins(baseConfig.getOptOutPlugins())
                .withGithubApiUrl(baseConfig.getGithubApiUrl())
                .withCachePath(baseConfig.getCachePath())
                .withMavenHome(mavenHome)
                .withMavenLocalRepo(baseConfig.getMavenLocalRepo())
                .withSkipMetadata(skipMetadata)
                .withOverrideOptOutPlugins(baseConfig.isOverrideOptOutPlugins())
                .withDryRun(true)
                .withDraft(false)
                .withRemoveForks(false)
                .withAllowDeprecatedPlugins(baseConfig.isAllowDeprecatedPlugins())
                .withDuplicatePrStrategy(baseConfig.getDuplicatePrStrategy())
                .build();
    }

    private final class CampaignPluginTask implements Callable<CampaignPluginReport> {

        private final CampaignDefinition definition;
        private final Plugin initialPlugin;

        private CampaignPluginTask(CampaignDefinition definition, Plugin initialPlugin) {
            this.definition = definition;
            this.initialPlugin = initialPlugin;
        }

        @Override
        public CampaignPluginReport call() {
            List<CampaignStageReport> stageReports = new ArrayList<>();
            Plugin currentPlugin = initialPlugin;

            for (CampaignStage stage : definition.getStages()) {
                Config stageConfig = buildStageConfig(currentPlugin, definition, stage);
                CampaignStageReport stageReport = campaignModernizerRunner.runStage(stageConfig, stage);
                stageReports.add(stageReport);

                if (stageReport.getLocalRepository() != null
                        && !stageReport.getLocalRepository().isBlank()) {
                    Path nextRepository = Path.of(stageReport.getLocalRepository());
                    if (Files.isDirectory(nextRepository)) {
                        currentPlugin = Plugin.build(initialPlugin.getName(), nextRepository);
                    }
                }

                if (!stageReport.isSuccess() && !definition.getExecution().isContinueOnFailure()) {
                    break;
                }
            }

            CampaignPluginReport report = new CampaignPluginReport();
            report.setPluginName(initialPlugin.getName());
            report.setLocal(initialPlugin.isLocal());
            report.setSource(
                    initialPlugin.isLocal()
                            ? initialPlugin
                                    .getLocalRepository()
                                    .toAbsolutePath()
                                    .toString()
                            : initialPlugin.getName());
            if (currentPlugin.isLocal() && currentPlugin.getLocalRepository() != null) {
                report.setFinalLocalRepository(
                        currentPlugin.getLocalRepository().toAbsolutePath().toString());
            } else {
                report.setFinalLocalRepository(null);
            }
            report.setSuccess(stageReports.stream().allMatch(CampaignStageReport::isSuccess));
            report.setStages(stageReports);
            return report;
        }
    }
}
