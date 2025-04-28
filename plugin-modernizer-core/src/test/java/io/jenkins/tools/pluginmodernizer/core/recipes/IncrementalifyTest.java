package io.jenkins.tools.pluginmodernizer.core.recipes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

public class IncrementalifyTest {

    @Test
    void testIncrementalifyRecipe() {
        // Create the recipe
        Incrementalify recipe = new Incrementalify();

        // Verify the recipe's metadata
        assertEquals("Incrementalify recipe", recipe.getDisplayName());
        assertEquals(
                "Enables incrementals in a Jenkins plugin by running the incrementals:incrementalify goal.",
                recipe.getDescription());

        // Verify that the visitor is created
        assertNotNull(recipe.getVisitor());
    }
}
