package io.jenkins.tools.pluginmodernizer.core.extractor;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.jenkins.tools.pluginmodernizer.core.impl.CacheManager;
import io.jenkins.tools.pluginmodernizer.core.model.CacheEntry;
import io.jenkins.tools.pluginmodernizer.core.model.Plugin;
import java.nio.file.Path;
import java.util.Set;

/**
 * Modernization metadata for a plugin extracted after executing the recipes
 */
public class ModernizationMetadata extends CacheEntry<ModernizationMetadata> {

    /**
     * Name of the plugin
     */
    private String pluginName;

    /**
     * Repository of the plugin
     */
    private String pluginRepository;

    /**
     * Version of the plugin
     */
    private String pluginVersion;

    /**
     * Baseline for the RPU
     */
    private String rpuBaseline;

    /**
     * Name of the migration
     */
    private String migrationName;

    /**
     * Description of the migration
     */
    private String migrationDescription;

    /**
     * Tags for the migration
     */
    private Set<String> tags;

    /**
     * Unique identifier for the migration
     */
    private String migrationId;

    /**
     * Number of deprecated APIs removed by the migration
     */
    private Integer removedDeprecatedApis;

    /**
     * Create a new modernization metadata
     * Store the metadata in the relative target directory of current folder
     */
    public ModernizationMetadata() {
        super(
                new CacheManager(Path.of("target")),
                ModernizationMetadata.class,
                CacheManager.MODERNIZATION_METADATA_CACHE_KEY,
                Path.of("."));
    }

    /**
     * Create a new modernization metadata with the given key
     * @param key The key
     */
    @JsonCreator
    public ModernizationMetadata(@JsonProperty("key") String key) {
        super(new CacheManager(Path.of("target")), ModernizationMetadata.class, key, Path.of("."));
    }

    /**
     * Create a new modernization metadata. Store the metadata at the root of the given cache manager
     * @param cacheManager The cache manager
     */
    public ModernizationMetadata(CacheManager cacheManager) {
        super(
                cacheManager,
                ModernizationMetadata.class,
                CacheManager.MODERNIZATION_METADATA_CACHE_KEY,
                cacheManager.root());
    }

    /**
     * Create a new modernization metadata. Store the metadata to the plugin subdirectory of the given cache manager
     * @param cacheManager The cache manager
     * @param plugin The plugin
     */
    public ModernizationMetadata(CacheManager cacheManager, Plugin plugin) {
        super(
                cacheManager,
                ModernizationMetadata.class,
                CacheManager.MODERNIZATION_METADATA_CACHE_KEY,
                Path.of(plugin.getName()));
    }

    public String getPluginRepository() {
        return pluginRepository;
    }

    public void setPluginRepository(String pluginRepository) {
        this.pluginRepository = pluginRepository;
    }

    public String getPluginName() {
        return pluginName;
    }

    public void setPluginName(String pluginName) {
        this.pluginName = pluginName;
    }

    public String getMigrationName() {
        return migrationName;
    }

    public void setMigrationName(String migrationName) {
        this.migrationName = migrationName;
    }

    public String getMigrationDescription() {
        return migrationDescription;
    }

    public void setMigrationDescription(String migrationDescription) {
        this.migrationDescription = migrationDescription;
    }

    public Integer getRemovedDeprecatedApis() {
        return removedDeprecatedApis;
    }

    public void setRemovedDeprecatedApis(Integer removedDeprecatedApis) {
        this.removedDeprecatedApis = removedDeprecatedApis;
    }

    public Set<String> getTags() {
        return tags;
    }

    public void setTags(Set<String> tags) {
        this.tags = tags;
    }

    public String getMigrationId() {
        return migrationId;
    }

    public void setMigrationId(String migrationId) {
        this.migrationId = migrationId;
    }

    public String getRpuBaseline() {
        return rpuBaseline;
    }

    public void setRpuBaseline(String rpuBaseline) {
        this.rpuBaseline = rpuBaseline;
    }

    public String getPluginVersion() {
        return pluginVersion;
    }

    public void setPluginVersion(String pluginVersion) {
        this.pluginVersion = pluginVersion;
    }
}
