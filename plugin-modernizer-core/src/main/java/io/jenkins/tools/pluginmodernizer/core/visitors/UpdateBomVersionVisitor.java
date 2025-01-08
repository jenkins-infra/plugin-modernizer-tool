package io.jenkins.tools.pluginmodernizer.core.visitors;

import io.jenkins.tools.pluginmodernizer.core.config.RecipesConsts;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.openrewrite.ExecutionContext;
import org.openrewrite.maven.MavenDownloadingException;
import org.openrewrite.maven.MavenIsoVisitor;
import org.openrewrite.maven.internal.MavenPomDownloader;
import org.openrewrite.maven.table.MavenMetadataFailures;
import org.openrewrite.maven.tree.GroupArtifact;
import org.openrewrite.maven.tree.MavenMetadata;
import org.openrewrite.maven.tree.MavenResolutionResult;
import org.openrewrite.semver.LatestRelease;
import org.openrewrite.semver.Semver;
import org.openrewrite.xml.ChangeTagValueVisitor;
import org.openrewrite.xml.tree.Xml;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A visitor that updates the BOM version in a maven pom file.
 */
public class UpdateBomVersionVisitor extends MavenIsoVisitor<ExecutionContext> {

    /**
     * LOGGER.
     */
    private static final Logger LOG = LoggerFactory.getLogger(UpdateBomVersionVisitor.class);

    /**
     * The metadata failures from recipe
     */
    private final transient MavenMetadataFailures metadataFailures;

    /**
     * The version comparator for the bom
     */
    private final transient LatestRelease latestBomReleaseComparator =
            new LatestRelease(RecipesConsts.VERSION_METADATA_PATTERN);

    /**
     * Contructor
     */
    public UpdateBomVersionVisitor(MavenMetadataFailures metadataFailures) {
        this.metadataFailures = metadataFailures;
    }

    @Override
    public Xml.Document visitDocument(Xml.Document document, ExecutionContext ctx) {
        document = super.visitDocument(document, ctx);

        // Resolve artifact id
        Xml.Tag bomTag = getBomTag(document);
        if (bomTag == null) {
            LOG.info("Using bom, but not on the current pom (maybe on parent?)");
            return document;
        }
        Xml.Tag versionTag = bomTag.getChild("version").orElseThrow();
        Xml.Tag getProperties = getProperties(document);

        String artifactId =
                bomTag.getChild("artifactId").orElseThrow().getValue().orElseThrow();
        String version = bomTag.getChild("version").orElseThrow().getValue().orElseThrow();
        Xml.Tag versionProperty = null;
        if (version.contains("${")) {
            versionProperty = getProperties
                    .getChild(version.substring(2, version.length() - 1))
                    .orElse(null);
            version = versionProperty != null ? versionProperty.getValue().orElseThrow() : null;
        }
        LOG.debug("Updating bom version from {} to latest.release", version);
        if (artifactId.equals("bom-${jenkins.baseline}.x")) {
            artifactId =
                    "bom-" + getProperties.getChildValue("jenkins.baseline").orElseThrow() + ".x";
        }

        if (artifactId.contains("${")) {
            artifactId = getProperties
                    .getChildValue(artifactId.substring(2, artifactId.length() - 1))
                    .orElseThrow();
        }

        String newBomVersion = getLatestBomVersion(artifactId, version, ctx);
        if (newBomVersion == null) {
            LOG.debug("No newer version available for {}", artifactId);
            return document;
        }
        LOG.debug("Newer version available for {}: {}", artifactId, newBomVersion);

        // Change the property
        if (versionProperty != null) {
            return (Xml.Document)
                    new ChangeTagValueVisitor<>(versionProperty, newBomVersion).visitNonNull(document, ctx);
        }
        return (Xml.Document) new ChangeTagValueVisitor<>(versionTag, newBomVersion).visitNonNull(document, ctx);
    }

    /**
     * Find the newer bom version
     * @param artifactId The artifact id
     * @param currentVersion The current version
     * @param ctx The execution context
     * @return The newer bom version
     */
    public String getLatestBomVersion(String artifactId, String currentVersion, ExecutionContext ctx) {
        try {
            return getLatestBomVersion(artifactId, currentVersion, getResolutionResult(), ctx);
        } catch (MavenDownloadingException e) {
            LOG.warn("Failed to download metadata for {}", artifactId, e);
            return null;
        }
    }

    /**
     * Get the bom tag from the document
     * @param document The document
     * @return The bom tag
     */
    public static Xml.Tag getBomTag(Xml.Document document) {
        return document.getRoot()
                .getChild("dependencyManagement")
                .flatMap(dm -> dm.getChild("dependencies"))
                .flatMap(deps -> deps.getChildren("dependency").stream()
                        .filter(dep -> dep.getChildValue("groupId")
                                .map(RecipesConsts.PLUGINS_BOM_GROUP_ID::equals)
                                .orElse(false))
                        .findFirst())
                .orElse(null);
    }

    /**
     * Get the latest bom version
     * @param artifactId The artifact id
     * @param currentVersion The current version
     * @param mrr The maven resolution result
     * @param ctx The execution context
     * @return The latest
     */
    private String getLatestBomVersion(
            String artifactId, String currentVersion, MavenResolutionResult mrr, ExecutionContext ctx)
            throws MavenDownloadingException {

        // Since 'incrementals' repository is always enabled with -Pconsume-incrementals
        // the only way to exclude incrementals bom version is to exclude the repository
        MavenMetadata mavenMetadata = metadataFailures.insertRows(ctx, () -> (new MavenPomDownloader(ctx))
                .downloadMetadata(
                        new GroupArtifact(RecipesConsts.PLUGINS_BOM_GROUP_ID, artifactId),
                        null,
                        mrr.getPom().getRepositories().stream()
                                .filter(r -> !Objects.equals(r.getId(), RecipesConsts.INCREMENTAL_REPO_ID))
                                .toList()));

        // Keep track of version found
        List<String> versions = new ArrayList<>();
        for (String v : mavenMetadata.getVersioning().getVersions()) {
            if (latestBomReleaseComparator.isValid(currentVersion, v)) {
                versions.add(v);
            }
        }

        // Take latest version available. Allow to downgrade from incrementals to release
        if (!Semver.isVersion(currentVersion) && !versions.isEmpty() || (!versions.contains(currentVersion))) {
            versions.sort(latestBomReleaseComparator);
            return versions.get(versions.size() - 1);
        } else {
            return latestBomReleaseComparator.upgrade(currentVersion, versions).orElse(null);
        }
    }

    /**
     * Get the properties tag from the document
     * @param document The document
     * @return The properties tag
     */
    private Xml.Tag getProperties(Xml.Document document) {
        return document.getRoot().getChild("properties").orElseThrow();
    }
}
