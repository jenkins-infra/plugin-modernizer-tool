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

The Jenkins plugin parent POM can now detect obsolete `dependencyManagement` overrides.

Setting `<banObsoleteDependencyOverrides.skip>false</banObsoleteDependencyOverrides.skip>` opts the plugin into that enforcement so builds fail when a version override is no longer needed or no longer matches the BOM and plugin parent recommendations.

See https://github.com/jenkinsci/plugin-pom/pull/1381 for upstream details.
