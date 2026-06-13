package io.jenkins.tools.pluginmodernizer.core.utils;

import io.jenkins.tools.pluginmodernizer.core.model.Plugin;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Resolves a local filesystem path to a Jenkins plugin.
 */
public class PluginPathResolver {

    private static final Logger LOG = LoggerFactory.getLogger(PluginPathResolver.class);

    public Plugin resolve(String value) throws IOException {
        return resolve(Path.of(value));
    }

    public Plugin resolve(Path path) throws IOException {
        if (!Files.isDirectory(path)) {
            throw new IllegalArgumentException("Path is not a directory: " + path);
        }

        Path pom = path.resolve("pom.xml");
        if (!Files.exists(pom)) {
            throw new IllegalArgumentException("Path does not contain a pom.xml: " + path);
        }

        StaticPomParser rootPomParser = new StaticPomParser(pom.toString());
        String packaging = rootPomParser.getPackaging();

        // Check if this is a single-module Jenkins plugin
        if ("hpi".equals(packaging)) {
            String artifactId = rootPomParser.getArtifactId();
            if (artifactId == null) {
                throw new IllegalArgumentException("Path does not contain a valid Jenkins plugin: " + path);
            }
            LOG.info("Found single-module plugin '{}' at root level", artifactId);
            return Plugin.build(artifactId, path);
        }

        // Check if this is a multi-module project (packaging = pom)
        if ("pom".equals(packaging) || packaging == null || packaging.isEmpty()) {
            LOG.info("Detected multi-module project, searching for Jenkins plugin module...");
            Path pluginPath = findJenkinsPluginModule(path);
            if (pluginPath != null) {
                StaticPomParser pluginPomParser =
                        new StaticPomParser(pluginPath.resolve("pom.xml").toString());
                String artifactId = pluginPomParser.getArtifactId();
                if (artifactId == null) {
                    throw new IllegalArgumentException(
                            "Plugin module does not contain valid artifactId: " + pluginPath);
                }
                LOG.info("Found Jenkins plugin module '{}' at: {}", artifactId, pluginPath);
                return Plugin.build(artifactId, pluginPath);
            }
            throw new IllegalArgumentException(
                    "Multi-module project detected but no module with packaging 'hpi' found" + path);
        }

        throw new IllegalArgumentException(
                "Path does not contain a Jenkins plugin (packaging must be 'hpi' or a multi-module project with an hpi module): "
                        + path);
    }

    /**
     * Find the Jenkins plugin module in a multi-module project.
     * Searches all subdirectories for a pom.xml with packaging 'hpi'.
     *
     * @param rootPath The root path of the multi-module project
     * @return The path to the plugin module, or null if not found
     * @throws IOException if an I/O error occurs
     */
    private Path findJenkinsPluginModule(Path rootPath) throws IOException {
        try (Stream<Path> paths = Files.walk(rootPath, 2)) { // Search up to 2 levels deep
            return paths.filter(Files::isDirectory)
                    .filter(dir -> !dir.equals(rootPath)) // Skip root directory
                    .filter(dir -> Files.exists(dir.resolve("pom.xml")))
                    .filter(dir -> {
                        try {
                            StaticPomParser parser =
                                    new StaticPomParser(dir.resolve("pom.xml").toString());
                            String packaging = parser.getPackaging();
                            return "hpi".equals(packaging);
                        } catch (Exception e) {
                            LOG.debug("Failed to parse pom.xml in {}: {}", dir, e.getMessage());
                            return false;
                        }
                    })
                    .findFirst()
                    .orElse(null);
        }
    }
}
