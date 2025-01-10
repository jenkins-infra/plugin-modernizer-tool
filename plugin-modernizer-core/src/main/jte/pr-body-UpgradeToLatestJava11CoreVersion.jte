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

### Transitional Step to Java 17
Upgrading to Jenkins LTS Core `2.462.3` is a critical step in our roadmap towards leveraging Java 17. This transition serves as an intermediary phase, ensuring that all systems are optimized for Java 11 before making the leap to Java 17.

### Enhancements and Security
By moving to this LTS version, we benefit from the latest performance and security improvements foundational to Java 11. Ensuring our dependencies are up-to-date and secure prepares the groundwork for a smoother transition to Java 17.

### Long-Term Support
The LTS version provides the necessary support and stability for Java 11, offering assurance during our gradual migration process. This allows us to take advantage of long-term updates and critical patches.

Embracing these incremental changes aligns our development environment with current standards and strategically positions us for future advancement. We appreciate your feedback and testing as we aim for a seamless adjustment period.
