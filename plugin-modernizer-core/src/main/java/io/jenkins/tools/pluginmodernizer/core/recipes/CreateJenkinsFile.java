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
        LOG.debug("Supported JDKs: {}", supportedJdks);

        if (supportedJdks.isEmpty()) {
            LOG.warn("No supported JDKs found for Jenkins version: {}", jenkinsVersion);
            return Collections.emptyList();
        }

        List<Integer> topJdkVersions = JDK.getTopTwoJdkVersions(supportedJdks);
        LOG.debug("Top two JDK versions: {}", topJdkVersions);
        String jenkinsfileContent = String.format(JENKINSFILE_TEMPLATE, topJdkVersions.get(0), topJdkVersions.get(1));
        LOG.debug(
                "Generated Jenkinsfile content with JDK versions: {} and {}",
                topJdkVersions.get(0),
                topJdkVersions.get(1));

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

    protected static class ConfigState {
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
