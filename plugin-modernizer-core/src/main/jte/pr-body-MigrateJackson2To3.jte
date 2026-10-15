@import io.jenkins.tools.pluginmodernizer.core.model.Plugin
@import io.jenkins.tools.pluginmodernizer.core.model.Recipe
@param Plugin plugin
@param Recipe recipe
This recipe migrates jackson2-api usage to jackson3-api.

@if(plugin.shouldDescribeRecipe("org.openrewrite.java.dependencies.ChangeDependency"))Update the dependency from `jackson2-api` to `jackson3-api`.

@endif@if(plugin.shouldDescribeRecipe("org.openrewrite.java.jackson.UpgradeJackson_2_3"))Adjust Java code for Jackson 3.
@endif
