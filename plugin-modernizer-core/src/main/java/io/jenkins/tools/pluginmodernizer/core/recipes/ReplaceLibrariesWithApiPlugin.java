package io.jenkins.tools.pluginmodernizer.core.recipes;

import java.util.Set;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.maven.AddDependencyVisitor;
import org.openrewrite.maven.MavenVisitor;
import org.openrewrite.maven.RemoveDependency;
import org.openrewrite.maven.tree.ResolvedDependency;
import org.openrewrite.xml.tree.Xml;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReplaceLibrariesWithApiPlugin extends Recipe {
    /**
     * Logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(ReplaceLibrariesWithApiPlugin.class);

    @Option(
            displayName = "API Plugin's groupId",
            description = "The first part of a dependency coordinate 'io.jenkins.plugins:ARTIFACT_ID:VERSION'.",
            example = "io.jenkins.plugins")
    String pluginGroupId;

    @Option(
            displayName = "API Plugin's artifactId",
            description = "The second part of a dependency coordinate 'GROUP_ID:jackson2-api:VERSION'.",
            example = "jackson2-api")
    String pluginArtifactId;

    @Option(
            displayName = "API Plugin's version",
            description = "An exact version number.",
            example = "1981.v17df70e84a_a_1")
    String pluginVersion;

    @Option(
            displayName = "Replaced Libraries",
            description = "The set of library coordinates replaced by this API Plugin.",
            example = "groupId: org.apache.commons\nartifactId: commons-text")
    Set<Library> replaces;

    /**
     * The groupId:artifactId combos to be replaced if present.
     */
    public static final class Library {
        private final String groupId;
        private final String artifactId;

        public Library(String groupId, String artifactId) {
            this.groupId = groupId;
            this.artifactId = artifactId;
        }
    }

    /**
     * Replaces a set of libraries with an api plugin.
     * @param pluginGroupId api plugin's groupId
     * @param pluginArtifactId api plugin's artifactId
     * @param pluginVersion api plugin's version
     * @param replaces set of libraries included in the api plugin
     */
    public ReplaceLibrariesWithApiPlugin(
            String pluginGroupId, String pluginArtifactId, String pluginVersion, Set<Library> replaces) {
        this.pluginGroupId = pluginGroupId;
        this.pluginArtifactId = pluginArtifactId;
        this.pluginVersion = pluginVersion;
        this.replaces = replaces;
    }

    @Override
    public String getDisplayName() {
        return "Use Jenkins API plugin instead of libraries";
    }

    @Override
    public String getDescription() {
        return "Prefer Jenkins API plugins over bundling libraries for slimmer plugins.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new MavenVisitor<ExecutionContext>() {
            @Override
            public Xml visitTag(Xml.Tag tag, ExecutionContext ctx) {
                if (isDependencyTag()) {
                    ResolvedDependency dependency = findDependency(tag);
                    if (dependency != null && !isApiPlugin(dependency)) {
                        for (Library replaced : replaces) {
                            String groupId = replaced.groupId;
                            String artifactId = replaced.artifactId;
                            ResolvedDependency found = dependency.findDependency(groupId, artifactId);
                            if (found == null) {
                                continue;
                            }
                            doAfterVisit(new AddDependencyVisitor(
                                    pluginGroupId,
                                    pluginArtifactId,
                                    pluginVersion,
                                    null,
                                    null,
                                    true,
                                    null,
                                    null,
                                    false,
                                    null));
                            doAfterVisit(new RemoveDependency(groupId, artifactId, null).getVisitor());
                        }
                    }
                }
                return super.visitTag(tag, ctx);
            }

            private boolean isApiPlugin(ResolvedDependency dependency) {
                return pluginGroupId.equals(dependency.getGroupId())
                        && pluginArtifactId.equals(dependency.getArtifactId());
            }
        };
    }
}
