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

@if(plugin.shouldDescribeRecipe("io.jenkins.tools.pluginmodernizer.core.recipes.UpdateParent", "io.jenkins.tools.pluginmodernizer.core.recipes.UpgradeJenkinsVersion", "io.jenkins.tools.pluginmodernizer.core.recipes.UpdateJenkinsfileForJavaVersion", "io.jenkins.tools.pluginmodernizer.core.recipes.CreateJenkinsFile"))#### Jenkins baseline and build requirements

The Jenkins baseline or build configuration was updated. Check the changes to `pom.xml` and `Jenkinsfile` for the required Jenkins and Java versions.

@endif@if(plugin.shouldDescribeRecipe("io.jenkins.tools.pluginmodernizer.RemoveDevelopersTag"))#### Removing `developers` Tag from `pom.xml`

Jenkins no longer requires the `developers` tag in `pom.xml`, as the `developers` section was traditionally used to list individuals responsible for the plugin.
Instead, Jenkins now uses the Repository Permission Updater (RPU) to manage permissions and developer information.

##### Benefits of Removing `developers` Tag:

- **Simplification:** Removes unnecessary metadata from the `pom.xml`, resulting in a cleaner and more maintainable file.
- **Consistency:** Centralizes developer information management through the RPU, minimizing discrepancies.
- **Security:** Utilizes the RPU's controlled permission management, enhancing the security of artifact deployments.

Removing the `developers` tag aligns with modern Jenkins infrastructure standards and prevents outdated or redundant developer information from being included in plugin metadata.

@endif@if(plugin.shouldDescribeRecipe("io.jenkins.tools.pluginmodernizer.core.recipes.MigrateAcegiSecurityToSpringSecurity"))#### JEP-227: Replace Acegi Security with Spring Security

Migrating Jenkins plugin code from Acegi Security to Spring Security is important for several reasons:
- **Security updates**: Spring Security is the modern, actively maintained [successor to Acegi Security](https://github.com/jenkinsci/jep/blob/master/jep/227/README.adoc), providing up-to-date security features and patches.
- **Compatibility**: Jenkins core version 2.266 and later have replaced Acegi Security with Spring Security, so plugins need to be updated to remain compatible with newer Jenkins versions.
- **Eliminating false positives**: Security scanners often flag the outdated Acegi Security library as vulnerable, causing unnecessary concerns and exemption requests in security-conscious organizations.
- **Reducing technical debt**: The Acegi Security code in Jenkins was written over 13 years ago and has barely been touched since, making it difficult to maintain and understand.
- **Access to modern features**: Spring Security offers more current security implementations and features that weren't available in Acegi Security.
- **Community support**: With the Jenkins ecosystem moving to Spring Security, plugins using the newer library will benefit from better community support and compatibility with other plugins.
- **Simplified API**: The migration offers an opportunity to introduce a new simplified security API in Jenkins, potentially making it easier for plugin developers to work with security-related functions.

By migrating to Spring Security, plugin developers ensure their code remains compatible with current Jenkins versions, benefit from modern security features, and contribute to a more secure and maintainable Jenkins ecosystem.

@endif#### Summary

By upgrading, you'll be positioning your plugin at the forefront of performance, security, and user satisfaction. We encourage you to explore these updates and provide feedback. Let's continue to build a robust Jenkins ecosystem together!
