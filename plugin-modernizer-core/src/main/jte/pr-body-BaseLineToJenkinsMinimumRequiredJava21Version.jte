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

#### Why Upgrade to Java 21 and Jenkins 2.555.x?

- Use Java 21 feature and APIs on your plugin
