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

@if(plugin.shouldDescribeRecipe("io.jenkins.tools.pluginmodernizer.core.recipes.UpgradeJenkinsVersion", "io.jenkins.tools.pluginmodernizer.core.recipes.UpdateBom"))#### Jenkins baseline or plugin BOM

This recipe targets a baseline supporting Java 21 features and APIs. The Jenkins baseline or plugin BOM is updated; check `pom.xml` for the changes.
@endif
