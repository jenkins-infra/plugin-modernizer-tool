package io.jenkins.tools.pluginmodernizer.core.extractor;

import io.jenkins.tools.pluginmodernizer.core.impl.CacheManager;

/**
 * Replace the ExecutionContext by our own MetadataContext.
 * We are not planning to distribute recipes so we tolerate using execution context messaging.
 */
public class MetadataExecutionContext {
    private PluginMetadata mergedMetadata;
    private PluginMetadata jenkinsFileMetadata;
    private PluginMetadata pomMetadata;
    private PluginMetadata javaMetadata;
    private PluginMetadata commonMetadata;

    /**
     * Metadata file name for the merged metadata.
     */
    private final String fileName;

    /**
     * Constructor with the metadata file name.
     * @param fileName metadata file name
     */
    public MetadataExecutionContext(String fileName) {
        this.fileName = fileName;
    }

    /**
     * Default constructor with the default metadata file name.
     */
    public MetadataExecutionContext() {
        this.fileName = CacheManager.PLUGIN_METADATA_CACHE_KEY;
    }

    public PluginMetadata getMergedMetadata() {
        return mergedMetadata == null ? new PluginMetadata(fileName) : mergedMetadata;
    }

    public void setMergedMetadata(PluginMetadata mergedMetadata) {
        this.mergedMetadata = mergedMetadata;
        this.mergedMetadata.setKey(fileName);
    }

    public PluginMetadata getCommonMetadata() {
        return commonMetadata == null ? new PluginMetadata(fileName) : commonMetadata;
    }

    public void setCommonMetadata(PluginMetadata commonMetadata) {
        this.commonMetadata = commonMetadata;
        this.commonMetadata.setKey(fileName);
    }

    public PluginMetadata getJavaMetadata() {
        return javaMetadata == null ? new PluginMetadata(fileName) : javaMetadata;
    }

    public void setJavaMetadata(PluginMetadata javaMetadata) {
        this.javaMetadata = javaMetadata;
        this.javaMetadata.setKey(fileName);
    }

    public PluginMetadata getPomMetadata() {
        return pomMetadata == null ? new PluginMetadata(fileName) : pomMetadata;
    }

    public void setPomMetadata(PluginMetadata pomMetadata) {
        this.pomMetadata = pomMetadata;
        this.pomMetadata.setKey(fileName);
    }

    public PluginMetadata getJenkinsFileMetadata() {
        return jenkinsFileMetadata == null ? new PluginMetadata(fileName) : jenkinsFileMetadata;
    }

    public void setJenkinsFileMetadata(PluginMetadata jenkinsFileMetadata) {
        this.jenkinsFileMetadata = jenkinsFileMetadata;
        this.jenkinsFileMetadata.setKey(fileName);
    }
}
