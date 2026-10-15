package io.jenkins.tools.pluginmodernizer.core.utils;

import static org.assertj.core.api.Assertions.assertThat;

import io.jenkins.tools.pluginmodernizer.core.extractor.PluginMetadata;
import io.jenkins.tools.pluginmodernizer.core.model.Plugin;
import io.jenkins.tools.pluginmodernizer.core.model.Recipe;
import java.util.Set;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class CompositeRecipeDescriptionTest {
    @ParameterizedTest
    @CsvSource(
            delimiter = '|',
            value = {
                "MigrateCommonsLang2ToLang3AndCommonText | io.jenkins.tools.pluginmodernizer.core.recipes.UpdateParent | - Update the parent POM | - Update Commons Lang or Commons Text usages",
                "MigrateCommonsLang2ToLang3AndCommonText | org.openrewrite.java.ChangeType | - Update Commons Lang or Commons Text usages | - Add Commons API plugin dependencies",
                "MigrateCommonsLang2ToLang3AndCommonText | org.openrewrite.maven.AddDependency | - Add Commons API plugin dependencies | - Update Commons Lang or Commons Text usages",
                "MigrateToJava25 | io.jenkins.tools.pluginmodernizer.core.recipes.UpdateJenkinsfileForJavaVersion | - Update Jenkinsfile Java configuration | - Update Java code or compatibility settings",
                "MigrateToJava25 | org.openrewrite.java.migrate.UpgradeToJava17 | - Update Java code or compatibility settings | - Update Jenkinsfile Java configuration",
                "MigrateToJava25 | io.jenkins.tools.pluginmodernizer.core.recipes.UpdateParent | - Update Maven parent configuration | - Update Java code or compatibility settings",
                "MigrateJackson2To3 | org.openrewrite.java.dependencies.ChangeDependency | Update the dependency from | Adjust Java code",
                "MigrateJackson2To3 | org.openrewrite.java.jackson.UpgradeJackson_2_3 | Adjust Java code | Update the dependency from",
                "UpgradeToRecommendCoreVersion | io.jenkins.tools.pluginmodernizer.core.recipes.UpdateBom | ## Plugin dependencies | ## Jenkins baseline",
                "UpgradeToRecommendCoreVersion | io.jenkins.tools.pluginmodernizer.core.recipes.UpgradeJenkinsVersion | ## Jenkins baseline | ## Jenkinsfile changes",
                "UpgradeToRecommendCoreVersion | io.jenkins.tools.pluginmodernizer.core.recipes.CreateJenkinsFile | ## Jenkinsfile changes | ## Plugin dependencies"
            })
    void describesOnlyAppliedChanges(String name, String applied, String included, String omitted) {
        Plugin plugin = plugin();
        plugin.setAppliedRecipes(Set.of(applied));

        String body = TemplateUtils.renderPullRequestBody(plugin, recipe(name));

        assertThat(body).contains(included).doesNotContain(omitted);
    }

    @ParameterizedTest
    @CsvSource(
            delimiter = '|',
            value = {
                "MigrateCommonsLang2ToLang3AndCommonText | - Update the parent POM | - Update Commons Lang or Commons Text usages",
                "MigrateToJava25 | - Update Java code or compatibility settings | - Update Jenkinsfile Java configuration",
                "MigrateJackson2To3 | Update the dependency from | Adjust Java code",
                "UpgradeToRecommendCoreVersion | ## Jenkins baseline | ## Jenkinsfile changes",
                "UpgradeToLatestJava11CoreVersion | ## Jenkins baseline | 2.462.3",
                "BaseLineToJenkinsMinimumRequiredJava21Version | #### Jenkins baseline | Java 21 features"
            })
    void keepsFullDescriptionWhenResultsAreUnavailable(String name, String first, String second) {
        String body = TemplateUtils.renderPullRequestBody(plugin(), recipe(name));

        assertThat(body).contains(first, second);
    }

    @ParameterizedTest
    @CsvSource({"UpgradeToLatestJava11CoreVersion", "BaseLineToJenkinsMinimumRequiredJava21Version"})
    void omitsBaselineExplanationWhenOnlyOtherRecipesApplied(String name) {
        Plugin plugin = plugin();
        plugin.setAppliedRecipes(Set.of("io.jenkins.tools.pluginmodernizer.core.recipes.UpdateBom"));

        String body = TemplateUtils.renderPullRequestBody(plugin, recipe(name));

        assertThat(body).doesNotContain("# Jenkins baseline");
    }

    private Plugin plugin() {
        Plugin plugin = Plugin.build("example");
        PluginMetadata metadata = new PluginMetadata();
        metadata.setJenkinsVersion("2.555.1");
        plugin.setMetadata(metadata);
        return plugin;
    }

    private Recipe recipe(String name) {
        Recipe recipe = new Recipe();
        recipe.setName("io.jenkins.tools.pluginmodernizer." + name);
        recipe.setDisplayName(name);
        recipe.setDescription("Modernize the plugin.");
        return recipe;
    }
}
