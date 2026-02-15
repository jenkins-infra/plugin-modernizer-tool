package io.jenkins.tools.pluginmodernizer.core.recipes;

import io.jenkins.tools.pluginmodernizer.core.extractor.ArchetypeCommonFile;
import io.jenkins.tools.pluginmodernizer.core.visitors.EnableCDVisitor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;
import org.intellij.lang.annotations.Language;
import org.openrewrite.ExecutionContext;
import org.openrewrite.ScanningRecipe;
import org.openrewrite.SourceFile;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.text.PlainTextParser;
import org.openrewrite.xml.tree.Xml;
import org.openrewrite.yaml.YamlParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Recipe to enable CD (JEP-229) in a Jenkins plugin.
 * Sets up continuous delivery workflow, updates Maven configuration, and transforms POM.
 */
public class EnableCD extends ScanningRecipe<EnableCD.ConfigState> {

    private static final Logger LOG = LoggerFactory.getLogger(EnableCD.class);

    @Language("yaml")
    private static final String CD_WORKFLOW_TEMPLATE = """
        # Note: additional setup is required, see https://www.jenkins.io/redirect/continuous-delivery-of-plugins
        #
        # Please find additional hints for individual trigger use case
        # configuration options inline this script below.
        #
        ---
        name: cd
        on:
          workflow_dispatch:
            inputs:
              validate_only:
                required: false
                type: boolean
                description: |
                  Run validation with release drafter only
                  → Skip the release job
                # Note: Change this default to true,
                #       if the checkbox should be checked by default.
                default: false
          # If you don't want any automatic trigger in general, then
          # the following check_run trigger lines should all be commented.
          # Note: Consider the use case #2 config for 'validate_only' below
          #       as an alternative option!
          check_run:
            types:
              - completed

        permissions:
          checks: read
          contents: write

        jobs:
          maven-cd:
            uses: jenkins-infra/github-reusable-workflows/.github/workflows/maven-cd.yml@v1
            with:
              # Comment / uncomment the validate_only config appropriate to your preference:
              #
              # Use case #1 (automatic release):
              #   - Let any successful Jenkins build trigger another release,
              #     if there are merged pull requests of interest
              #   - Perform a validation only run with drafting a release note,
              #     if manually triggered AND inputs.validate_only has been checked.
              #
              validate_only: ${{ inputs.validate_only == true }}
              #
              # Alternative use case #2 (no automatic release):
              #   - Same as use case #1 - but:
              #     - Let any check_run trigger a validate_only run.
              #       => enforce the release job to be skipped.
              #
              #validate_only: ${{ inputs.validate_only == true || github.event_name == 'check_run' }}
            secrets:
              MAVEN_USERNAME: ${{ secrets.MAVEN_USERNAME }}
              MAVEN_TOKEN: ${{ secrets.MAVEN_TOKEN }}
        """;

    @Language("yaml")
    public static final String DEPENDABOT_UPDATE_TEMPLATE = """
        - package-ecosystem: github-actions
          directory: /
          schedule:
            interval: monthly
        """;

    @Override
    public String getDisplayName() {
        return "Enable CD (JEP-229)";
    }

    @Override
    public String getDescription() {
        return "Enables continuous delivery (CD) using JEP-229 for automated plugin releases from pull requests.";
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
                    if (ArchetypeCommonFile.WORKFLOW_CD.same(sourceFile.getSourcePath())) {
                        LOG.debug(".github/workflows/cd.yaml already exists. Marking as present.");
                        state.setCdWorkflowExists(true);
                    }
                    if (ArchetypeCommonFile.MAVEN_CONFIG.same(sourceFile.getSourcePath())) {
                        LOG.debug(".mvn/maven.config already exists. Will be updated.");
                        state.setMavenConfigExists(true);
                    }
                    if (ArchetypeCommonFile.DEPENDABOT.same(sourceFile.getSourcePath())) {
                        LOG.debug(".github/dependabot.yml already exists. Will be updated.");
                        state.setDependabotExists(true);
                    }
                    if (ArchetypeCommonFile.RELEASE_DRAFTER_WORKFLOW.same(sourceFile.getSourcePath())) {
                        LOG.debug("Release drafter workflow exists. Will be deleted.");
                        state.setReleaseDrafterWorkflowExists(true);
                    }
                    if (ArchetypeCommonFile.POM.same(sourceFile.getSourcePath())) {
                        LOG.debug("POM file found. Will be processed by visitor.");
                        state.setPomExists(true);

                        // Check if POM has properties section
                        if (sourceFile instanceof Xml.Document) {
                            Xml.Document doc = (Xml.Document) sourceFile;
                            Xml.Tag root = doc.getRoot();
                            if (root != null && root.getChild("properties").isPresent()) {
                                LOG.debug("POM has properties section.");
                                state.setPomHasProperties(true);

                                // Check if already using CD format
                                root.getChild("version").ifPresent(versionTag -> {
                                    String currentVersion =
                                            versionTag.getValue().orElse("");
                                    
                                    boolean isValidCDFormat = currentVersion.equals("${changelist}")
                                            || currentVersion.equals("${revision}.${changelist}")
                                            || currentVersion.equals("${revision}-${changelist}");
                                    
                                    if (isValidCDFormat) {
                                        LOG.debug("POM version '{}' already uses valid CD format.", currentVersion);
                                        
                                        // Verify changelist property exists with SNAPSHOT value
                                        root.getChild("properties").ifPresent(props -> {
                                            if (!props.getChildren("changelist").isEmpty()) {
                                                String changelistValue = props.getChildren("changelist").get(0)
                                                        .getValue().orElse("");
                                                if (changelistValue.contains("SNAPSHOT")) {
                                                    LOG.debug("POM has valid CD format with SNAPSHOT changelist. Marking as already using CD.");
                                                    state.setPomAlreadyUsesCDFormat(true);
                                                }
                                            }
                                        });
                                    }
                                });
                            } else {
                                LOG.debug("POM lacks properties section.");
                                state.setPomHasProperties(false);
                            }
                        }
                    }
                }
                return tree;
            }
        };
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(ConfigState state) {
        return new EnableCDVisitor(state);
    }

    @Override
    public Collection<SourceFile> generate(ConfigState state, ExecutionContext ctx) {
        if (!state.isPomExists()) {
            LOG.warn("No pom.xml found. Cannot generate CD workflow files.");
            return Collections.emptyList();
        }

        if (!state.isPomHasProperties()) {
            LOG.warn("POM lacks a properties section. Cannot generate CD workflow files.");
            return Collections.emptyList();
        }

        Collection<SourceFile> generatedFiles = new ArrayList<>();

        // Generate CD workflow if it doesn't exist
        if (!state.isCdWorkflowExists()) {
            LOG.debug("Generating .github/workflows/cd.yaml");
            generatedFiles.addAll(YamlParser.builder()
                    .build()
                    .parse(CD_WORKFLOW_TEMPLATE)
                    .map(brandNewFile ->
                            (SourceFile) brandNewFile.withSourcePath(ArchetypeCommonFile.WORKFLOW_CD.getPath()))
                    .collect(Collectors.toList()));
        }

        // Generate maven.config if it doesn't exist
        if (!state.isMavenConfigExists()) {
            LOG.debug("Generating .mvn/maven.config");
            String mavenConfig = "-Dchangelist.format=%d.v%s";
            generatedFiles.addAll(PlainTextParser.builder()
                    .build()
                    .parse(mavenConfig)
                    .map(brandNewFile ->
                            (SourceFile) brandNewFile.withSourcePath(ArchetypeCommonFile.MAVEN_CONFIG.getPath()))
                    .collect(Collectors.toList()));
        }

        return generatedFiles;
    }

    /**
     * Configuration state for the recipe
     */
    public static class ConfigState {
        private boolean cdWorkflowExists = false;
        private boolean mavenConfigExists = false;
        private boolean dependabotExists = false;
        private boolean releaseDrafterWorkflowExists = false;
        private boolean pomExists = false;
        private boolean pomHasProperties = false;
        private boolean pomAlreadyUsesCDFormat = false;

        public boolean isCdWorkflowExists() {
            return cdWorkflowExists;
        }

        public void setCdWorkflowExists(boolean value) {
            cdWorkflowExists = value;
        }

        public boolean isMavenConfigExists() {
            return mavenConfigExists;
        }

        public void setMavenConfigExists(boolean value) {
            mavenConfigExists = value;
        }

        public boolean isDependabotExists() {
            return dependabotExists;
        }

        public void setDependabotExists(boolean value) {
            dependabotExists = value;
        }

        public boolean isReleaseDrafterWorkflowExists() {
            return releaseDrafterWorkflowExists;
        }

        public void setReleaseDrafterWorkflowExists(boolean value) {
            releaseDrafterWorkflowExists = value;
        }

        public boolean isPomExists() {
            return pomExists;
        }

        public void setPomExists(boolean value) {
            pomExists = value;
        }

        public boolean isPomHasProperties() {
            return pomHasProperties;
        }

        public void setPomHasProperties(boolean value) {
            pomHasProperties = value;
        }

        public boolean isPomAlreadyUsesCDFormat() {
            return pomAlreadyUsesCDFormat;
        }

        public void setPomAlreadyUsesCDFormat(boolean value) {
            pomAlreadyUsesCDFormat = value;
        }
    }
}
