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

The Maven project object model ("POM") defines a parent project object model that provides default settings. An update of the parent POM will allow this plugin to compile with recent Java versions. The most recent parent POM files also provide additional static analysis like spotbugs and optional automated source code formatting.

More information can be found on this [Documentation](https://www.jenkins.io/doc/developer/tutorial-improve/update-parent-pom/).
