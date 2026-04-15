package io.jenkins.tools.pluginmodernizer.cli.command;

import com.google.inject.Guice;
import io.jenkins.tools.pluginmodernizer.cli.options.EnvOptions;
import io.jenkins.tools.pluginmodernizer.cli.options.GitHubOptions;
import io.jenkins.tools.pluginmodernizer.cli.options.GlobalOptions;
import io.jenkins.tools.pluginmodernizer.core.GuiceModule;
import io.jenkins.tools.pluginmodernizer.core.campaign.CampaignReport;
import io.jenkins.tools.pluginmodernizer.core.campaign.CampaignService;
import io.jenkins.tools.pluginmodernizer.core.config.Config;
import io.jenkins.tools.pluginmodernizer.core.model.ModernizerException;
import java.nio.file.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

/**
 * Campaign command.
 */
@CommandLine.Command(
        name = "campaign",
        description =
                "Run a multi-stage modernization campaign in dry-run mode and emit a structured JSON report")
public class CampaignCommand implements ICommand {

    private static final Logger LOG = LoggerFactory.getLogger(CampaignCommand.class);

    @CommandLine.Option(
            names = {"--file"},
            required = true,
            description = "Path to the campaign YAML file.")
    private Path file;

    @CommandLine.Mixin
    private EnvOptions envOptions;

    @CommandLine.Mixin
    private GlobalOptions options = GlobalOptions.getInstance();

    @CommandLine.Mixin
    private GitHubOptions githubOptions;

    @Override
    public Config setup(Config.Builder builder) {
        options.config(builder);
        envOptions.config(builder);
        githubOptions.config(builder);
        return builder.withDryRun(true).build();
    }

    @Override
    public Integer call() {
        try {
            CampaignService campaignService =
                    Guice.createInjector(new GuiceModule(setup(Config.builder()))).getInstance(CampaignService.class);
            CampaignReport report = campaignService.run(file);
            LOG.info(
                    "Campaign finished. Plugins: {} success / {} failed. Report: {}",
                    report.getSuccessfulPlugins(),
                    report.getFailedPlugins(),
                    report.getReportJson());
            return report.getFailedStages() > 0 ? 1 : 0;
        } catch (ModernizerException e) {
            LOG.error("Campaign validation error");
            LOG.error(e.getMessage());
            return 1;
        } catch (RuntimeException e) {
            LOG.error("Campaign execution failed");
            LOG.error(e.getMessage() != null ? e.getMessage() : e.getClass().getName());
            return 1;
        }
    }
}
