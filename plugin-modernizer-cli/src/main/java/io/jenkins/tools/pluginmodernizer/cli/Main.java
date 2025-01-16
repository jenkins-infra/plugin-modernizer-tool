package io.jenkins.tools.pluginmodernizer.cli;

import io.jenkins.tools.pluginmodernizer.cli.command.BuildMetadataCommand;
import io.jenkins.tools.pluginmodernizer.cli.command.CleanupCommand;
import io.jenkins.tools.pluginmodernizer.cli.command.DryRunCommand;
import io.jenkins.tools.pluginmodernizer.cli.command.ListRecipesCommand;
import io.jenkins.tools.pluginmodernizer.cli.command.RunCommand;
import io.jenkins.tools.pluginmodernizer.cli.command.ValidateCommand;
import io.jenkins.tools.pluginmodernizer.cli.command.VersionCommand;
import io.jenkins.tools.pluginmodernizer.cli.options.GlobalOptions;
import org.slf4j.bridge.SLF4JBridgeHandler;
import picocli.AutoComplete;
import picocli.CommandLine;
import picocli.CommandLine.Command;

@Command(
        name = "plugin-modernizer",
        description = "Plugin Modernizer. A tool to modernize Jenkins plugins",
        synopsisSubcommandLabel = "COMMAND",
        subcommands = {
            AutoComplete.GenerateCompletion.class,
            ValidateCommand.class,
            ListRecipesCommand.class,
            BuildMetadataCommand.class,
            DryRunCommand.class,
            RunCommand.class,
            CleanupCommand.class,
            VersionCommand.class
        },
        mixinStandardHelpOptions = true,
        versionProvider = VersionProvider.class)
public class Main {

    static {
        System.setProperty("slf4j.internal.verbosity", "WARN");
        SLF4JBridgeHandler.removeHandlersForRootLogger();
        SLF4JBridgeHandler.install();
    }

    /**
     * Main method
     * @param args Command line arguments
     */
    public static void main(final String[] args) {
        GlobalOptions globalOptions = GlobalOptions.getInstance();
        CommandLine cmd = new CommandLine(new Main());
        cmd.addMixin("globalOptions", globalOptions);
        CommandLine gen = cmd.getSubcommands().get("generate-completion");
        gen.getCommandSpec().usageMessage().hidden(true);
        System.exit(cmd.execute(args));
    }
}
