package io.jenkins.tools.pluginmodernizer.cli.command;

import io.jenkins.tools.pluginmodernizer.cli.converter.RecipeConverter;
import io.jenkins.tools.pluginmodernizer.cli.options.EnvOptions;
import io.jenkins.tools.pluginmodernizer.cli.options.GitHubOptions;
import io.jenkins.tools.pluginmodernizer.cli.options.GlobalOptions;
import io.jenkins.tools.pluginmodernizer.cli.options.PluginOptions;
import io.jenkins.tools.pluginmodernizer.core.config.Config;
import io.jenkins.tools.pluginmodernizer.core.impl.PluginModernizer;
import io.jenkins.tools.pluginmodernizer.core.model.ModernizerException;
import io.jenkins.tools.pluginmodernizer.core.model.Recipe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

/**
 * Run command
 */
@CommandLine.Command(name = "run", description = "Run")
public class RunCommand implements ICommand {

    /**
     * Logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(RunCommand.class);

    /**
     * Plugins options
     */
    @CommandLine.ArgGroup
    private PluginOptions pluginOptions;

    /**
     * Recipe to be applied
     */
    @CommandLine.Option(
            names = {"-r", "--recipe"},
            required = true,
            description = "Recipe to be applied.",
            completionCandidates = RecipeConverter.class,
            converter = RecipeConverter.class)
    private Recipe recipe;

    @CommandLine.Option(
            names = {"--draft"},
            description = "Open a draft pull request.")
    public boolean draft;

    @CommandLine.Option(
            names = {"--clean-forks"},
            description =
                    "Remove forked repositories before and after the modernization process. Might cause data loss if you have other changes pushed on those forks. Forks with open pull request targeting original repo are not removed to prevent closing unmerged pull requests.")
    public boolean removeForks;

    /**
     * Skip modernization metadata
     */
    @CommandLine.Option(
            names = {"--skip-metadata"},
            description = "Disable collection and pushing of modernization metadata")
    private boolean skipMetadata;

    /**
     * Override opt out plugins
     */
    @CommandLine.Option(
            names = {"--override-opt-out-plugins"},
            description = "Override the default behavior to allow PRs to plugins marked as opt-out")
    private boolean overrideOptOutPlugins;

    /**
     * Environment options
     */
    @CommandLine.Mixin
    private EnvOptions envOptions;

    /**
     * Global options for all commands
     */
    @CommandLine.Mixin
    private GlobalOptions options = GlobalOptions.getInstance();

    /**
     * GitHub options
     */
    @CommandLine.Mixin
    private GitHubOptions githubOptions;

    @Override
    public Config setup(Config.Builder builder) {
        options.config(builder);
        envOptions.config(builder);
        if (pluginOptions == null) {
            pluginOptions = new PluginOptions();
        }
        pluginOptions.config(builder);
        githubOptions.config(builder);
        return builder.withRecipe(recipe)
                .withDraft(draft)
                .withRemoveForks(removeForks)
                .withSkipMetadata(skipMetadata)
                .withOverrideOptOutPlugins(overrideOptOutPlugins)
                .build();
    }

    @Override
    public Integer call() throws Exception {
        LOG.info("Run Plugin Modernizer");
        PluginModernizer modernizer = getModernizer();
        try {
            modernizer.validate();
        } catch (ModernizerException e) {
            LOG.error("Validation error");
            LOG.error(e.getMessage());
            return 1;
        }
        modernizer.start();
        return 0;
    }
}
