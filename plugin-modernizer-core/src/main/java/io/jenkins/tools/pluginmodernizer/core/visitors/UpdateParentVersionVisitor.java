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
 * A visitor that updates the parent version in a maven pom file.
 */
public class UpdateParentVersionVisitor extends MavenIsoVisitor<ExecutionContext> {

    /**
     * LOGGER.
     */
    private static final Logger LOG = LoggerFactory.getLogger(UpdateParentVersionVisitor.class);

    /**
     * The metadata failures from recipe
     */
    private final transient MavenMetadataFailures metadataFailures;

    /**
     * The major version to restrict updates to (nullable)
     */
    private final Integer majorVersion;

    /**
     * The version comparator for the parent
     */
    private final transient LatestRelease latestParentReleaseComparator =
            new LatestRelease(RecipesConsts.VERSION_METADATA_PATTERN);

    /**
     * Old parent version comparator that where not using JEP-229
     */
    private final transient LatestRelease oldParentReleaseComparator =
            new LatestRelease(RecipesConsts.OLD_PARENT_VERSION_PATTERN);

    /**
     * Contructor
     */
    public UpdateParentVersionVisitor(MavenMetadataFailures metadataFailures) {
        this.metadataFailures = metadataFailures;
        this.majorVersion = null;
    }

    /**
     * Contructor
     */
    public UpdateParentVersionVisitor(Integer majorVersion, MavenMetadataFailures metadataFailures) {
        this.metadataFailures = metadataFailures;
        this.majorVersion = majorVersion;
    }

    @Override
    public Xml.Document visitDocument(Xml.Document document, ExecutionContext ctx) {
        document = super.visitDocument(document, ctx);

        Xml.Tag parentTag = getParentTag(document);
        if (parentTag == null) {
            LOG.info("No parent found");
            return document;
        }

        Xml.Tag versionTag = parentTag.getChild("version").orElseThrow();
        String version = versionTag.getValue().orElseThrow();

        LOG.debug("Updating parent version from {} to latest.release", version);

        String newParentVersion = getLatestParentVersion(version, ctx);
        if (newParentVersion == null) {
            LOG.debug("No newer version available for parent plugin pom");
            return document;
        }
        LOG.debug("Newer version available for parent plugin pom: {}", newParentVersion);

        // Change the version
        return (Xml.Document) new ChangeTagValueVisitor<>(versionTag, newParentVersion).visitNonNull(document, ctx);
    }

    /**
     * Find the newer parent version
     * @param currentVersion The current version
     * @param ctx The execution context
     * @return The newer parent version
     */
    public String getLatestParentVersion(String currentVersion, ExecutionContext ctx) {
        try {
            return getLatestParentVersion(currentVersion, getResolutionResult(), ctx);
        } catch (MavenDownloadingException e) {
            LOG.warn("Failed to download metadata for parent pom", e);
            return null;
        }
    }

    /**
     * Get the parent tag from the document
     * @param document The document
     * @return The parent tag
     */
    public static Xml.Tag getParentTag(Xml.Document document) {
        return document.getRoot().getChild("parent").orElse(null);
    }

    /**
     * Get the latest parent version
     * @param currentVersion The current version
     * @param mrr The maven resolution result
     * @param ctx The execution context
     * @return The latest
     */
    private String getLatestParentVersion(String currentVersion, MavenResolutionResult mrr, ExecutionContext ctx)
            throws MavenDownloadingException {

        // Since 'incrementals' repository is always enabled with -Pconsume-incrementals
        // the only way to exclude incrementals parent version is to exclude the repository
        MavenMetadata mavenMetadata = metadataFailures.insertRows(ctx, () -> (new MavenPomDownloader(ctx))
                .downloadMetadata(
                        new GroupArtifact(RecipesConsts.PLUGIN_POM_GROUP_ID, "plugin"),
                        null,
                        mrr.getPom().getRepositories().stream()
                                .filter(r -> !Objects.equals(r.getId(), RecipesConsts.INCREMENTAL_REPO_ID))
                                .toList()));

        // Keep track of version found
        List<String> versions = new ArrayList<>();
        List<String> oldVersions = new ArrayList<>();
        for (String v : mavenMetadata.getVersioning().getVersions()) {
            if (latestParentReleaseComparator.isValid(currentVersion, v)) {
                versions.add(v);
            }
            if (oldParentReleaseComparator.isValid(currentVersion, v)) {
                oldVersions.add(v);
            }
        }

        // Apply major version filter if any (just check beginning of the version string)
        if (majorVersion != null) {
            String majorPrefix = majorVersion + ".";
            versions.removeIf(v -> !v.startsWith(majorPrefix));
            oldVersions.removeIf(v -> !v.startsWith(majorPrefix));
        }

        // Take latest version available. Allow to downgrade from incrementals to release
        if (!Semver.isVersion(currentVersion) && !versions.isEmpty() || (!versions.contains(currentVersion))) {
            versions.sort(latestParentReleaseComparator);
            oldVersions.sort(oldParentReleaseComparator);
            if (!versions.isEmpty()) {
                return versions.get(versions.size() - 1);
            }
            if (!oldVersions.isEmpty()) {
                return oldVersions.get(oldVersions.size() - 1);
            }
            return currentVersion;
        } else {
            return latestParentReleaseComparator
                    .upgrade(currentVersion, versions)
                    .orElse(null);
        }
    }
}
