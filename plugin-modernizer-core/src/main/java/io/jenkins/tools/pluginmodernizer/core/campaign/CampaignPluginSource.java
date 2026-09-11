package io.jenkins.tools.pluginmodernizer.core.campaign;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

/**
 * Plugin selection inputs for a campaign.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class CampaignPluginSource {

    private List<String> names;
    private String file;
    private List<String> localPaths;
    private Integer topPlugins;
    private CampaignFilters filters = new CampaignFilters();

    public List<String> getNames() {
        return names;
    }

    public void setNames(List<String> names) {
        this.names = names;
    }

    public String getFile() {
        return file;
    }

    public void setFile(String file) {
        this.file = file;
    }

    public List<String> getLocalPaths() {
        return localPaths;
    }

    public void setLocalPaths(List<String> localPaths) {
        this.localPaths = localPaths;
    }

    public Integer getTopPlugins() {
        return topPlugins;
    }

    public void setTopPlugins(Integer topPlugins) {
        this.topPlugins = topPlugins;
    }

    public CampaignFilters getFilters() {
        return filters;
    }

    public void setFilters(CampaignFilters filters) {
        this.filters = filters;
    }
}
