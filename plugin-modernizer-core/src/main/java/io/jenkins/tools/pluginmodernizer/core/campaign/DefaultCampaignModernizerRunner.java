package io.jenkins.tools.pluginmodernizer.core.campaign;

import com.google.inject.Guice;
import io.jenkins.tools.pluginmodernizer.core.GuiceModule;
import io.jenkins.tools.pluginmodernizer.core.config.Config;
import io.jenkins.tools.pluginmodernizer.core.impl.PluginModernizer;
import io.jenkins.tools.pluginmodernizer.core.model.Plugin;
import io.jenkins.tools.pluginmodernizer.core.model.PluginProcessingException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * Default stage runner backed by the existing PluginModernizer orchestration.
 */
public class DefaultCampaignModernizerRunner implements CampaignModernizerRunner {

    @Override
    public CampaignStageReport runStage(Config stageConfig, CampaignStage stage) {
        Instant startedAt = Instant.now();
        Plugin plugin = stageConfig.getPlugins().get(0);
        List<String> errors = new ArrayList<>();

        try {
            PluginModernizer modernizer =
                    Guice.createInjector(new GuiceModule(stageConfig)).getInstance(PluginModernizer.class);
            modernizer.validate();
            modernizer.start();
        } catch (Exception e) {
            errors.add(messageOf(e));
        }

        errors.addAll(plugin.getErrors().stream()
                .map(PluginProcessingException::getMessage)
                .collect(LinkedHashSet::new, LinkedHashSet::add, LinkedHashSet::addAll));
        if (plugin.hasPreconditionErrors()) {
            plugin.getPreconditionErrors().stream()
                    .map(Object::toString)
                    .forEach(errors::add);
        }

        Instant finishedAt = Instant.now();
        CampaignStageReport report = new CampaignStageReport();
        report.setStageName(stage.getName() != null && !stage.getName().isBlank()
                ? stage.getName()
                : stage.getRecipe());
        report.setRecipe(stageConfig.getRecipe().getName());
        report.setSuccess(errors.isEmpty());
        report.setStartedAt(startedAt.toString());
        report.setFinishedAt(finishedAt.toString());
        report.setDurationMillis(finishedAt.toEpochMilli() - startedAt.toEpochMilli());
        if (plugin.getLocalRepository() != null) {
            report.setLocalRepository(plugin.getLocalRepository().toAbsolutePath().toString());
        }
        report.setModifiedFiles(plugin.getModifiedFiles().stream().sorted(Comparator.naturalOrder()).toList());
        report.setErrors(List.copyOf(errors));
        return report;
    }

    private String messageOf(Exception e) {
        return e.getMessage() != null && !e.getMessage().isBlank() ? e.getMessage() : e.getClass().getName();
    }
}
