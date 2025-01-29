@import io.jenkins.tools.pluginmodernizer.core.model.Plugin
@import io.jenkins.tools.pluginmodernizer.core.model.Recipe
@param Plugin plugin
@param Recipe recipe
chore(pom): Use Java 11, require ${plugin.getMetadata().getJenkinsVersion()}
