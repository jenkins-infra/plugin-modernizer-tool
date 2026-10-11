package io.jenkins.tools.pluginmodernizer.core.recipes;

import org.openrewrite.ExecutionContext;
import org.openrewrite.NlsRewrite;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.maven.MavenIsoVisitor;
import org.openrewrite.xml.RemoveContentVisitor;
import org.openrewrite.xml.tree.Xml;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Remove explicit versions from {@code dependencyManagement} entries when a Jenkins BOM is used.
 * <p>
 * {@link org.openrewrite.maven.RemoveRedundantDependencyVersions} only strips versions from
 * {@code dependencies}, leaving stale {@code dependencyManagement} overrides that prevent compilation.
 */
public class RemoveDependencyManagementVersionOverride extends Recipe {

    private static final Logger LOG = LoggerFactory.getLogger(RemoveDependencyManagementVersionOverride.class);

    @Override
    public @NlsRewrite.DisplayName String getDisplayName() {
        return "Remove dependencyManagement version overrides";
    }

    @Override
    public @NlsRewrite.Description String getDescription() {
        return "Remove explicit versions from dependencyManagement entries managed by the Jenkins BOM.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new IsUsingBom(true), new RemoveDependencyManagementVersionOverrideVisitor());
    }

    private static class RemoveDependencyManagementVersionOverrideVisitor extends MavenIsoVisitor<ExecutionContext> {

        @Override
        public Xml.Tag visitTag(Xml.Tag tag, ExecutionContext ctx) {
            if (isManagedDependencyTag() && "dependency".equals(tag.getName()) && !isBomImport(tag)) {
                tag.getChild("version").ifPresent(versionTag -> {
                    String groupId = tag.getChildValue("groupId").orElse("");
                    String artifactId = tag.getChildValue("artifactId").orElse("");
                    LOG.info("Removing dependencyManagement version override for {}:{}", groupId, artifactId);
                    doAfterVisit(new RemoveContentVisitor<>(versionTag, true, true));
                    maybeUpdateModel();
                });
            }
            return super.visitTag(tag, ctx);
        }

        private static boolean isBomImport(Xml.Tag dependency) {
            return "pom".equals(dependency.getChildValue("type").orElse(""))
                    && "import".equals(dependency.getChildValue("scope").orElse(""));
        }
    }
}
