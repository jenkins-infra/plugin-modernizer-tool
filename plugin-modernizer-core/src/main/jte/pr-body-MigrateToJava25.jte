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

## Java 25 migration

This recipe can make the following changes:

@if(plugin.shouldDescribeRecipe("org.openrewrite.java.migrate.UpgradeToJava17", "org.openrewrite.java.migrate.lang.ThreadStopUnsupported", "org.openrewrite.java.migrate.net.URLConstructorToURICreate", "org.openrewrite.java.migrate.util.SequencedCollection", "org.openrewrite.java.migrate.util.UseLocaleOf", "org.openrewrite.java.migrate.DeleteDeprecatedFinalize", "org.openrewrite.java.migrate.RemovedSubjectMethods", "org.openrewrite.java.migrate.AccessController", "org.openrewrite.java.migrate.RemoveSecurityPolicy", "org.openrewrite.java.migrate.RemoveSecurityManager", "org.openrewrite.java.migrate.SystemGetSecurityManagerToNull", "org.openrewrite.staticanalysis.RemoveExtraSemicolons", "org.openrewrite.staticanalysis.ReplaceDeprecatedRuntimeExecMethods"))- Update Java code or compatibility settings
@endif@if(plugin.shouldDescribeRecipe("io.jenkins.tools.pluginmodernizer.core.recipes.UpdateParent", "io.jenkins.tools.pluginmodernizer.core.recipes.EnsureRelativePath"))- Update Maven parent configuration
@endif@if(plugin.shouldDescribeRecipe("org.openrewrite.java.migrate.UpgradePluginsForJava21"))- Update build plugins for Java compatibility
@endif@if(plugin.shouldDescribeRecipe("org.openrewrite.github.SetupJavaUpgradeJavaVersion"))- Update Java versions in GitHub Actions
@endif@if(plugin.shouldDescribeRecipe("io.jenkins.tools.pluginmodernizer.core.recipes.CreateJenkinsFile", "io.jenkins.tools.pluginmodernizer.core.recipes.UpdateJenkinsfileForJavaVersion"))- Update Jenkinsfile Java configuration
@endif
