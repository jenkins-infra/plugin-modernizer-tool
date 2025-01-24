@import io.jenkins.tools.pluginmodernizer.core.model.JDK
@import io.jenkins.tools.pluginmodernizer.core.model.Plugin
@import io.jenkins.tools.pluginmodernizer.core.model.Recipe
@import java.util.Objects
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

This pull request ensure your plugin is build on the Jenkins infrastructure.

Based on your plugin requirements, the Jenkins infrastructure will build your plugin using Java ${JDK.filter(plugin.getMetadata().getJdks(), 2).stream().map(Objects::toString).collect(java.util.stream.Collectors.joining(" and "))}.

@if(plugin.getMetadata().getJdks().stream().anyMatch(jdk -> jdk.getMajor() == 8 || jdk.getMajor() == 11))
There will come a time when we no longer support plugins built with JDK 8 or 11.

After this PR is merged, we will submit additional automated PRs to enable your plugin to build with Java 17 and 21.
@elseif(plugin.getMetadata().getJdks().stream().anyMatch(jdk -> jdk.getMajor() == 17 || jdk.getMajor() == 21))
Your plugin is already building with Java 17 and 21. We will continue to support these versions.
@endif
