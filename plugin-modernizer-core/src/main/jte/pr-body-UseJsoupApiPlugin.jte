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

## Why is this important?

Depending on direct dependencies can cause classpath conflicts with other plugins, specially when multiple plugins depend on different versions of the same library.

Dynamically linking your plugin against an API plugin avoids multiple can avoid runtime conflicts and optimize multiple similar jar files loaded by Jenkins.

This PR ensure your plugin is linked against [Documentation](https://plugins.jenkins.io/jsoup/) instead of bundle direct dependency of `org.jsoup:jsoup`.