package io.jenkins.tools.pluginmodernizer.cli.nativeimage;

import io.jenkins.tools.pluginmodernizer.core.extractor.ArchetypeCommonFile;
import io.jenkins.tools.pluginmodernizer.core.extractor.MetadataFlag;
import io.jenkins.tools.pluginmodernizer.core.extractor.MetadataXmlTag;
import io.jenkins.tools.pluginmodernizer.core.extractor.ModernizationMetadata;
import io.jenkins.tools.pluginmodernizer.core.extractor.PluginMetadata;
import io.jenkins.tools.pluginmodernizer.core.model.CacheEntry;
import io.jenkins.tools.pluginmodernizer.core.model.DetachedPlugins;
import io.jenkins.tools.pluginmodernizer.core.model.DiffStats;
import io.jenkins.tools.pluginmodernizer.core.model.HealthScoreData;
import io.jenkins.tools.pluginmodernizer.core.model.JDK;
import io.jenkins.tools.pluginmodernizer.core.model.OptOutPluginsData;
import io.jenkins.tools.pluginmodernizer.core.model.Platform;
import io.jenkins.tools.pluginmodernizer.core.model.PlatformConfig;
import io.jenkins.tools.pluginmodernizer.core.model.Plugin;
import io.jenkins.tools.pluginmodernizer.core.model.PluginInstallationStatsData;
import io.jenkins.tools.pluginmodernizer.core.model.PluginVersionData;
import io.jenkins.tools.pluginmodernizer.core.model.PreconditionError;
import io.jenkins.tools.pluginmodernizer.core.model.Recipe;
import io.jenkins.tools.pluginmodernizer.core.model.RepoType;
import io.jenkins.tools.pluginmodernizer.core.model.UpdateCenterData;
import io.quarkus.runtime.annotations.RegisterForReflection;
import org.openrewrite.table.SourcesFileResults;

/**
 * Data classes (de)serialized by reflection with Gson, Jackson or SnakeYAML.
 */
@RegisterForReflection(
        targets = {
            ArchetypeCommonFile.class,
            CacheEntry.class,
            DetachedPlugins.class,
            DiffStats.class,
            HealthScoreData.class,
            JDK.class,
            MetadataFlag.class,
            MetadataXmlTag.class,
            ModernizationMetadata.class,
            OptOutPluginsData.class,
            Platform.class,
            PlatformConfig.class,
            Plugin.class,
            PluginInstallationStatsData.class,
            PluginMetadata.class,
            PluginVersionData.class,
            PreconditionError.class,
            Recipe.class,
            RepoType.class,
            SourcesFileResults.Row.class,
            UpdateCenterData.class
        })
public class CoreReflectionConfiguration {}
