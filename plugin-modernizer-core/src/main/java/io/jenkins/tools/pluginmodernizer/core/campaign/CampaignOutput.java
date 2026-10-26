package io.jenkins.tools.pluginmodernizer.core.campaign;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Output configuration for campaign artifacts.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class CampaignOutput {

    private String reportJson;

    public String getReportJson() {
        return reportJson;
    }

    public void setReportJson(String reportJson) {
        this.reportJson = reportJson;
    }
}
