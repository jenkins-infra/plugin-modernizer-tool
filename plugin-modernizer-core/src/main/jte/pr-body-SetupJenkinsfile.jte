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

Starting with the Jenkins 2.463 weekly release, Jenkins now requires Java 17 or newer.
The first Long-Term Support (LTS) release requiring Java 17 or newer (version 2.479.x) was released at the end of October 2024.

The Jenkins core team strongly recommends that all users adopt either Java 17 or Java 21.
The adoption of Java 17 has almost surpassed that of Java 11, and the usage of Java 21 is rapidly increasing.

There will come a time when we no longer support plugins built with JDK 8 or 11.

While this PR does not automatically make your plugin compatible with Java 17 or 21, it represents the first step towards a new era. Your plugin will be built and tested within the Jenkins infrastructure using Java 17 and 21.
After this PR is merged, we will submit additional automated PRs to enable your plugin to build with Java 17 and 21.
