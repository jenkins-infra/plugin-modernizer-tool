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

## What's Changed?

We have introduced changes to improve the plugin's compatibility with `Java 25` and align it with modern Jenkins development standards

- Replaced deprecated API calls with their current equivalents where a clear migration path exists.
- Upgrade build files to use `Java 25` as the target/source.
- Upgrade plugins to versions that are compatible with `Java 25`.
- Add `Java 25` configuration in `Jenkinsfile` for testing on `ci.jenkins.io`.


