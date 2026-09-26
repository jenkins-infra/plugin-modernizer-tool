package io.jenkins.tools.pluginmodernizer.core.utils;

import io.jenkins.tools.pluginmodernizer.core.config.Settings;
import io.jenkins.tools.pluginmodernizer.core.model.Recipe;
import java.util.List;

/**
 * Resolves recipe names using either the fully qualified name or the short CLI name.
 */
public class RecipeResolver {

    public Recipe resolve(String value) {
        return Settings.AVAILABLE_RECIPES.stream()
                .filter(recipe -> recipe.getName().equals(value)
                        || recipe.getName()
                                .replace(Settings.RECIPE_FQDN_PREFIX + ".", "")
                                .equals(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Invalid recipe name: " + value));
    }

    public List<String> candidates() {
        return Settings.AVAILABLE_RECIPES.stream()
                .map(recipe -> recipe.getName().replace(Settings.RECIPE_FQDN_PREFIX + ".", ""))
                .toList();
    }
}
