@import io.jenkins.tools.pluginmodernizer.core.model.Plugin
@import io.jenkins.tools.pluginmodernizer.core.model.Recipe
@param Plugin plugin
@param Recipe recipe

Hello `${plugin.getName()}` developers! :wave:

This is an automated pull request created by the [Jenkins Plugin Modernizer](https://github.com/jenkins-infra/plugin-modernizer-tool) tool. The tool has applied the following recipe to modernize the plugin:

<details aria-label="Recipe details for ${recipe.getDisplayName()}">
    <summary>${recipe.getDisplayName()}</summary>
    <p><em>${recipe.getName()}</em></p>
    <blockquote>${recipe.getDescription()}</blockquote>
</details>

#### Removing `developers` Tag from `pom.xml`

Jenkins no longer requires the `developers` tag in `pom.xml`, as the `developers` section was traditionally used to list individuals responsible for the plugin.
Instead, Jenkins now uses the Repository Permission Updater (RPU) to manage permissions and developer information.

#### Benefits of Removing `developers` Tag:

- **Simplification:** Removes unnecessary metadata from the `pom.xml`, resulting in a cleaner and more maintainable file.
- **Consistency:** Centralizes developer information management through the RPU, minimizing discrepancies.
- **Security:** Utilizes the RPU's controlled permission management, enhancing the security of artifact deployments.

Removing the `developers` tag aligns with modern Jenkins infrastructure standards and prevents outdated or redundant developer information from being included in plugin metadata.

