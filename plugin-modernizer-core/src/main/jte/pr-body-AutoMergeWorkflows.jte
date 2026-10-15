@import io.jenkins.tools.pluginmodernizer.core.model.Plugin
@import io.jenkins.tools.pluginmodernizer.core.model.Recipe
@param Plugin plugin
@param Recipe recipe
Hello `${plugin.getName()}` developers! :wave:

This is an automated pull request created by the [Jenkins Plugin Modernizer](https://github.com/jenkins-infra/plugin-modernizer-tool) tool. The tool has applied the following recipes to modernize the plugin:
<details aria-label="Recipe details for ${recipe.getDisplayName()}">
    <summary>${recipe.getDisplayName()}</summary>
    <p><em>${recipe.getName()}</em></p>
    <blockquote>${recipe.getDescription()}</blockquote>
</details>

## Setup auto-merge workflows

Add missing auto-merge workflows from the Jenkins plugin archetype for safe dependency updates (like parent POM or extensions).

Depending on the existing files, this recipe can add a workflow to merge safe dependency updates, a workflow to close passing BOM update PRs, or both.

## Why is this important?

Reduce maintenance burden and noise in automatic PRs by auto-merging safe dependency updates.

Closing BOM update PRs will ensure that the plugin depends on minimum plugins version for the given Jenkins baseline.

Bumping the BOM version is only required when the plugin needs a newer version of a plugin dependency.

See [Archetype](https://github.com/jenkinsci/archetypes/tree/master/common-files/.github/workflows) for more details.
