package io.jenkins.tools.pluginmodernizer.core.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.Objects;
import java.util.Set;

/**
 * Our own representation of a recipe.
 */
public class Recipe implements Comparable<Recipe> {

    /**
     * Name of the recipe.
     */
    private String name;

    /**
     * Display name of the recipe.
     */
    private String displayName;

    /**
     * Description of the recipe.
     */
    private String description;

    /**
     * Tags of the recipe.
     */
    private Set<String> tags;

    /**
     * Return if the execution the recipe will be skipped
     * @return True if the recipe will skip verification
     */
    public boolean isSkipVerification() {
        return tags != null && tags.contains("skip-verification");
    }

    @JsonIgnore
    private Object type;

    @JsonIgnore
    private Object recipeList; // Use Object to avoid mapping complex nested structures.

    @JsonIgnore
    private Object preconditions; // Use Object to avoid mapping complex nested structures.

    @JsonIgnore
    private Boolean causesAnotherCycle;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Set<String> getTags() {
        return tags;
    }

    public void setTags(Set<String> tags) {
        this.tags = tags;
    }

    public Object getType() {
        return type;
    }

    public void setType(Object type) {
        this.type = type;
    }

    public Object getRecipeList() {
        return recipeList;
    }

    public void setRecipeList(Object recipeList) {
        this.recipeList = recipeList;
    }

    public Object getPreconditions() {
        return preconditions;
    }

    public void setPreconditions(Object preconditions) {
        this.preconditions = preconditions;
    }

    public Boolean getCausesAnotherCycle() {
        return causesAnotherCycle;
    }

    public void setCausesAnotherCycle(Boolean causesAnotherCycle) {
        this.causesAnotherCycle = causesAnotherCycle;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Recipe recipe = (Recipe) o;
        return Objects.equals(getName(), recipe.getName());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getName());
    }

    @Override
    public int compareTo(Recipe o) {
        return this.getName().compareTo(o.getName());
    }
}
