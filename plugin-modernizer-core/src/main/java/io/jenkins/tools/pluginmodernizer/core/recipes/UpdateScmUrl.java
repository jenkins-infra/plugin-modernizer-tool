package io.jenkins.tools.pluginmodernizer.core.recipes;

import io.jenkins.tools.pluginmodernizer.core.visitors.UpdateScmUrlVisitor;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;

/**
 * A recipe that updates the SCM URL from git:// to https://.
 */
public class UpdateScmUrl extends Recipe {

    @Override
    public String getDisplayName() {
        return "Update SCM URLs from git:// to https://";
    }

    @Override
    public String getDescription() {
        return "Update the SCM URL in the SCM section of the POM file.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new UpdateScmUrlVisitor();
    }
}
