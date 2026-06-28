package io.jenkins.tools.pluginmodernizer.core.campaign;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

/**
 * Top-level campaign definition loaded from YAML.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class CampaignDefinition {

    private CampaignPluginSource plugins = new CampaignPluginSource();
    private List<CampaignStage> stages;
    private CampaignExecution execution = new CampaignExecution();
    private CampaignOutput output = new CampaignOutput();

    public CampaignPluginSource getPlugins() {
        return plugins;
    }

    public void setPlugins(CampaignPluginSource plugins) {
        this.plugins = plugins;
    }

    public List<CampaignStage> getStages() {
        return stages;
    }

    public void setStages(List<CampaignStage> stages) {
        this.stages = stages;
    }

    public CampaignExecution getExecution() {
        return execution;
    }

    public void setExecution(CampaignExecution execution) {
        this.execution = execution;
    }

    public CampaignOutput getOutput() {
        return output;
    }

    public void setOutput(CampaignOutput output) {
        this.output = output;
    }
}
