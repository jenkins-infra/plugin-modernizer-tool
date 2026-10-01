package io.jenkins.tools.pluginmodernizer.core.campaign;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Optional filters applied to remote plugin selections.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class CampaignFilters {

    private Integer minInstallations;
    private Double maxHealthScore;
    private boolean excludeDeprecated;
    private boolean excludeApiPlugins;
    private boolean requireAdoptThisPlugin;
    private boolean excludeNoKnownInstallations;

    public Integer getMinInstallations() {
        return minInstallations;
    }

    public void setMinInstallations(Integer minInstallations) {
        this.minInstallations = minInstallations;
    }

    public Double getMaxHealthScore() {
        return maxHealthScore;
    }

    public void setMaxHealthScore(Double maxHealthScore) {
        this.maxHealthScore = maxHealthScore;
    }

    public boolean isExcludeDeprecated() {
        return excludeDeprecated;
    }

    public void setExcludeDeprecated(boolean excludeDeprecated) {
        this.excludeDeprecated = excludeDeprecated;
    }

    public boolean isExcludeApiPlugins() {
        return excludeApiPlugins;
    }

    public void setExcludeApiPlugins(boolean excludeApiPlugins) {
        this.excludeApiPlugins = excludeApiPlugins;
    }

    public boolean isRequireAdoptThisPlugin() {
        return requireAdoptThisPlugin;
    }

    public void setRequireAdoptThisPlugin(boolean requireAdoptThisPlugin) {
        this.requireAdoptThisPlugin = requireAdoptThisPlugin;
    }

    public boolean isExcludeNoKnownInstallations() {
        return excludeNoKnownInstallations;
    }

    public void setExcludeNoKnownInstallations(boolean excludeNoKnownInstallations) {
        this.excludeNoKnownInstallations = excludeNoKnownInstallations;
    }
}
