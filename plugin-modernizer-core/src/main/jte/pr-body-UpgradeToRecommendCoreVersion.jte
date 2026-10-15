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

@if(plugin.shouldDescribeRecipe("io.jenkins.tools.pluginmodernizer.core.recipes.UpgradeJenkinsVersion"))## Jenkins baseline

The minimum Jenkins version is updated to `${plugin.getMetadata().getJenkinsVersion()}`. See the [developer documentation](https://www.jenkins.io/doc/developer/plugin-development/choosing-jenkins-baseline) for guidance on choosing a baseline.

@endif@if(plugin.shouldDescribeRecipe("org.openrewrite.jenkins.AddPluginsBom", "io.jenkins.tools.pluginmodernizer.core.recipes.UpdateBom"))## Plugin dependencies

The Jenkins plugin BOM is added or updated to match the baseline.

@endif@if(plugin.shouldDescribeRecipe("io.jenkins.tools.pluginmodernizer.core.recipes.CreateJenkinsFile", "io.jenkins.tools.pluginmodernizer.core.recipes.UpdateJenkinsfileForJavaVersion"))## Jenkinsfile changes

Jenkins infrastructure builds pull requests using the Jenkinsfile from the default branch. If checks fail because they need the proposed Java configuration, a maintainer can replay the build with the updated Jenkinsfile.
@endif
