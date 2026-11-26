package io.jenkins.tools.pluginmodernizer.cli;

import io.jenkins.tools.pluginmodernizer.cli.command.ICommand;
import io.jenkins.tools.pluginmodernizer.core.config.Config;
import io.jenkins.tools.pluginmodernizer.core.impl.CacheManager;
import io.jenkins.tools.pluginmodernizer.core.utils.JdkFetcher;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;
import org.apache.maven.shared.invoker.DefaultInvoker;
import org.apache.maven.shared.invoker.Invoker;
import picocli.CommandLine;

/**
 * Producers for the core services, configured from the options of the executed command
 */
@Singleton
public class ModernizerProducers {

    private CommandLine.ParseResult parseResult;

    void onParse(@Observes CommandLine.ParseResult parseResult) {
        this.parseResult = parseResult;
    }

    @Produces
    @Singleton
    Config config() {
        if (parseResult == null) {
            throw new IllegalStateException("Configuration requested before the command line was parsed");
        }
        CommandLine.ParseResult executed = parseResult;
        while (executed.hasSubcommand()) {
            executed = executed.subcommand();
        }
        return ((ICommand) executed.commandSpec().userObject()).setup(Config.builder());
    }

    @Produces
    @Singleton
    Invoker invoker() {
        return new DefaultInvoker();
    }

    @Produces
    @Singleton
    CacheManager cacheManager(Config config) {
        return new CacheManager(config.getCachePath());
    }

    @Produces
    @Singleton
    JdkFetcher jdkFetcher(Config config) {
        return new JdkFetcher(config.getCachePath());
    }
}
