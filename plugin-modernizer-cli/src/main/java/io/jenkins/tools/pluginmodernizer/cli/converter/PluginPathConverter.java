package io.jenkins.tools.pluginmodernizer.cli.converter;

import io.jenkins.tools.pluginmodernizer.core.model.Plugin;
import io.jenkins.tools.pluginmodernizer.core.utils.PluginPathResolver;
import picocli.CommandLine;

/**
 * Custom converter to get a list of plugin from a local folder
 */
public class PluginPathConverter implements CommandLine.ITypeConverter<Plugin> {
    private final PluginPathResolver pluginPathResolver = new PluginPathResolver();

    @Override
    public Plugin convert(String value) throws Exception {
        return pluginPathResolver.resolve(value);
    }
}
