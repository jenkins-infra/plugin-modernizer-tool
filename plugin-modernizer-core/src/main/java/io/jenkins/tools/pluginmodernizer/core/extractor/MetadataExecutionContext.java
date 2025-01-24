package io.jenkins.tools.pluginmodernizer.core.extractor;

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

    public PluginMetadata getMergedMetadata() {
        return mergedMetadata == null ? new PluginMetadata() : mergedMetadata;
    }

    public void setMergedMetadata(PluginMetadata mergedMetadata) {
        this.mergedMetadata = mergedMetadata;
    }

    public PluginMetadata getCommonMetadata() {
        return commonMetadata == null ? new PluginMetadata() : commonMetadata;
    }

    public void setCommonMetadata(PluginMetadata commonMetadata) {
        this.commonMetadata = commonMetadata;
    }

    public PluginMetadata getJavaMetadata() {
        return javaMetadata == null ? new PluginMetadata() : javaMetadata;
    }

    public void setJavaMetadata(PluginMetadata javaMetadata) {
        this.javaMetadata = javaMetadata;
    }

    public PluginMetadata getPomMetadata() {
        return pomMetadata == null ? new PluginMetadata() : pomMetadata;
    }

    public void setPomMetadata(PluginMetadata pomMetadata) {
        this.pomMetadata = pomMetadata;
    }

    public PluginMetadata getJenkinsFileMetadata() {
        return jenkinsFileMetadata == null ? new PluginMetadata() : jenkinsFileMetadata;
    }

    public void setJenkinsFileMetadata(PluginMetadata jenkinsFileMetadata) {
        this.jenkinsFileMetadata = jenkinsFileMetadata;
    }
}
