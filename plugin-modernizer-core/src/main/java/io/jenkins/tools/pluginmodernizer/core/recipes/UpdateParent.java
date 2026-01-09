package io.jenkins.tools.pluginmodernizer.core.recipes;

import io.jenkins.tools.pluginmodernizer.core.visitors.UpdateParentVersionVisitor;
import org.openrewrite.ExecutionContext;
import org.openrewrite.NlsRewrite;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.maven.table.MavenMetadataFailures;

/**
 * A recipe that update the parent version to latest available.
 */
public class UpdateParent extends Recipe {

    @Override
    public @NlsRewrite.DisplayName String getDisplayName() {
        return "Update parent recipe";
    }

    @Override
    public @NlsRewrite.Description String getDescription() {
        return "Update parent recipe.";
    }

    /**
     * The minimum version.
     */
    @Option(displayName = "Version", description = "The major version to filter.", example = "5", required = false)
    Integer majorVersionFilter;

    public UpdateParent() {}

    public UpdateParent(Integer majorVersionFilter) {
        this.majorVersionFilter = majorVersionFilter;
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new UpdateParentVersionVisitor(majorVersionFilter, new MavenMetadataFailures(this));
    }
}
