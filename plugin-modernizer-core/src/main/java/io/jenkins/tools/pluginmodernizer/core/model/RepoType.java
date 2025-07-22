package io.jenkins.tools.pluginmodernizer.core.model;

import io.jenkins.tools.pluginmodernizer.core.github.GHService;
import io.jenkins.tools.pluginmodernizer.core.utils.TemplateUtils;
import java.nio.file.Path;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import org.kohsuke.github.GHRepository;

/**
 * Enum to represent the type of repository.
 */
public enum RepoType {
    PLUGIN("plugin"),
    METADATA("metadata");

    /**
     * The type of repository.
     */
    private final String type;

    /**
     * Constructor to initialize the repository type.
     *
     * @param type the type of repository (e.g., "plugin" or "metadata")
     */
    RepoType(String type) {
        this.type = type;
    }

    /**
     * Get the type of the repository.
     *
     * @return the type of repository as a string
     */
    public String getType() {
        return type;
    }

    /**
     * Get the name of the plugin or the metadata repository
     *
     * @param plugin the plugin object
     * @return the name of the plugin or the metadata repository
     */
    public String getName(Plugin plugin) {
        return this == PLUGIN ? plugin.getName() : Plugin.METADATA_REPOSITORY_NAME;
    }

    /**
     * Get the repository name for the given repository type.
     *
     * @param plugin the plugin object
     * @return the repository name
     */
    public String getRepositoryName(Plugin plugin) {
        return this == PLUGIN ? plugin.getRepositoryName() : Plugin.METADATA_REPOSITORY_NAME;
    }

    /**
     * Get the local repository path for the given repository type.
     *
     * @param plugin the plugin object
     * @return the local repository path
     */
    public Path getLocalRepository(Plugin plugin) {
        return this == PLUGIN ? plugin.getLocalRepository() : plugin.getLocalMetadataRepository();
    }

    /**
     * Get the remote repository for the given repository type.
     *
     * @param plugin the plugin object
     * @param service the GitHub service instance
     * @return the remote repository object
     */
    public GHRepository getRemoteRepository(Plugin plugin, GHService service) {
        return this == PLUGIN ? plugin.getRemoteRepository(service) : plugin.getRemoteMetadataRepository(service);
    }

    /**
     * Get the branch name for the given repository type.
     *
     * @param plugin the plugin object
     * @param recipe the recipe object
     * @return the branch name
     */
    public String getBranchName(Plugin plugin, Recipe recipe) {
        return this == PLUGIN
                ? TemplateUtils.renderBranchName(plugin, recipe)
                : plugin.getName() + "-" + "modernization-metadata";
    }

    /**
     * Get the commit message for the given repository type.
     *
     * @param plugin the plugin object
     * @param recipe the recipe object
     * @return the commit message
     */
    public String getCommitMessage(Plugin plugin, Recipe recipe) {
        return this == PLUGIN
                ? TemplateUtils.renderCommitMessage(plugin, recipe)
                : "Add Modernization metadata for plugin " + plugin.getName();
    }

    /**
     * Check if the repository has commits.
     *
     * @param plugin the plugin object
     * @return true if the repository has commits, false otherwise
     */
    public boolean hasCommits(Plugin plugin) {
        return this == PLUGIN ? plugin.hasCommits() : plugin.hasMetadataCommits();
    }

    /**
     * Mark the repository type as having commits.
     *
     * @param plugin the plugin object
     * @return the updated plugin object
     */
    public Plugin withCommits(Plugin plugin) {
        return this == PLUGIN ? plugin.withCommits() : plugin.withMetadataCommits();
    }

    /**
     * Mark the repository type as not having commits.
     *
     * @param plugin the plugin object
     * @return the updated plugin object
     */
    public Plugin withoutCommits(Plugin plugin) {
        return this == PLUGIN ? plugin.withoutCommits() : plugin.withoutMetadataCommits();
    }

    /**
     * Check if changes have been pushed to the repository type.
     *
     * @param plugin the plugin object
     * @return true if changes have been pushed, false otherwise
     */
    public boolean hasChangesPushed(Plugin plugin) {
        return this == PLUGIN ? plugin.hasChangesPushed() : plugin.hasMetadataChangesPushed();
    }

    /**
     * Mark the repository type as having changes pushed.
     *
     * @param plugin the plugin object
     * @return the updated plugin object
     */
    public Plugin withChangesPushed(Plugin plugin) {
        return this == PLUGIN ? plugin.withChangesPushed() : plugin.withMetadataChangesPushed();
    }

    /**
     * Mark the repository type as having a pull request.
     *
     * @param plugin the plugin object
     * @return the updated plugin object
     */
    public Plugin withPullRequest(Plugin plugin) {
        return this == PLUGIN ? plugin.withPullRequest() : plugin.withMetadataPullRequest();
    }

    /**
     * Get the pull request title for the given repository type.
     *
     * @param plugin the plugin object
     * @param recipe the recipe object
     * @return the pull request title
     */
    public String getPrTitle(Plugin plugin, Recipe recipe) {
        return this == PLUGIN
                ? TemplateUtils.renderPullRequestTitle(plugin, recipe)
                : "Modernization-metadata for" + " " + plugin.getName();
    }

    /**
     * Get the pull request body for the given repository type.
     *
     * @param plugin the plugin object
     * @param recipe the recipe object
     * @return the pull request body
     */
    public String getPrBody(Plugin plugin, Recipe recipe) {
        return this == PLUGIN
                ? TemplateUtils.renderPullRequestBody(plugin, recipe)
                : "Modernization metadata for `" + plugin.getName() + "` at `" + ZonedDateTime.now(ZoneId.of("UTC"))
                        + "`" + "\n" + "PR: " + plugin.getPullRequestUrl();
    }
}
