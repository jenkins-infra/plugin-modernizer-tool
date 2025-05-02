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

## Migrate from `com.github.tomakehurst` to `org.wiremock`

In 2023, [WireMock](https://wiremock.org) updated their `groupId` and `artifactId` with the major release of WireMock 3.
See [the announcement](https://www.wiremock.io/post/wiremock-3-goes-ga) for more details.

This PR migrates the `groupId` and `artifactId` to the new coordinates and updates to the latest available version.
This change allows receiving future updates to the dependency (e.g., via Dependabot or Renovate).

## Testing done

None. Relying on `ci.jenkins.io` to validate the change.
