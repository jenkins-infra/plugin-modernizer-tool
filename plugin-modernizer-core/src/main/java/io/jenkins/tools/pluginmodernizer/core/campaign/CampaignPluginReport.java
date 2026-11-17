package io.jenkins.tools.pluginmodernizer.core.campaign;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

/**
 * Result of all stages executed for one plugin in a campaign.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class CampaignPluginReport {

    private String pluginName;
    private boolean local;
    private String source;
    private String finalLocalRepository;
    private boolean success;
    private List<CampaignStageReport> stages;

    public String getPluginName() {
        return pluginName;
    }

    public void setPluginName(String pluginName) {
        this.pluginName = pluginName;
    }

    public boolean isLocal() {
        return local;
    }

    public void setLocal(boolean local) {
        this.local = local;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getFinalLocalRepository() {
        return finalLocalRepository;
    }

    public void setFinalLocalRepository(String finalLocalRepository) {
        this.finalLocalRepository = finalLocalRepository;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public List<CampaignStageReport> getStages() {
        return stages;
    }

    public void setStages(List<CampaignStageReport> stages) {
        this.stages = stages;
    }
}
