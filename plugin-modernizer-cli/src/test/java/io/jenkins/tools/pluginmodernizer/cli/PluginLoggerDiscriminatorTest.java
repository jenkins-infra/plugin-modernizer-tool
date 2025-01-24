package io.jenkins.tools.pluginmodernizer.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.spi.ILoggingEvent;
import io.jenkins.tools.pluginmodernizer.cli.options.GlobalOptions;
import java.nio.file.Path;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Marker;

public class PluginLoggerDiscriminatorTest {

    @BeforeEach
    void resetSingleton() {
        GlobalOptions.reset();
    }

    @Test
    void testGetDiscriminatingValueNoMarkers() {
        PluginLoggerDiscriminator discriminator = new PluginLoggerDiscriminator();
        ILoggingEvent event = mock(ILoggingEvent.class);
        when(event.getMarkerList()).thenReturn(null);

        String expectedValue = Path.of(
                        System.getProperty("user.home"), ".cache", "jenkins-plugin-modernizer-cli", "modernizer.logs")
                .toString();
        String discriminatingValue = discriminator.getDiscriminatingValue(event);
        assertEquals(expectedValue, discriminatingValue);
    }

    @Test
    void testGetDiscriminatingValueEmptyMarkers() {
        PluginLoggerDiscriminator discriminator = new PluginLoggerDiscriminator();
        ILoggingEvent event = mock(ILoggingEvent.class);
        when(event.getMarkerList()).thenReturn(Collections.emptyList());

        String expectedValue = Path.of(
                        System.getProperty("user.home"), ".cache", "jenkins-plugin-modernizer-cli", "modernizer.logs")
                .toString();
        String discriminatingValue = discriminator.getDiscriminatingValue(event);
        assertEquals(expectedValue, discriminatingValue);
    }

    @Test
    void testGetDiscriminatingValueWithMarkers() {
        PluginLoggerDiscriminator discriminator = new PluginLoggerDiscriminator();
        ILoggingEvent event = mock(ILoggingEvent.class);
        Marker marker = mock(Marker.class);
        when(marker.getName()).thenReturn("testMarker");
        when(event.getMarkerList()).thenReturn(Collections.singletonList(marker));

        String discriminatingValue = discriminator.getDiscriminatingValue(event);
        String expectedValue = Path.of(
                        System.getProperty("user.home"),
                        ".cache",
                        "jenkins-plugin-modernizer-cli",
                        "testMarker",
                        "logs",
                        "invoker.logs")
                .toString();
        assertEquals(expectedValue, discriminatingValue);
    }

    @Test
    void testGetKey() {
        PluginLoggerDiscriminator discriminator = new PluginLoggerDiscriminator();
        String key = discriminator.getKey();
        assertEquals("filename", key);
    }
}
