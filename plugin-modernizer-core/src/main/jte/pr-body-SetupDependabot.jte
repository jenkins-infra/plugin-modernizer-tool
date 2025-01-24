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

Managing dependencies can be a time-consuming and challenging task, especially when maintaining a Jenkins plugin.
By automating this process with Dependabot, you gain numerous advantages:

- **Keep Pace with Innovation:** Dependabot ensures
your plugin remains cutting-edge by promptly incorporating the latest features and enhancements from upstream libraries.

- **Strengthen Security:** Staying up-to-date with dependency updates is crucial for security.
Dependabot automatically flags and updates any vulnerable dependencies,
helping to protect your plugin from potential exploits.

- **Save Time and Effort:** Spend less time managing dependencies and more time innovating.
Dependabot automatically submits pull requests for new releases,
allowing you to effortlessly review and integrate updates.

- **Enhance Compatibility and Stability:** Regular updates improve both performance and compatibility with other plugins and the Jenkins core,
ensuring a seamless experience for your users.

- **Improve GitHub Integration:** Dependabot streamlines and enhances the integration process with GitHub,
making it easier to manage updates.

- **Enhance the [plugin health score](https://plugins.jenkins.io/${plugin.getName()}/healthscore/):** Regular updates contribute to a better health score by ensuring your plugin is reliable and up-to-date.

- **Focus on What Matters:** With Dependabot, you can concentrate on developing new features and fixing bugs, knowing that it handles dependency updates efficiently.

Setting up Dependabot exemplifies a proactive approach to modern software maintenance, aligning your plugin with best practices in the industry. We invite you to test these updates and adopt a maintenance workflow that is both efficient and secure.

Your insights and feedback are invaluable to us, and we are here to support you through this transition as needed.
