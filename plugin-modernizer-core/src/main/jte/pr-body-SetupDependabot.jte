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

### Why Automate with Dependabot?

Managing dependencies can be a time-consuming and challenging task, especially when maintaining a Jenkins plugin. By automating this process with Dependabot, you open the door to a host of advantages:

- **Keep Pace with Innovation:** Dependabot ensures your plugin stays at the cutting edge by promptly updating it with the latest features and improvements from upstream libraries.

- **Strengthen Security:** Staying current with dependency updates is critical for security. Dependabot automatically flags and updates any vulnerable dependencies, helping to shield your plugin from potential exploits.

- **Save Time and Effort:** Spend less time managing dependencies and more time innovating. Dependabot automatically submits pull requests for new releases, allowing you to effortlessly review and integrate updates.

- **Enhance Compatibility and Stability:** Regular updates enhance both performance and compatibility with other plugins and the Jenkins core, ensuring a smooth experience for your users.

- **Focus on What Matters:** With Dependabot, you can concentrate on developing new features and fixing bugs, while it takes care of keeping everything up-to-date under the hood.

Setting up Dependabot exemplifies a proactive approach to modern software maintenance, aligning your plugin with the best practices in the industry. We invite you to test these updates and embrace a maintenance workflow that is efficient and secure.

Your insights and feedback are invaluable to us, and we'll be here to support you through this transition as needed.


