package io.jenkins.tools.pluginmodernizer.core.campaign;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

/**
 * Final structured report for a campaign execution.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class CampaignReport {

    private String campaignFile;
    private boolean dryRun;
    private int concurrency;
    private String startedAt;
    private String finishedAt;
    private int totalPlugins;
    private int successfulPlugins;
    private int failedPlugins;
    private int totalStages;
    private int successfulStages;
    private int failedStages;
    private String reportJson;
    private List<CampaignPluginReport> plugins;

    public String getCampaignFile() {
        return campaignFile;
    }

    public void setCampaignFile(String campaignFile) {
        this.campaignFile = campaignFile;
    }

    public boolean isDryRun() {
        return dryRun;
    }

    public void setDryRun(boolean dryRun) {
        this.dryRun = dryRun;
    }

    public int getConcurrency() {
        return concurrency;
    }

    public void setConcurrency(int concurrency) {
        this.concurrency = concurrency;
    }

    public String getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(String startedAt) {
        this.startedAt = startedAt;
    }

    public String getFinishedAt() {
        return finishedAt;
    }

    public void setFinishedAt(String finishedAt) {
        this.finishedAt = finishedAt;
    }

    public int getTotalPlugins() {
        return totalPlugins;
    }

    public void setTotalPlugins(int totalPlugins) {
        this.totalPlugins = totalPlugins;
    }

    public int getSuccessfulPlugins() {
        return successfulPlugins;
    }

    public void setSuccessfulPlugins(int successfulPlugins) {
        this.successfulPlugins = successfulPlugins;
    }

    public int getFailedPlugins() {
        return failedPlugins;
    }

    public void setFailedPlugins(int failedPlugins) {
        this.failedPlugins = failedPlugins;
    }

    public int getTotalStages() {
        return totalStages;
    }

    public void setTotalStages(int totalStages) {
        this.totalStages = totalStages;
    }

    public int getSuccessfulStages() {
        return successfulStages;
    }

    public void setSuccessfulStages(int successfulStages) {
        this.successfulStages = successfulStages;
    }

    public int getFailedStages() {
        return failedStages;
    }

    public void setFailedStages(int failedStages) {
        this.failedStages = failedStages;
    }

    public String getReportJson() {
        return reportJson;
    }

    public void setReportJson(String reportJson) {
        this.reportJson = reportJson;
    }

    public List<CampaignPluginReport> getPlugins() {
        return plugins;
    }

    public void setPlugins(List<CampaignPluginReport> plugins) {
        this.plugins = plugins;
    }
}
