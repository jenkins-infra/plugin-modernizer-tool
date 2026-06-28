package io.jenkins.tools.pluginmodernizer.core.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.jenkins.tools.pluginmodernizer.core.model.Recipe;
import org.junit.jupiter.api.Test;

class RecipeResolverTest {

    private final RecipeResolver resolver = new RecipeResolver();

    @Test
    void shouldResolveByShortName() {
        Recipe recipe = resolver.resolve("SetupDependabot");
        assertTrue(recipe.getName().endsWith("SetupDependabot"));
    }

    @Test
    void shouldResolveByFullyQualifiedName() {
        Recipe recipe = resolver.resolve("io.jenkins.tools.pluginmodernizer.SetupDependabot");
        assertEquals("io.jenkins.tools.pluginmodernizer.SetupDependabot", recipe.getName());
    }

    @Test
    void shouldThrowForUnknownRecipe() {
        assertThrows(IllegalArgumentException.class, () -> resolver.resolve("NotARealRecipe"));
    }

    @Test
    void shouldReturnNonEmptyCandidates() {
        assertFalse(resolver.candidates().isEmpty());
        assertTrue(resolver.candidates().contains("SetupDependabot"));
    }
}
