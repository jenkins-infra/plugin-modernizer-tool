package io.jenkins.tools.pluginmodernizer.cli.command;

import io.jenkins.tools.pluginmodernizer.core.config.Config;
import io.jenkins.tools.pluginmodernizer.core.impl.PluginModernizer;
import jakarta.enterprise.inject.spi.CDI;
import java.util.concurrent.Callable;

/**
 * Interface for all subcommands
 */
public interface ICommand extends Callable<Integer> {

    /**
     * Enrich the configuration with the command specific options
     * @param builder the configuration builder
     * @return the enriched configuration
     */
    default Config setup(Config.Builder builder) {
        return builder.build();
    }

    /**
     * Looked up lazily because the configuration depends on the parsed options of the executed command
     * @see io.jenkins.tools.pluginmodernizer.cli.ModernizerProducers
     */
    default PluginModernizer getModernizer() {
        return CDI.current().select(PluginModernizer.class).get();
    }
}
