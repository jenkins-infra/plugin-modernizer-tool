package io.jenkins.tools.pluginmodernizer.cli;

import io.jenkins.tools.pluginmodernizer.cli.options.GlobalOptions;
import io.quarkus.picocli.runtime.PicocliCommandLineFactory;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import picocli.CommandLine;

@ApplicationScoped
public class CommandLineFactory {

    @Produces
    CommandLine commandLine(PicocliCommandLineFactory factory) {
        CommandLine cmd = factory.create();
        cmd.addMixin("globalOptions", GlobalOptions.getInstance());
        cmd.getSubcommands()
                .get("generate-completion")
                .getCommandSpec()
                .usageMessage()
                .hidden(true);
        return cmd;
    }
}
