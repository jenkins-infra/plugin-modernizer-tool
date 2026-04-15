package io.jenkins.tools.pluginmodernizer.cli.converter;

import io.jenkins.tools.pluginmodernizer.core.model.Recipe;
import io.jenkins.tools.pluginmodernizer.core.utils.RecipeResolver;
import java.util.Iterator;
import picocli.CommandLine;

/**
 * Custom converter for Recipe interface.
 */
public final class RecipeConverter implements CommandLine.ITypeConverter<Recipe>, Iterable<String> {
    private final RecipeResolver recipeResolver = new RecipeResolver();

    @Override
    public Recipe convert(String value) {
        return recipeResolver.resolve(value);
    }

    @Override
    public Iterator<String> iterator() {
        return recipeResolver.candidates().iterator();
    }
}
