@import io.jenkins.tools.pluginmodernizer.core.model.Plugin
@import io.jenkins.tools.pluginmodernizer.core.model.Recipe
@param Plugin plugin
@param Recipe recipe
feat(java): Require Jenkins core ${plugin.getMetadata().getJenkinsVersion()} and Java 17.
