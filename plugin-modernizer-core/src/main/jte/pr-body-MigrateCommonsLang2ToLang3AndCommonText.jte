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

## Commons Lang and Text migration

This recipe can make the following changes:

@if(plugin.shouldDescribeRecipe("io.jenkins.tools.pluginmodernizer.core.recipes.UpdateParent"))- Update the parent POM
@endif@if(plugin.shouldDescribeRecipe("io.jenkins.tools.pluginmodernizer.core.recipes.AddProperty"))- Enable checks for remaining Commons Lang 2 usage
@endif@if(plugin.shouldDescribeRecipe("org.openrewrite.maven.AddDependency"))- Add Commons API plugin dependencies
@endif@if(plugin.shouldDescribeRecipe("org.openrewrite.java.ChangePackage", "org.openrewrite.java.ChangeType", "org.openrewrite.java.ChangeMethodName"))- Update Commons Lang or Commons Text usages
@endif
