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

#### Why Upgrade to Java 17 and Jenkins 2.479.x?

- **Embrace Java 17 LTS Stability:** Benefit from long-term support with modern language features that improve development practice and plugin performance.

- **Harness Jenkins 2.479.x Innovations:** Stay aligned with the latest features and stability improvements, ensuring smooth integration and future-proofing.

- **Enhance Security:** Protect your plugin with up-to-date security fixes from both Java 17 and Jenkins core improvements.

- **Align with the Community:** Keep pace with ecosystem shifts towards Java 17, ensuring compatibility and expanding your plugin's user base.

- **Enjoy a Better Developer Experience:** Make the most of advanced tooling support and simplified dependency management with Java 17's enhancements.

#### Removing `developers` Tag from `pom.xml`

Jenkins no longer requires the `developers` tag in `pom.xml`, as the `developers` section was traditionally used to list individuals responsible for the plugin.
Instead, Jenkins now uses the Repository Permission Updater (RPU) to manage permissions and developer information.

##### Benefits of Removing `developers` Tag:

- **Simplification:** Removes unnecessary metadata from the `pom.xml`, resulting in a cleaner and more maintainable file.
- **Consistency:** Centralizes developer information management through the RPU, minimizing discrepancies.
- **Security:** Utilizes the RPU's controlled permission management, enhancing the security of artifact deployments.

Removing the `developers` tag aligns with modern Jenkins infrastructure standards and prevents outdated or redundant developer information from being included in plugin metadata.

#### Summary

By upgrading, you'll be positioning your plugin at the forefront of performance, security, and user satisfaction. We encourage you to explore these updates and provide feedback. Let's continue to build a robust Jenkins ecosystem together!
