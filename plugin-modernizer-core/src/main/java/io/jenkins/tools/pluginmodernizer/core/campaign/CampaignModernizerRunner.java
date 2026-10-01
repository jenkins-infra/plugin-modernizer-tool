package io.jenkins.tools.pluginmodernizer.core.campaign;

import io.jenkins.tools.pluginmodernizer.core.config.Config;

/**
 * Runs one stage against one plugin using the existing PluginModernizer engine.
 */
public interface CampaignModernizerRunner {

    CampaignStageReport runStage(Config stageConfig, CampaignStage stage);
}
