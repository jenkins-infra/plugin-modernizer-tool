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

#### JUnit 5 migration

This recipe can make the following changes:

@if(plugin.shouldDescribeRecipe("io.jenkins.tools.pluginmodernizer.core.recipes.UpdateParent"))- Upgrade to the latest parent version
@endif@if(plugin.shouldDescribeRecipe("io.jenkins.tools.pluginmodernizer.core.recipes.EnsureRelativePath"))- Update the parent POM relative path
@endif@if(plugin.shouldDescribeRecipe("io.jenkins.tools.pluginmodernizer.core.recipes.AddProperty"))- Set `ban-junit4-imports.skip` to `false` to check for remaining JUnit 4 imports
@endif@if(plugin.shouldDescribeRecipe("org.openrewrite.java.testing.cleanup.TestsShouldNotBePublic"))- Remove public visibility from test classes and methods
@endif@if(plugin.shouldDescribeRecipe("io.jenkins.tools.pluginmodernizer.core.recipes.AnnotateWithJenkins"))- Add Jenkins-specific test annotations
@endif@if(plugin.shouldDescribeRecipe("org.openrewrite.java.testing.junit5.UpdateTestAnnotation"))- Update `@Test` annotations to JUnit 5
@endif@if(plugin.shouldDescribeRecipe("org.openrewrite.java.testing.junit5.UpdateBeforeAfterAnnotations"))- Convert `@Before`/`@After` to `@BeforeEach`/`@AfterEach`
@endif@if(plugin.shouldDescribeRecipe("org.openrewrite.java.ChangeType"))- Update test types and imports
@endif@if(plugin.shouldDescribeRecipe("org.openrewrite.java.testing.junit5.TemporaryFolderToTempDir"))- Migrate `TemporaryFolder` to `@TempDir`
@endif@if(plugin.shouldDescribeRecipe("org.openrewrite.java.testing.junit5.AssertToAssertions"))- Convert `Assert.*` to `Assertions.*`
@endif@if(plugin.shouldDescribeRecipe("org.openrewrite.java.testing.junit5.AssertThrowsOnLastStatement"))- Refactor `assertThrows` to handle exceptions on the last statement
@endif@if(plugin.shouldDescribeRecipe("org.openrewrite.java.testing.junit5.ExpectedExceptionToAssertThrows"))- Replace `ExpectedException` with `assertThrows`
@endif@if(plugin.shouldDescribeRecipe("org.openrewrite.java.testing.junit5.AssertTrueInstanceofToAssertInstanceOf"))- Use `assertInstanceOf` for type assertions
@endif@if(plugin.shouldDescribeRecipe("org.openrewrite.java.testing.junit5.RemoveTryCatchFailBlocks"))- Replace `try-catch` assertion blocks with `Assertions.assertDoesNotThrow`
@endif@if(plugin.shouldDescribeRecipe("org.openrewrite.java.ChangeMethodTargetToStatic"))- Update assertion method targets
@endif@if(plugin.shouldDescribeRecipe("org.openrewrite.java.UseStaticImport"))- Add static imports for assertion methods
@endif@if(plugin.shouldDescribeRecipe("org.openrewrite.java.testing.junit5.AddMissingTestBeforeAfterAnnotations"))- Add missing `@BeforeEach`/`@AfterEach` annotations
@endif@if(plugin.shouldDescribeRecipe("io.jenkins.tools.pluginmodernizer.core.recipes.AddThrowsExceptionWhereAssertThrows"))- Add `throws Exception` to test methods using `assertThrows`
@endif
