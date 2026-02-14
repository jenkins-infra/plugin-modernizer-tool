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

## Enable CD (JEP-229)

This PR enables [JEP-229 Continuous Delivery](https://www.jenkins.io/doc/developer/publishing/releasing-cd/) for automated plugin releases.

### Changes

- Added `.github/workflows/cd.yaml` workflow for automated releases
- Updated `.mvn/maven.config` with `-Dchangelist.format=%d.v%s` for CD versioning
- Modified `pom.xml` to use `${'$'}{changelist}` version format with `999999-SNAPSHOT` value
- Configured Dependabot to keep GitHub Actions dependencies up to date

### Next Steps

After this PR is merged, a follow-up PR must be filed to [repository-permissions-updater](https://github.com/jenkins-infra/repository-permissions-updater/) to enable CD for this plugin by adding `cd: enabled: true` to `permissions/plugin-${plugin.getName()}.yml`.

Once approved, `MAVEN_TOKEN` and `MAVEN_USERNAME` secrets will be configured in the repository settings.

### Important

- Version numbers will use the format `123.vabcdef456789` (based on Git history depth)
- Releases happen automatically when PRs with labels like `enhancement`, `bug` or `breaking` are merged
- Manual releases can be triggered using the GitHub Actions workflow dispatch

**Note:** Do not merge this PR before the Jenkins infrastructure team reviews it.

See the [releasing cd doc](https://www.jenkins.io/doc/developer/publishing/releasing-cd/) for more details.
