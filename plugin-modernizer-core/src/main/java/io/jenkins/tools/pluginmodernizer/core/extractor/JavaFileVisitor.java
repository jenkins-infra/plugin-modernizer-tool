package io.jenkins.tools.pluginmodernizer.core.extractor;

import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A visitor to extract metadata from Java files.
 */
public class JavaFileVisitor extends JavaIsoVisitor<PluginMetadata> {

    /**
     * LOGGER.
     */
    private static final Logger LOG = LoggerFactory.getLogger(JavaFileVisitor.class);

    @Override
    public J.Import visitImport(J.Import _import, PluginMetadata pluginMetadata) {
        _import = super.visitImport(_import, pluginMetadata);
        // Rather a naive approach, but let's assume we can detect testcontainers usage by the package name
        if (_import.getPackageName().startsWith("org.testcontainers.containers")) {
            LOG.info("Found testcontainers import: {}. Plugin is using container tests", _import.getPackageName());
            pluginMetadata.setUseContainerTests(true);
        }
        if (_import.getPackageName().startsWith("org.jenkinsci.test.acceptance.docker")) {
            LOG.info("Found docker-fixtures import: {}. Plugin is using container tests", _import.getPackageName());
            pluginMetadata.setUseContainerTests(true);
        }
        return _import;
    }
}
