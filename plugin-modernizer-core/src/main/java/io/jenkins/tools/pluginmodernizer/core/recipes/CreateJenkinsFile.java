package io.jenkins.tools.pluginmodernizer.core.recipes;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.jenkins.tools.pluginmodernizer.core.extractor.ArchetypeCommonFile;
import io.jenkins.tools.pluginmodernizer.core.extractor.ArchetypeCommonFileVisitor;
import io.jenkins.tools.pluginmodernizer.core.extractor.PluginMetadata;
import io.jenkins.tools.pluginmodernizer.core.extractor.PomResolutionVisitor;
import io.jenkins.tools.pluginmodernizer.core.model.JDK;
import io.jenkins.tools.pluginmodernizer.core.utils.JsonUtils;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import org.intellij.lang.annotations.Language;
import org.openrewrite.*;
import org.openrewrite.text.CreateTextFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressFBWarnings(value = "VA_FORMAT_STRING_USES_NEWLINE", justification = "Newline is used for formatting")
public class CreateJenkinsFile extends ScanningRecipe<CreateJenkinsFile.ConfigState> {
    private static final Logger LOG = LoggerFactory.getLogger(CreateJenkinsFile.class);

    @Language("groovy")
    private static final String JENKINSFILE_TEMPLATE = """
            /*
            See the documentation for more options:
            https://github.com/jenkins-infra/pipeline-library/
            */
            buildPlugin(
                forkCount: '1C', // Run a JVM per core in tests
                useContainerAgent: true, // Set to `false` if you need to use Docker for containerized tests
                configurations: [
                    [platform: 'linux', jdk: %d],
                    [platform: 'windows', jdk: %d]
                ]
            )""";

    @Override
    public String getDisplayName() {
        return "Create Jenkinsfile";
    }

    @Override
    public String getDescription() {
        return "Creates a Jenkinsfile with JDK versions based on Jenkins version compatibility.";
    }

    @Override
    public ConfigState getInitialValue(ExecutionContext ctx) {
        return new ConfigState();
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(ConfigState state) {
        return new TreeVisitor<>() {
            @Override
            public Tree visit(Tree tree, ExecutionContext ctx) {
                if (tree instanceof SourceFile sourceFile) {
                    if (ArchetypeCommonFile.JENKINSFILE.same(sourceFile.getSourcePath())) {
                        LOG.debug("Jenkinsfile already exists. No need to generate.");
                        state.setJenkinsfileExists(true);
                        return tree;
                    }
                    if (ArchetypeCommonFile.POM.same(sourceFile.getSourcePath())) {
                        LOG.debug("Visiting POM {}", sourceFile.getSourcePath());

                        // First pass with ArchetypeCommonFileVisitor
                        PluginMetadata commonMetadata =
                                new ArchetypeCommonFileVisitor().reduce(tree, new PluginMetadata());

                        // Second pass with PomResolutionVisitor
                        PluginMetadata pomMetadata = new PomResolutionVisitor().reduce(tree, commonMetadata);
                        LOG.debug("POM metadata: {}", JsonUtils.toJson(pomMetadata));

                        // Add Jenkins version to the state
                        String jenkinsVersion = pomMetadata.getJenkinsVersion();
                        LOG.debug("Found Jenkins version: {}", jenkinsVersion);
                        state.setJenkinsVersion(jenkinsVersion);
                        if (pomMetadata.getProperties() != null) {
                            state.setProperties(pomMetadata.getProperties());
                        }
                        return tree;
                    }
                }
                return tree;
            }
        };
    }

    @Override
    public Collection<SourceFile> generate(ConfigState state, ExecutionContext ctx) {
        if (state.jenkinsfileExists) {
            LOG.debug("Jenkinsfile exists, skipping generation");
            return Collections.emptyList();
        }

        String jenkinsVersion = state.getJenkinsVersion();
        if (jenkinsVersion == null || jenkinsVersion.isEmpty()) {
            LOG.warn("No Jenkins version found in pom.xml");
            return Collections.emptyList();
        }

        LOG.debug("Generating Jenkinsfile for Jenkins version: {}", jenkinsVersion);
        List<JDK> supportedJdks = JDK.get(jenkinsVersion);
        int buildJava = resolvePluginJavaVersionForJdkCap(state.getProperties());
        int maxJenkinsJdkForBuildJava = 0;
        if (buildJava > 0) {
            JDK buildJdk = JDK.get(buildJava);
            JDK nextJdk = buildJdk.next();
            maxJenkinsJdkForBuildJava = nextJdk != null ? nextJdk.getMajor() : buildJdk.getMajor();
            final int jdkCap = maxJenkinsJdkForBuildJava;
            supportedJdks = supportedJdks.stream()
                    .filter(jdk -> jdk.getMajor() <= jdkCap)
                    .toList();
            LOG.debug(
                    "Filtered supported JDKs for plugin build Java {} (max major {}): {}",
                    buildJava,
                    maxJenkinsJdkForBuildJava,
                    supportedJdks);
        }
        LOG.debug("Supported JDKs: {}", supportedJdks);

        if (supportedJdks.isEmpty()) {
            LOG.warn("No supported JDKs found for Jenkins version: {}", jenkinsVersion);
            return Collections.emptyList();
        }

        List<Integer> topJdkVersions = JDK.getTopTwoJdkVersions(supportedJdks);
        LOG.debug("Top two JDK versions: {}", topJdkVersions);
        int linuxJdk = topJdkVersions.get(0);
        int windowsJdk = topJdkVersions.size() > 1 ? topJdkVersions.get(1) : linuxJdk;
        if (buildJava > 0 && buildJava < linuxJdk && buildJava <= maxJenkinsJdkForBuildJava) {
            windowsJdk = buildJava;
        }
        String jenkinsfileContent = String.format(JENKINSFILE_TEMPLATE, linuxJdk, windowsJdk);
        LOG.debug("Generated Jenkinsfile content with JDK versions: {} and {}", linuxJdk, windowsJdk);

        CreateTextFile createJenkinsfile = new CreateTextFile(
                jenkinsfileContent, ArchetypeCommonFile.JENKINSFILE.getPath().toString(), false);

        return createJenkinsfile.generate(new AtomicBoolean(true), ctx);
    }

    public TreeVisitor<?, ExecutionContext> getVisitor(ConfigState state) {
        return new TreeVisitor<>() {
            @Override
            public Tree visit(Tree tree, ExecutionContext ctx) {
                return tree;
            }
        };
    }

    /**
     * Resolves explicit plugin build Java from POM properties for Jenkinsfile JDK capping.
     * Only {@code java.version} and {@code java.level} are considered so compiler settings left
     * over from an older parent do not skew JDK suggestions during major parent upgrades.
     */
    static int resolvePluginJavaVersionForJdkCap(Map<String, String> properties) {
        return resolvePropertyAsJavaMajor(properties, List.of("java.version", "java.level"));
    }

    static int resolveBuildJavaVersion(Map<String, String> properties) {
        return resolvePropertyAsJavaMajor(
                properties,
                List.of("java.version", "maven.compiler.release", "maven.compiler.target", "java.level"));
    }

    private static int resolvePropertyAsJavaMajor(Map<String, String> properties, List<String> keys) {
        if (properties == null || properties.isEmpty()) {
            return 0;
        }
        for (String key : keys) {
            String value = properties.get(key);
            if (value == null || value.isBlank()) {
                continue;
            }
            try {
                return Integer.parseInt(value.trim());
            } catch (NumberFormatException e) {
                LOG.debug("Ignoring non-numeric {} value: {}", key, value);
            }
        }
        return 0;
    }

    protected static class ConfigState {
        private boolean jenkinsfileExists = false;
        private String jenkinsVersion = null;
        private Map<String, String> properties = Map.of();

        public void setJenkinsfileExists(boolean exists) {
            this.jenkinsfileExists = exists;
        }

        public boolean isJenkinsfileExists() {
            return jenkinsfileExists;
        }

        public void setJenkinsVersion(String version) {
            this.jenkinsVersion = version;
        }

        public String getJenkinsVersion() {
            return jenkinsVersion;
        }

        public void setProperties(Map<String, String> properties) {
            this.properties = properties != null ? properties : Map.of();
        }

        public Map<String, String> getProperties() {
            return properties;
        }
    }
}
