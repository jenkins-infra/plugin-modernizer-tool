package io.jenkins.tools.pluginmodernizer.core.recipes;

import io.jenkins.tools.pluginmodernizer.core.extractor.ArchetypeCommonFileVisitor;
import io.jenkins.tools.pluginmodernizer.core.extractor.PluginMetadata;
import io.jenkins.tools.pluginmodernizer.core.extractor.PomResolutionVisitor;
import io.jenkins.tools.pluginmodernizer.core.model.JDK;
import io.jenkins.tools.pluginmodernizer.core.utils.JsonUtils;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import org.openrewrite.*;
import org.openrewrite.text.CreateTextFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CreateDynamicJenkinsFile extends ScanningRecipe<CreateDynamicJenkinsFile.ConfigState> {
    private static final Logger LOG = LoggerFactory.getLogger(CreateDynamicJenkinsFile.class);

    @Option(displayName = "Version", description = "The version.", example = "2.452.4")
    String minimumVersion;

    private static final String JENKINSFILE_TEMPLATE =
            """
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
    )
    """;

    public CreateDynamicJenkinsFile(String minimumVersion) {
        this.minimumVersion = minimumVersion;
    }

    @Override
    public String getDisplayName() {
        return "Create Dynamic Jenkinsfile";
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
                    if (PathUtils.matchesGlob(sourceFile.getSourcePath(), "**/Jenkinsfile")) {
                        LOG.debug("Jenkinsfile already exists");
                        state.setJenkinsfileExists(true);
                        return tree;
                    }

                    if (PathUtils.matchesGlob(sourceFile.getSourcePath(), "**/pom.xml")) {
                        LOG.debug("Visiting POM {}", sourceFile.getSourcePath());
                        PluginMetadata commonMetadata =
                                new ArchetypeCommonFileVisitor().reduce(tree, new PluginMetadata());
                        PluginMetadata pomMetadata = new PomResolutionVisitor().reduce(tree, commonMetadata);
                        LOG.debug("POM metadata: {}", JsonUtils.toJson(pomMetadata));

                        if (pomMetadata.getJenkinsVersion() != null) {
                            state.setJenkinsVersion(pomMetadata.getJenkinsVersion());
                        }
                    }
                }
                return tree;
            }
        };
    }

    @Override
    public Collection<SourceFile> generate(ConfigState state, ExecutionContext ctx) {
        if (state.isJenkinsfileExists()) {
            return Collections.emptyList();
        }

        // Calculate JDK versions
        String jenkinsVersion = state.getJenkinsVersion() != null ? state.getJenkinsVersion() : minimumVersion;
        List<JDK> supportedJdks = JDK.get(jenkinsVersion);

        if (supportedJdks.isEmpty()) {
            LOG.warn("No supported JDKs found for Jenkins version: {}", jenkinsVersion);
            return Collections.emptyList();
        }

        List<JDK> sortedJdks = supportedJdks.stream()
                .sorted((j1, j2) -> Integer.compare(j2.getMajor(), j1.getMajor()))
                .collect(Collectors.toList());

        int highestJdk = sortedJdks.get(0).getMajor();
        int nextJdk = sortedJdks.size() > 1 ? sortedJdks.get(1).getMajor() : highestJdk;

        // Create Jenkinsfile content
        String jenkinsfileContent = String.format(JENKINSFILE_TEMPLATE, highestJdk, nextJdk);

        // Create and return the new file
        CreateTextFile createJenkinsfile = new CreateTextFile(jenkinsfileContent, "Jenkinsfile", false);

        return createJenkinsfile.generate(new AtomicBoolean(true), ctx);
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(ConfigState state) {
        return new TreeVisitor<>() {
            @Override
            public Tree visit(Tree tree, ExecutionContext ctx) {
                return tree;
            }
        };
    }

    public static class ConfigState {
        private boolean jenkinsfileExists = false;
        private String jenkinsVersion = null;

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
    }
}
