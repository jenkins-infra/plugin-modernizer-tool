package io.jenkins.tools.pluginmodernizer.core.extractor;

import io.jenkins.tools.pluginmodernizer.core.impl.CacheManager;
import io.jenkins.tools.pluginmodernizer.core.model.CacheEntry;
import io.jenkins.tools.pluginmodernizer.core.model.JDK;
import io.jenkins.tools.pluginmodernizer.core.model.Platform;
import io.jenkins.tools.pluginmodernizer.core.model.PlatformConfig;
import io.jenkins.tools.pluginmodernizer.core.model.Plugin;
import io.jenkins.tools.pluginmodernizer.core.model.PreconditionError;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Metadata of a plugin extracted from its POM file or code
 */
public class PluginMetadata extends CacheEntry<PluginMetadata> {

    /**
     * Name of the plugin
     */
    private String pluginName;

    /**
     * List of flags present in the plugin
     */
    private Set<MetadataFlag> flags;

    /**
     * List of errors present in the plugin
     */
    private Set<PreconditionError> errors;

    /**
     * List of well known files present in the plugin
     */
    private List<ArchetypeCommonFile> commonFiles;

    /**
     * List of platforms extracted from Jenkinsfile
     */
    private List<PlatformConfig> platforms;

    /**
     * Use container agent for build extracted from Jenkinsfile
     */
    private Boolean useContainerAgent;

    /**
     * If the plugin is using container tests
     */
    private Boolean useContainerTests;

    /**
     * forkCount extracted from Jenkinsfile
     */
    private String forkCount;

    /**
     * Jenkins version required by the plugin
     */
    private String jenkinsVersion;

    /**
     * Parent version
     */
    private String parentVersion;

    /**
     * BOM version
     */
    private String bomVersion;

    /**
     * BOM artifact ID
     */
    private String bomArtifactId;

    /**
     * Properties defined in the POM file of the plugin
     */
    private Map<String, String> properties;

    /**
     * Create a new plugin metadata
     * Store the metadata in the relative target directory of current folder
     */
    public PluginMetadata() {
        super(
                new CacheManager(Path.of("target")),
                PluginMetadata.class,
                CacheManager.PLUGIN_METADATA_CACHE_KEY,
                Path.of("."));
    }

    /**
     * Create a new plugin metadata with the given key
     * @param key The key
     */
    public PluginMetadata(String key) {
        super(new CacheManager(Path.of("target")), PluginMetadata.class, key, Path.of("."));
    }

    /**
     * Create a new plugin metadata. Store the metadata at the root of the given cache manager
     * @param cacheManager The cache manager
     */
    public PluginMetadata(CacheManager cacheManager) {
        super(cacheManager, PluginMetadata.class, CacheManager.PLUGIN_METADATA_CACHE_KEY, cacheManager.root());
    }

    /**
     * Create a new plugin metadata. Store the metadata to the plugin subdirectory of the given cache manager
     * @param cacheManager The cache manager
     * @param plugin The plugin
     */
    public PluginMetadata(CacheManager cacheManager, Plugin plugin) {
        super(cacheManager, PluginMetadata.class, CacheManager.PLUGIN_METADATA_CACHE_KEY, Path.of(plugin.getName()));
    }

    public String getPluginName() {
        return pluginName;
    }

    public void setPluginName(String pluginName) {
        this.pluginName = pluginName;
    }

    public Set<MetadataFlag> getFlags() {
        return flags;
    }

    public void setFlags(Set<MetadataFlag> flags) {
        this.flags = flags;
    }

    public void addFlag(MetadataFlag flag) {
        if (flags == null) {
            flags = new HashSet<>();
        }
        flags.add(flag);
    }

    public void addFlags(Collection<MetadataFlag> flags) {
        if (this.flags == null) {
            this.flags = new HashSet<>();
        }
        this.flags.addAll(flags);
    }

    public boolean hasFlag(MetadataFlag flag) {
        return flags != null && flags.contains(flag);
    }

    public Set<PreconditionError> getErrors() {
        if (errors == null) {
            errors = new HashSet<>();
        }
        return Collections.unmodifiableSet(errors);
    }

    public void setErrors(Set<PreconditionError> errors) {
        this.errors = errors;
    }

    public List<ArchetypeCommonFile> getCommonFiles() {
        if (commonFiles == null) {
            commonFiles = new ArrayList<>();
        }
        return commonFiles;
    }

    public List<ArchetypeCommonFile> addCommonFile(ArchetypeCommonFile commonFiles) {
        if (this.commonFiles == null) {
            this.commonFiles = new ArrayList<>();
        }
        this.commonFiles.add(commonFiles);
        return this.commonFiles;
    }

    public boolean hasCommonFile(ArchetypeCommonFile commonFile) {
        return commonFiles != null && commonFiles.contains(commonFile);
    }

    public void setCommonFiles(List<ArchetypeCommonFile> commonFiles) {
        this.commonFiles = commonFiles;
    }

    public Set<JDK> getJdks() {
        if (platforms == null) {
            platforms = new LinkedList<>();
        }
        return platforms.stream().map(PlatformConfig::jdk).collect(HashSet::new, Set::add, Set::addAll);
    }

    public Set<Platform> getPlatforms() {
        if (platforms == null) {
            platforms = new LinkedList<>();
        }
        return platforms.stream().map(PlatformConfig::name).collect(HashSet::new, Set::add, Set::addAll);
    }

    /**
     * Set the JDK versions without platform information
     * @param jdkVersions The JDK versions
     */
    public void setJdks(Set<JDK> jdkVersions) {
        if (platforms == null) {
            platforms = new ArrayList<>();
        }
        platforms.addAll(jdkVersions.stream()
                .map(jdk -> new PlatformConfig(Platform.UNKNOWN, jdk, null, true))
                .toList());
    }

    public void setPlatforms(List<PlatformConfig> platforms) {
        this.platforms = platforms;
    }

    public void addPlatform(PlatformConfig platform) {
        if (platforms == null) {
            platforms = new ArrayList<>();
        }
        platforms.add(platform);
    }

    public void addPlatform(Platform platform, JDK jdk, String jenkins) {
        if (platforms == null) {
            platforms = new ArrayList<>();
        }
        platforms.add(new PlatformConfig(platform, jdk, jenkins, false));
    }

    public void addError(PreconditionError error) {
        if (errors == null) {
            errors = new HashSet<>();
        }
        errors.add(error);
    }

    public Boolean isUseContainerAgent() {
        return useContainerAgent;
    }

    public void setUseContainerAgent(Boolean useContainerAgent) {
        this.useContainerAgent = useContainerAgent;
    }

    public Boolean isUseContainerTests() {
        return Objects.requireNonNullElse(useContainerTests, false);
    }

    public void setUseContainerTests(Boolean useContainerTests) {
        this.useContainerTests = useContainerTests;
    }

    public String getForkCount() {
        return forkCount;
    }

    public void setForkCount(String forkCount) {
        this.forkCount = forkCount;
    }

    /**
     * The file with the given path or null if not found
     * @param path The path
     * @return The file or null
     */
    public ArchetypeCommonFile getFile(Path path) {
        return commonFiles.stream()
                .filter(f -> f.getPath().equals(path))
                .findFirst()
                .orElse(null);
    }

    /**
     * Check if the plugin has a file with the given path
     * @param path The path
     * @return True if the file is present
     */
    public boolean hasFile(Path path) {
        return commonFiles.stream().anyMatch(f -> f.getPath().equals(path));
    }

    /**
     * Check if the plugin has the given file
     * @param file The file
     * @return True if the file is present
     */
    public boolean hasFile(ArchetypeCommonFile file) {
        return commonFiles != null && commonFiles.contains(file);
    }

    public String getJenkinsVersion() {
        return jenkinsVersion;
    }

    public void setJenkinsVersion(String jenkinsVersion) {
        this.jenkinsVersion = jenkinsVersion;
    }

    public String getParentVersion() {
        return parentVersion;
    }

    public void setParentVersion(String parentVersion) {
        this.parentVersion = parentVersion;
    }

    public String getBomVersion() {
        return bomVersion;
    }

    public void setBomVersion(String bomVersion) {
        this.bomVersion = bomVersion;
    }

    public String getBomArtifactId() {
        return bomArtifactId;
    }

    public void setBomArtifactId(String bomArtifactId) {
        this.bomArtifactId = bomArtifactId;
    }

    public Map<String, String> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, String> properties) {
        this.properties = properties;
    }

    public void addProperty(String key, String value) {
        if (properties == null) {
            properties = new HashMap<>();
        }
        properties.put(key, value);
    }
}
