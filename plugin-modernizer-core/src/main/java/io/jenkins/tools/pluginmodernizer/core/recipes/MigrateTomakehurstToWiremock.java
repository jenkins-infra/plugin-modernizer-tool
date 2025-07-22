package io.jenkins.tools.pluginmodernizer.core.recipes;

import io.jenkins.tools.pluginmodernizer.core.config.Settings;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.maven.MavenIsoVisitor;
import org.openrewrite.xml.ChangeTagValueVisitor;
import org.openrewrite.xml.tree.Xml;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A recipe to upgrade WireMock coordinates to org.wiremock as of WireMock 3 release.
 * Migrates groupId from com.github.tomakehurst to org.wiremock, updates artifactId to wiremock or wiremock-standalone,
 * and sets the version to the latest release.
 */
public class MigrateTomakehurstToWiremock extends Recipe {

    /**
     * Logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(MigrateTomakehurstToWiremock.class);

    @Override
    public String getDisplayName() {
        return "Migrate from com.github.tomakehurst to org.wiremock";
    }

    @Override
    public String getDescription() {
        return "Migrates Maven dependencies from com.github.tomakehurst:wiremock, wiremock-jre8-standalone to the corresponding artifact under the org.wiremock group (wiremock or wiremock-standalone) with the latest version.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new MavenIsoVisitor<>() {
            @Override
            public Xml.Tag visitTag(Xml.Tag tag, ExecutionContext ctx) {
                String wiremockVersion = Settings.getWiremockVersion();
                if (isDependencyTag()) {
                    String groupId = tag.getChildValue("groupId").orElse("");
                    String artifactId = tag.getChildValue("artifactId").orElse("");
                    if (groupId.equals("com.github.tomakehurst")
                            && (artifactId.equals("wiremock") || artifactId.equals("wiremock-jre8-standalone"))) {

                        // Update groupId
                        if (tag.getChild("groupId").isPresent()) {
                            doAfterVisit(new ChangeTagValueVisitor<>(
                                    tag.getChild("groupId").get(), "org.wiremock"));
                        }

                        // Update artifactId
                        String newArtifactId = artifactId.equals("wiremock") ? "wiremock" : "wiremock-standalone";
                        if (!artifactId.equals("wiremock")) {
                            if (tag.getChild("artifactId").isPresent()) {
                                doAfterVisit(new ChangeTagValueVisitor<>(
                                        tag.getChild("artifactId").get(), newArtifactId));
                            }
                        }

                        // Update version
                        if (tag.getChild("version").isPresent()) {
                            doAfterVisit(new ChangeTagValueVisitor<>(
                                    tag.getChild("version").get(), wiremockVersion));
                        }
                        maybeUpdateModel();

                        LOG.info("Migrated from com.github.tomakehurst to org.wiremock");
                    }
                }
                return super.visitTag(tag, ctx);
            }
        };
    }
}
