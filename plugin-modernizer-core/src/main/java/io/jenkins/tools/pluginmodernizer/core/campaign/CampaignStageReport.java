package io.jenkins.tools.pluginmodernizer.core.campaign;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

/**
 * Result of a single stage execution for a plugin.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class CampaignStageReport {

    private String stageName;
    private String recipe;
    private boolean success;
    private String startedAt;
    private String finishedAt;
    private long durationMillis;
    private String localRepository;
    private List<String> modifiedFiles;
    private List<String> errors;

    public String getStageName() {
        return stageName;
    }

    public void setStageName(String stageName) {
        this.stageName = stageName;
    }

    public String getRecipe() {
        return recipe;
    }

    public void setRecipe(String recipe) {
        this.recipe = recipe;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
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

    public long getDurationMillis() {
        return durationMillis;
    }

    public void setDurationMillis(long durationMillis) {
        this.durationMillis = durationMillis;
    }

    public String getLocalRepository() {
        return localRepository;
    }

    public void setLocalRepository(String localRepository) {
        this.localRepository = localRepository;
    }

    public List<String> getModifiedFiles() {
        return modifiedFiles;
    }

    public void setModifiedFiles(List<String> modifiedFiles) {
        this.modifiedFiles = modifiedFiles;
    }

    public List<String> getErrors() {
        return errors;
    }

    public void setErrors(List<String> errors) {
        this.errors = errors;
    }
}
