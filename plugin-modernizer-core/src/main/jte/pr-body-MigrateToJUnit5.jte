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

#### This PR aims to migrate all tests to JUnit 5. Changes include:

**Note:** Some changes listed below may not apply if the plugin does not use certain features (e.g., Hamcrest assertions or TemporaryFolder).

- Migrating annotations and imports
- Migrating assertions
- Removing public visibility for test classes and methods
- Adding Jenkins-specific annotations where applicable
- Updating `@Test` annotations to JUnit 5
- Converting `@Before`/`@After` to `@BeforeEach`/`@AfterEach`
- Replacing `@Ignore` with `@Disabled`
- Migrating `TemporaryFolder` to `@TempDir`
- Converting `Assert.*` to `Assertions.*`
- Refactoring `assertThrows` to handle exceptions on the last statement
- Replacing `ExpectedException` with `assertThrows`
- Simplifying `assertTrue` with `instanceof` to `assertInstanceOf`
- Removing `try-catch` blocks with `Assertions.assertDoesNotThrow`
- Migrating `Assert.assertThat` to `Hamcrest MatcherAssert.assertThat`
- Adding static imports for `MatcherAssert.assertThat`
- Adding static imports for `Assertions.*` methods
- Adding missing `@BeforeEach`/`@AfterEach` annotations
- Minor clean-up

I am well aware that this is quite a large changeset; however, I hope that there is still interest in this PR and that it will be reviewed. If there are any questions, please do not hesitate to ping me.
