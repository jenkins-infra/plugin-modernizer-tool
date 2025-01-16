package io.jenkins.tools.pluginmodernizer.cli;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.sift.AbstractDiscriminator;
import io.jenkins.tools.pluginmodernizer.cli.options.GlobalOptions;
import io.jenkins.tools.pluginmodernizer.core.config.Config;
import io.jenkins.tools.pluginmodernizer.core.model.Plugin;
import java.nio.file.Path;
import java.util.List;
import org.slf4j.Marker;

public class PluginLoggerDiscriminator extends AbstractDiscriminator<ILoggingEvent> {

    @Override
    public String getDiscriminatingValue(ILoggingEvent iLoggingEvent) {
        List<Marker> markers = iLoggingEvent.getMarkerList();

        Config.Builder builder = Config.builder();
        GlobalOptions globalOptions = GlobalOptions.getInstance();
        globalOptions.config(builder);
        Config config = builder.build();

        String cachePath = config.getCachePath().toString();

        if (markers == null || markers.isEmpty()) {
            return Path.of(cachePath, "modernizer.logs").toString();
        }

        final Marker marker = markers.get(0);
        String markerName = marker.getName();
        Plugin plugin = Plugin.build(markerName);

        return Path.of(cachePath, plugin.getLogFile().toString()).toString();
    }

    @Override
    public String getKey() {
        return "filename";
    }
}
