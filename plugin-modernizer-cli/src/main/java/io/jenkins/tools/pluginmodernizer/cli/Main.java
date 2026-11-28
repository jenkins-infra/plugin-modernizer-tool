package io.jenkins.tools.pluginmodernizer.cli;

import io.jenkins.tools.pluginmodernizer.cli.command.BuildMetadataCommand;
import io.jenkins.tools.pluginmodernizer.cli.command.CampaignCommand;
import io.jenkins.tools.pluginmodernizer.cli.command.CleanupCommand;
import io.jenkins.tools.pluginmodernizer.cli.command.DryRunCommand;
import io.jenkins.tools.pluginmodernizer.cli.command.ListRecipesCommand;
import io.jenkins.tools.pluginmodernizer.cli.command.RunCommand;
import io.jenkins.tools.pluginmodernizer.cli.command.ValidateCommand;
import io.jenkins.tools.pluginmodernizer.cli.command.VersionCommand;
import io.quarkus.picocli.runtime.annotations.TopCommand;
import picocli.AutoComplete;
import picocli.CommandLine.Command;

/**
 * Quarkus generates the main method and runs this command.
 */
@TopCommand
@Command(
        name = "plugin-modernizer",
        description = "Plugin Modernizer. A tool to modernize Jenkins plugins",
        synopsisSubcommandLabel = "COMMAND",
        subcommands = {
            AutoComplete.GenerateCompletion.class,
            ValidateCommand.class,
            ListRecipesCommand.class,
            BuildMetadataCommand.class,
            CampaignCommand.class,
            DryRunCommand.class,
            RunCommand.class,
            CleanupCommand.class,
            VersionCommand.class
        },
        mixinStandardHelpOptions = true,
        versionProvider = VersionProvider.class)
public class Main {}
