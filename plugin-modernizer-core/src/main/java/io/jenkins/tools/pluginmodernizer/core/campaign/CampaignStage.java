package io.jenkins.tools.pluginmodernizer.core.campaign;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * A single campaign stage mapped to one modernization recipe.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class CampaignStage {

    private String name;
    private String recipe;
    private Boolean skipMetadata;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRecipe() {
        return recipe;
    }

    public void setRecipe(String recipe) {
        this.recipe = recipe;
    }

    public Boolean getSkipMetadata() {
        return skipMetadata;
    }

    public void setSkipMetadata(Boolean skipMetadata) {
        this.skipMetadata = skipMetadata;
    }
}
