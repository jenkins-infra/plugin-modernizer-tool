package io.jenkins.tools.pluginmodernizer.core.campaign;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Execution settings for a campaign.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class CampaignExecution {

    private int concurrency = 1;
    private boolean continueOnFailure = true;
    private boolean skipMetadata;

    public int getConcurrency() {
        return concurrency;
    }

    public void setConcurrency(int concurrency) {
        this.concurrency = concurrency;
    }

    public boolean isContinueOnFailure() {
        return continueOnFailure;
    }

    public void setContinueOnFailure(boolean continueOnFailure) {
        this.continueOnFailure = continueOnFailure;
    }

    public boolean isSkipMetadata() {
        return skipMetadata;
    }

    public void setSkipMetadata(boolean skipMetadata) {
        this.skipMetadata = skipMetadata;
    }
}
