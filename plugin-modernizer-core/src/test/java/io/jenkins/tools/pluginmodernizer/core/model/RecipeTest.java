package io.jenkins.tools.pluginmodernizer.core.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

/**
 * Unit tests for {@link Recipe}.
 */
@Execution(ExecutionMode.CONCURRENT)
public class RecipeTest {

    @Test
    public void testToStringReturnsName() {
        Recipe recipe = new Recipe();
        recipe.setName("io.jenkins.tools.pluginmodernizer.UpgradeBomVersion");
        assertEquals("io.jenkins.tools.pluginmodernizer.UpgradeBomVersion", recipe.toString());
    }

    @Test
    public void testToStringWhenNameIsNull() {
        Recipe recipe = new Recipe();
        assertNull(recipe.toString());
    }
}
