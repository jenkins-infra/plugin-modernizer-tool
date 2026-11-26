package io.jenkins.tools.pluginmodernizer.cli;

import io.quarkus.test.junit.main.QuarkusMainIntegrationTest;
import org.junit.jupiter.api.Tag;

/**
 * Same checks as {@link MainTest} against the packaged application, the uber-jar or the native executable
 * when built with {@code -Dnative}.
 */
@Tag("Always")
@QuarkusMainIntegrationTest
public class MainITCase extends MainTest {}
