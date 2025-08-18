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

This pull request upgrades `Apache Commons Lang 2` to `Apache Commons Lang 3` and migrates HTML escaping functionality from `Apache Commons Lang` to `Apache Commons Text`.
The `Apache Commons` community recommends using `Apache Commons Text` for string escaping operations, as it offers a more focused and feature-rich API for text processing.

## What's Changed?

- Migrated from deprecated/EOL `Apache Commons Lang 2` to `Commons Lang 3`.
- Transitioned from deprecated/EOL `StringEscapeUtils` in `Apache Commons Lang` to `Commons Text`.



