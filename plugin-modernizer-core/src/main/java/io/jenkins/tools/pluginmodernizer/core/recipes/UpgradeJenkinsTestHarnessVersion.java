package io.jenkins.tools.pluginmodernizer.core.recipes;

import io.jenkins.tools.pluginmodernizer.core.config.Settings;
import io.jenkins.tools.pluginmodernizer.core.model.JDK;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.maven.ChangePropertyValue;
import org.openrewrite.maven.MavenIsoVisitor;
import org.openrewrite.maven.RemoveRedundantDependencyVersions;
import org.openrewrite.xml.tree.Xml;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Upgrade Jenkins Test Harness version to ensure compatibility with jenkins.version.
 */
public class UpgradeJenkinsTestHarnessVersion extends Recipe {
    /**
     * Logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(UpgradeJenkinsTestHarnessVersion.class);

    /**
     * The jenkins version.
     */
    @Option(displayName = "Version", description = "Jenkins version.", example = "2.440.3")
    String jenkinsVersion;

    /**
     * Constructor.
     * @param jenkinsVersion The Jenkins version.
     */
    public UpgradeJenkinsTestHarnessVersion(String jenkinsVersion) {
        this.jenkinsVersion = jenkinsVersion;
    }

    @Override
    public String getDisplayName() {
        return "Upgrade Jenkins Test Harness version";
    }

    @Override
    public String getDescription() {
        return "Upgrade Jenkins Test Harness version to ensure compatibility with jenkins.version.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new MavenIsoVisitor<>() {

            @Override
            public Xml.Document visitDocument(Xml.Document document, ExecutionContext ctx) {

                String latestCompatibleVersion = JDK.getLatestTestHarnessVersion(jenkinsVersion);

                if (latestCompatibleVersion == null) {
                    LOG.info(
                            "No compatible Jenkins Test Harness version found for Jenkins version {},  Defaulting to latest release.",
                            jenkinsVersion);
                    latestCompatibleVersion = Settings.getPluginVersion("jenkins-test-harness");
                }

                document = (Xml.Document)
                        new ChangePropertyValue("jenkins-test-harness.version", latestCompatibleVersion, false, false)
                                .getVisitor()
                                .visitNonNull(document, ctx);

                LOG.info("Removing redundant dependencies of jenkins-test-harness if any...");
                document = (Xml.Document) new RemoveRedundantDependencyVersions(
                                "org.jenkins-ci.main",
                                "jenkins-test-harness",
                                false,
                                RemoveRedundantDependencyVersions.Comparator.ANY,
                                null)
                        .getVisitor()
                        .visitNonNull(document, ctx);

                return super.visitDocument(document, ctx);
            }
        };
    }
}
