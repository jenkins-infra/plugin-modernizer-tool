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

Jenkins plugins declare a minimum supported Jenkins version.
The minimum Jenkins version is a useful way for plugin developers to indicate the range of Jenkins versions they are willing to support and test.
See the [developer documentation](https://www.jenkins.io/doc/developer/plugin-development/choosing-jenkins-baseline) to learn more about the recommended minimum Jenkins version.
The current minimum required Jenkins version is `${plugin.getMetadata().getJenkinsVersion()}`, which is why this pull request has been made.

If the plugin is already using the plugin bill of materials, then the bill of materials also needs to be updated with the matching `artifactId` for the minimum required Jenkins version.

## I can't see any change to the Jenkins version, how come?

This means your plugin POM was already using the `${plugin.getMetadata().getJenkinsVersion()}` version but still required a few adjustments.

Thanks for taking the time to review this PR. :pray:
