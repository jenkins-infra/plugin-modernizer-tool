package io.jenkins.tools.pluginmodernizer.core.campaign;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.jenkins.tools.pluginmodernizer.core.model.ModernizerException;
import io.jenkins.tools.pluginmodernizer.core.utils.RecipeResolver;
import jakarta.inject.Inject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Loads and validates campaign definitions from YAML files.
 */
public class CampaignParser {

    private final ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());
    private final RecipeResolver recipeResolver;

    @Inject
    public CampaignParser(RecipeResolver recipeResolver) {
        this.recipeResolver = recipeResolver;
    }

    public CampaignDefinition parse(Path campaignFile) {
        Path normalized = campaignFile.toAbsolutePath().normalize();
        if (!Files.isRegularFile(normalized)) {
            throw new ModernizerException("Campaign file does not exist: " + normalized);
        }
        try {
            CampaignDefinition definition = objectMapper.readValue(normalized.toFile(), CampaignDefinition.class);
            normalize(definition);
            validate(definition, normalized);
            return definition;
        } catch (IOException e) {
            throw new ModernizerException("Failed to parse campaign file: " + normalized, e);
        }
    }

    private void normalize(CampaignDefinition definition) {
        if (definition.getPlugins() == null) {
            definition.setPlugins(new CampaignPluginSource());
        }
        if (definition.getPlugins().getFilters() == null) {
            definition.getPlugins().setFilters(new CampaignFilters());
        }
        if (definition.getExecution() == null) {
            definition.setExecution(new CampaignExecution());
        }
        if (definition.getOutput() == null) {
            definition.setOutput(new CampaignOutput());
        }
    }

    private void validate(CampaignDefinition definition, Path campaignFile) {
        CampaignPluginSource pluginSource = definition.getPlugins();
        boolean hasNames =
                pluginSource.getNames() != null && !pluginSource.getNames().isEmpty();
        boolean hasFile =
                pluginSource.getFile() != null && !pluginSource.getFile().isBlank();
        boolean hasLocalPaths = pluginSource.getLocalPaths() != null
                && !pluginSource.getLocalPaths().isEmpty();
        boolean hasTopPlugins = pluginSource.getTopPlugins() != null;

        if (!hasNames && !hasFile && !hasLocalPaths && !hasTopPlugins) {
            throw new ModernizerException(
                    "Campaign file must define at least one plugin source (names, file, localPaths, or topPlugins): "
                            + campaignFile);
        }
        if (pluginSource.getTopPlugins() != null && pluginSource.getTopPlugins() <= 0) {
            throw new ModernizerException("Campaign topPlugins must be greater than zero");
        }

        List<CampaignStage> stages = definition.getStages();
        if (stages == null || stages.isEmpty()) {
            throw new ModernizerException("Campaign file must define at least one stage: " + campaignFile);
        }
        for (CampaignStage stage : stages) {
            if (stage.getRecipe() == null || stage.getRecipe().isBlank()) {
                throw new ModernizerException("Campaign stage is missing a recipe name");
            }
            recipeResolver.resolve(stage.getRecipe());
        }

        if (definition.getExecution().getConcurrency() <= 0) {
            throw new ModernizerException("Campaign concurrency must be greater than zero");
        }
    }
}
