package io.jenkins.tools.pluginmodernizer.core.model;

/**
 * The DiffStats record that represents
 * the number of additions, deletions and changed files
 */
public record DiffStats(int additions, int deletions, int changedFiles) {
    public DiffStats() {
        this(0, 0, 0); // Default values
    }
}
