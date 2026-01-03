package io.jenkins.tools.pluginmodernizer.core.github;

import io.jenkins.tools.pluginmodernizer.core.config.Config;
import io.jenkins.tools.pluginmodernizer.core.config.Settings;
import io.jenkins.tools.pluginmodernizer.core.model.DiffStats;
import io.jenkins.tools.pluginmodernizer.core.model.ModernizerException;
import io.jenkins.tools.pluginmodernizer.core.model.Plugin;
import io.jenkins.tools.pluginmodernizer.core.model.PluginProcessingException;
import io.jenkins.tools.pluginmodernizer.core.model.RepoType;
import io.jenkins.tools.pluginmodernizer.core.utils.JWTUtils;
import jakarta.inject.Inject;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.StreamSupport;
import org.apache.sshd.client.SshClient;
import org.apache.sshd.common.keyprovider.FileKeyPairProvider;
import org.apache.sshd.git.transport.GitSshdSessionFactory;
import org.eclipse.jgit.api.CommitCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ResetCommand;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.RefAlreadyExistsException;
import org.eclipse.jgit.api.errors.RefNotFoundException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.diff.Edit;
import org.eclipse.jgit.diff.EditList;
import org.eclipse.jgit.diff.RawTextComparator;
import org.eclipse.jgit.dircache.DirCacheIterator;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.errors.UnsupportedCredentialItem;
import org.eclipse.jgit.internal.signing.ssh.SshSigner;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.transport.CredentialItem;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.PushResult;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.SshSessionFactory;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.FileTreeIterator;
import org.kohsuke.github.GHApp;
import org.kohsuke.github.GHAppInstallationToken;
import org.kohsuke.github.GHBranchSync;
import org.kohsuke.github.GHFileNotFoundException;
import org.kohsuke.github.GHIssueState;
import org.kohsuke.github.GHMyself;
import org.kohsuke.github.GHOrganization;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GHRepositoryForkBuilder;
import org.kohsuke.github.GHUser;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.GitHubBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GHService {

    private static final Logger LOG = LoggerFactory.getLogger(GHService.class);

    /**
     * Allowed github tags for PR
     */
    private static final Set<String> ALLOWED_TAGS = Set.of("chore", "dependencies", "developer");

    @Inject
    private Config config;

    /**
     * The GitHub client
     */
    private GitHub github;

    /**
     * The GitHub App if connected by GitHub App
     */
    private GHApp app;

    /**
     * If the authentication is done using SSH key
     */
    private boolean sshKeyAuth = false;

    /**
     * Validate the configuration of the GHService
     */
    public void validate() {
        if (config.isFetchMetadataOnly()) {
            return;
        }
        setSshKeyAuth();
        if (Settings.GITHUB_TOKEN == null
                && (config.getGithubAppId() == null
                        || config.getGithubAppSourceInstallationId() == null
                        || config.getGithubAppTargetInstallationId() == null)) {
            throw new ModernizerException("Please set GH_TOKEN, GITHUB_TOKEN or configure GitHub app authentication.");
        }
        if (getGithubOwner() == null) {
            throw new ModernizerException(
                    "GitHub owner (username/organization) is not set. Please set GH_OWNER or GITHUB_OWNER environment variable. Or use --github-owner if running from CLI");
        }
        if (config.getGithubAppId() != null && config.getGithubAppSourceInstallationId() != null) {
            if (Settings.GITHUB_APP_PRIVATE_KEY_FILE == null) {
                throw new ModernizerException("GitHub App not configured. Please set GH_APP_PRIVATE_KEY_FILE");
            }
        }
    }

    public boolean isConnected() {
        return github != null;
    }

    /**
     * Connect to GitHub using the GitHub auth token
     */
    public void connect() {
        if (isConnected()) {
            return;
        }
        if (Settings.GITHUB_TOKEN == null
                && (config.getGithubAppId() == null
                        || config.getGithubAppSourceInstallationId() == null
                        || config.getGithubAppTargetInstallationId() == null)) {
            throw new ModernizerException("Please set GH_TOKEN, GITHUB_TOKEN or configure GitHub app authentication.");
        }
        try {

            // Connect with GitHub App
            if (config.getGithubAppId() != null
                    && config.getGithubAppSourceInstallationId() != null
                    && config.getGithubAppTargetInstallationId() != null) {
                LOG.debug("Connecting to GitHub using GitHub App...");
                LOG.debug("GitHub App ID: {}", config.getGithubAppId());
                LOG.debug("GitHub App Source Installation ID: {}", config.getGithubAppSourceInstallationId());
                LOG.debug("GitHub App Target Installation ID: {}", config.getGithubAppTargetInstallationId());
                LOG.debug("Private key file: {}", Settings.GITHUB_APP_PRIVATE_KEY_FILE);
                String jwtToken = JWTUtils.getJWT(config, Settings.GITHUB_APP_PRIVATE_KEY_FILE);

                // Get the GitHub App
                this.app = new GitHubBuilder().withJwtToken(jwtToken).build().getApp();
                GHAppInstallationToken appInstallationToken = this.app
                        .getInstallationById(config.getGithubAppSourceInstallationId())
                        .createToken()
                        .create();
                github = new GitHubBuilder()
                        .withEndpoint(config.getGithubApiUrl().toString())
                        .withAppInstallationToken(appInstallationToken.getToken())
                        .build();
                LOG.debug("Connected to GitHub using GitHub App");
            }
            // Connect with token
            else {
                LOG.debug("Connecting to GitHub using token...");
                github = new GitHubBuilder()
                        .withEndpoint(config.getGithubApiUrl().toString())
                        .withOAuthToken(Settings.GITHUB_TOKEN)
                        .build();
            }
            GHUser user = getCurrentUser();
            if (user == null) {
                throw new ModernizerException("Failed to get current user. Cannot use GitHub/SCM integration");
            }
            String email = getPrimaryEmail(user);
            if (email == null) {
                throw new ModernizerException(
                        "Email is not set in GitHub account. Please set email in GitHub account.");
            }
            LOG.debug("Connected to GitHub as {} <{}>", user.getName() != null ? user.getName() : user.getId(), email);

        } catch (IOException e) {
            throw new ModernizerException("Failed to connect to GitHub. Cannot use GitHub/SCM integration", e);
        }
        // Ensure to set up SSH client for Git operations
        setSshKeyAuth();
        if (sshKeyAuth) {
            try {
                SshClient client = SshClient.setUpDefaultClient();
                FileKeyPairProvider keyPairProvider =
                        new FileKeyPairProvider(Collections.singletonList(config.getSshPrivateKey()));
                client.setKeyIdentityProvider(keyPairProvider);
                GitSshdSessionFactory sshdFactory = new GitSshdSessionFactory(client);
                SshSessionFactory.setInstance(sshdFactory);
            } catch (Exception e) {
                throw new ModernizerException("Failed to set up SSH client for Git operations", e);
            }
        }
    }

    /**
     * Refresh the JWT token for the GitHub app. Only for GitHub App authentication
     *
     * @param installationId The installation ID
     */
    public void refreshToken(Long installationId) {
        if (installationId == null) {
            LOG.debug("Installation ID is not set. Skipping token refresh");
            return;
        }
        if (github == null) {
            throw new ModernizerException("GitHub client must be connected.");
        }
        try {
            String jwtToken = JWTUtils.getJWT(config, Settings.GITHUB_APP_PRIVATE_KEY_FILE);
            GHApp app = new GitHubBuilder().withJwtToken(jwtToken).build().getApp();
            GHAppInstallationToken appInstallationToken =
                    app.getInstallationById(installationId).createToken().create();
            github = new GitHubBuilder()
                    .withAppInstallationToken(appInstallationToken.getToken())
                    .build();
            this.app = app;
            LOG.debug("Refreshed token for GitHub App installation ID {}", installationId);
        } catch (IOException e) {
            throw new ModernizerException("Failed to refresh token", e);
        }
    }

    /**
     * Get the repository object for a plugin
     *
     * @param plugin The plugin to get the repository for
     * @return The GHRepository object
     */
    public GHRepository getRepository(Plugin plugin, RepoType repoType) {
        try {
            if (repoType == RepoType.PLUGIN) {
                return github.getRepository(Settings.ORGANIZATION + "/" + plugin.getRepositoryName());
            } else {
                return github.getRepository(Settings.METADATA_ORGANISATION + "/" + Settings.GITHUB_METADATA_REPOSITORY);
            }
        } catch (IOException e) {
            throw new PluginProcessingException("Failed to get" + repoType.getType() + "repository", e, plugin);
        }
    }

    /**
     * Get a plugin repository to the organization or personal account
     *
     * @param plugin The plugin
     * @param repoType The repo type
     * @return The GHRepository object
     */
    public GHRepository getRepositoryFork(Plugin plugin, RepoType repoType) {
        if (config.isDryRun()) {
            throw new PluginProcessingException(
                    "Cannot get" + repoType.getType() + "fork repository in dry-run mode", plugin);
        }
        try {
            if (repoType == RepoType.PLUGIN) {
                return github.getRepository(getGithubOwner() + "/" + plugin.getRepositoryName());
            } else {
                return github.getRepository(getGithubOwner() + "/" + Plugin.METADATA_REPOSITORY_NAME);
            }
        } catch (IOException e) {
            throw new PluginProcessingException("Failed to get" + repoType.getType() + "repository", e, plugin);
        }
    }

    /**
     * Check if the repository is forked to the organization or personal account
     *
     * @param plugin The plugin to check
     * @param repoType The repo type
     * @return True if the repository is forked
     */
    public boolean isForked(Plugin plugin, RepoType repoType) {
        if (plugin.isLocal()) {
            return false;
        }
        try {
            GHOrganization organization = getOrganization();
            if (organization != null) {
                return isRepositoryForked(organization, repoType.getRepositoryName(plugin));
            }
            return isRepositoryForked(repoType.getRepositoryName(plugin));
        } catch (IOException e) {
            throw new PluginProcessingException("Failed to check if repository is forked", e, plugin);
        }
    }

    /**
     * Check if the plugin repository is archived
     *
     * @param plugin The plugin to check
     * @return True if the repository is archived
     */
    public boolean isArchived(Plugin plugin) {
        if (plugin.isLocal()) {
            return false;
        }
        return plugin.getRemoteRepository(this).isArchived();
    }

    /**
     * Fork repository to the organization or personal account
     *
     * @param plugin The plugin
     * @param repoType The repo type to fork
     */
    public void fork(Plugin plugin, RepoType repoType) {
        if (config.isDryRun()) {
            LOG.info("Skipping forking {} {} in dry-run mode", repoType.getType(), plugin);
            return;
        }
        if (config.isFetchMetadataOnly()) {
            LOG.info("Skipping forking {} {} in fetch-metadata-only mode", repoType.getType(), plugin);
            return;
        }
        if (plugin.isArchived(this)) {
            LOG.info("Plugin {} is archived. Not forking {}", plugin, repoType.getType());
            return;
        }
        String repositoryName = repoType.getRepositoryName(plugin);
        LOG.info("Forking {} {} locally from repo {}...", repoType.getType(), plugin, repositoryName);
        try {
            GHRepository fork = forkRepoType(plugin, repoType);
            LOG.debug("Forked repository: {}", fork.getHtmlUrl());
        } catch (IOException | InterruptedException e) {
            plugin.addError("Failed to fork the" + repoType.getType() + "repository", e);
            plugin.raiseLastError();
        }

        Path localRepository = repoType.getLocalRepository(plugin);
        // Ensure to change the remote URL to the forked repository
        try (Git git = Git.open(localRepository.toFile())) {
            GHRepository fork = getRepositoryFork(plugin, repoType);
            URIish remoteUri = getRemoteUri(fork);
            git.remoteSetUrl().setRemoteName("origin").setRemoteUri(remoteUri).call();
            LOG.debug("Changed remote URL to forked repository {}", fork.getHtmlUrl());
        } catch (IOException | URISyntaxException | GitAPIException e) {
            plugin.addError("Failed to change remote URL to" + repoType.getType() + "forked repository", e);
            plugin.raiseLastError();
        }
    }

    /**
     * Fork repository to the organization or personal account
     *
     * @param plugin The plugin
     * @param repoType The repo type to fork
     * @throws IOException          Forking the repository failed due to I/O error
     * @throws InterruptedException Forking the repository failed due to interruption
     */
    private GHRepository forkRepoType(Plugin plugin, RepoType repoType) throws IOException, InterruptedException {
        GHOrganization organization = getOrganization();
        GHRepository originalRepo = repoType.getRemoteRepository(plugin, this);
        if (organization != null) {
            if (isRepositoryForked(organization, originalRepo.getName())) {
                LOG.debug(
                        "Repository of {} already forked to organization {}",
                        repoType.getType(),
                        organization.getLogin());
                GHRepository fork = getRepositoryFork(organization, originalRepo.getName());
                checkSameParentRepository(plugin, originalRepo, fork);
                return fork;
            } else {
                GHRepository fork = forkRepository(originalRepo, organization);
                Thread.sleep(5000); // Wait for the fork to be ready
                return fork;
            }
        } else {
            if (isRepositoryForked(originalRepo.getName())) {
                LOG.debug(
                        "Repository already forked to personal account {}",
                        getCurrentUser().getLogin());
                GHRepository fork = getRepositoryFork(originalRepo.getName());
                checkSameParentRepository(plugin, originalRepo, fork);
                return fork;
            } else {
                GHRepository fork = forkRepository(originalRepo);
                Thread.sleep(5000); // Wait for the fork to be ready
                return fork;
            }
        }
    }

    /**
     * Fork the repository
     *
     * @param originalRepo The original repository to fork
     * @param organization The organization to fork the repository to. Can be null for personal account
     * @return The forked repository
     * @throws IOException          If the fork operation failed
     * @throws InterruptedException If the fork operation was interrupted
     */
    private GHRepository forkRepository(GHRepository originalRepo, GHOrganization organization)
            throws IOException, InterruptedException {
        if (organization == null) {
            LOG.info(
                    "Forking the repository to personal account {}...",
                    getCurrentUser().getLogin());
            return fork(originalRepo, null);
        } else {
            LOG.info("Forking the repository to organisation {}...", organization.getLogin());
            return fork(originalRepo, organization);
        }
    }

    /**
     * Fork the default branch only
     *
     * @param originalRepo The original repository to fork
     * @param organization The organization to fork the repository to. Can be null for personal account
     * @return The forked repository
     * @throws IOException          If the fork operation failed
     * @throws InterruptedException If the fork operation was interrupted
     */
    private GHRepository fork(GHRepository originalRepo, GHOrganization organization)
            throws IOException, InterruptedException {
        GHRepositoryForkBuilder builder = originalRepo.createFork();
        if (organization != null) {
            builder.organization(organization);
        }
        builder.defaultBranchOnly(true);
        return builder.create();
    }

    /**
     * Fork the repository to the personal account
     *
     * @param originalRepo The original repository to fork
     * @return The forked repository
     * @throws IOException          If the fork operation failed
     * @throws InterruptedException If the fork operation was interrupted
     */
    private GHRepository forkRepository(GHRepository originalRepo) throws IOException, InterruptedException {
        return forkRepository(originalRepo, null);
    }

    /**
     * Get the organization object for the given owner or null if the owner is not an organization
     *
     * @return The GHOrganization object or null
     * @throws IOException If the organization access failed
     */
    private GHOrganization getOrganization() throws IOException {
        try {
            return github.getOrganization(getGithubOwner());
        } catch (GHFileNotFoundException e) {
            LOG.debug("Owner is not an organization: {}", config.getGithubOwner());
            return null;
        }
    }

    /**
     * Check if the repository is forked on the given organization
     *
     * @param organization The organization to check
     * @param repoName     The name of the repository
     * @return True if the repository is forked
     * @throws IOException If the repository access failed
     */
    private boolean isRepositoryForked(GHOrganization organization, String repoName) throws IOException {
        if (organization == null) {
            return false;
        }
        return getRepositoryFork(organization, repoName) != null;
    }

    /**
     * Get the forked repository on the given organization
     *
     * @param organization The organization to check
     * @param repoName     The name of the repository
     * @return The forked repository
     * @throws IOException If the repository access failed
     */
    private GHRepository getRepositoryFork(GHOrganization organization, String repoName) throws IOException {
        return organization.getRepository(repoName);
    }

    /**
     * Return if the repository is forked on current access
     *
     * @param repoName The name of the repository
     * @return True if the repository is forked
     * @throws IOException If the repository access failed
     */
    private boolean isRepositoryForked(String repoName) throws IOException {
        return getRepositoryFork(repoName) != null;
    }

    /**
     * Get the forked repository on the personal account
     *
     * @param repoName The name of the repository
     * @return The forked repository
     * @throws IOException If the repository access failed
     */
    private GHRepository getRepositoryFork(String repoName) throws IOException {
        return getCurrentUser().getRepository(repoName);
    }

    /**
     * Sync a fork repository from its original upstream. Only the main branch is synced in case multiple branches exist.
     *
     * @param plugin The plugin to sync
     * @param repoType The repo type
     */
    public void sync(Plugin plugin, RepoType repoType) {
        if (plugin.isLocal()) {
            LOG.info("Plugin {} is local. Not syncing {} repo", plugin, repoType.getType());
            return;
        }
        if (config.isDryRun()) {
            LOG.info("Skipping sync {} {} in dry-run mode", repoType.getType(), plugin);
            return;
        }
        if (config.isFetchMetadataOnly()) {
            LOG.info("Skipping sync {} {} in fetch-metadata-only mode", repoType.getType(), plugin);
            return;
        }
        String repoTypeCapitalised = repoType.getType().substring(0, 1).toUpperCase()
                + repoType.getType().substring(1);
        if (!isForked(plugin, repoType)) {
            LOG.info(
                    "{} {} is not forked. Not attempting sync of {} repo",
                    repoTypeCapitalised,
                    plugin,
                    repoType.getType());
            return;
        }
        try {
            syncRepository(getRepositoryFork(plugin, repoType));
            LOG.info("Synced the forked repository for {} {}", repoType.getType(), repoType.getName(plugin));
        } catch (IOException e) {
            plugin.addError("Failed to sync the" + repoType.getType() + "repository", e);
            plugin.raiseLastError();
        }
    }

    /**
     * Sync a fork repository from its original upstream. Only the main branch is synced in case multiple branches exist.
     *
     * @param forkedRepo Forked repository
     * @throws IOException if an error occurs while syncing the repository
     */
    private GHBranchSync syncRepository(GHRepository forkedRepo) throws IOException {
        LOG.debug("Syncing the forked repository {}", forkedRepo.getFullName());
        return forkedRepo.sync(forkedRepo.getDefaultBranch());
    }

    /**
     * Delete a plugin repository fork to the organization or personal account
     *
     * @param plugin The plugin of the fork to delete
     */
    public void deleteFork(Plugin plugin) {
        if (plugin.isLocal()) {
            LOG.info("Plugin {} is local. Not deleting fork", plugin);
            return;
        }
        if (config.isDryRun()) {
            LOG.info("Skipping delete fork for plugin {} in dry-run mode", plugin);
            return;
        }
        if (config.isFetchMetadataOnly()) {
            LOG.info("Skipping delete for for plugin {} in fetch-metadata-only mode", plugin);
            return;
        }
        if (!isForked(plugin, RepoType.PLUGIN)) {
            LOG.info("Plugin {} is not forked. Not attempting delete", plugin);
            return;
        }
        if (hasAnyPullRequestFrom(plugin)) {
            LOG.warn("Skipping delete fork for plugin {} as it has open pull requests", plugin);
            return;
        }
        GHRepository repository = getRepositoryFork(plugin, RepoType.PLUGIN);
        if (!repository.isFork()) {
            LOG.warn("Repository {} is not a fork. Not attempting delete", repository.getHtmlUrl());
            return;
        }
        if (repository.getOwnerName().equals(Settings.ORGANIZATION)) {
            LOG.warn("Not attempting to delete fork from organization {}", repository.getHtmlUrl());
            return;
        }
        if (config.isDebug()) {
            LOG.debug("Deleting fork for plugin {} from repo {}...", plugin, repository.getHtmlUrl());
        } else {
            LOG.info("Deleting fork for plugin {}...", plugin);
        }
        try {
            repository.delete();
            plugin.withoutCommits();
            plugin.withoutChangesPushed();
        } catch (IOException e) {
            plugin.addError("Failed to delete the fork", e);
            plugin.raiseLastError();
        }
    }

    /**
     * Fetch repository code from the fork or original repo in dry-run mode
     *
     * @param plugin The plugin
     * @param repoType The repo type to fetch
     */
    public void fetch(Plugin plugin, RepoType repoType) {
        if (plugin.isLocal()) {
            LOG.info("Plugin {} is local. Not fetching {} repo", plugin, repoType.getType());
            return;
        }

        // We always fetch from original repo to avoid forking when not necessary
        GHRepository repository = repoType.getRemoteRepository(plugin, this);

        if (config.isDebug()) {
            LOG.debug(
                    "Fetch {} code {} from {} into directory {}...",
                    repoType.getType(),
                    repoType.getName(plugin),
                    repository.getHtmlUrl(),
                    repoType.getRepositoryName(plugin));
        } else {
            LOG.info("Fetching {} code locally {}...", repoType.getType(), repoType.getName(plugin));
        }
        try {
            fetchRepository(plugin, repoType);
            LOG.debug(
                    "Fetched {} repository from {}",
                    repoType.getType(),
                    sshKeyAuth ? repository.getSshUrl() : repository.getHttpTransportUrl());
        } catch (GitAPIException | URISyntaxException e) {
            LOG.error("Failed to fetch the {} repository", repoType.getType(), e);
            plugin.addError("Failed to fetch the" + repoType.getType() + "repository", e);
            plugin.raiseLastError();
        }
    }

    /**
     * Fetch the repository code into local directory of the plugin
     *
     * @param plugin The plugin to fetch
     * @param repoType The repo type
     * @throws GitAPIException If the fetch operation failed
     */
    private void fetchRepository(Plugin plugin, RepoType repoType) throws GitAPIException, URISyntaxException {
        if (plugin.isLocal()) {
            LOG.info("Plugin {} is local. Not fetching {} repo", plugin, repoType.getType());
            return;
        }
        LOG.debug("Fetching {} {}", repoType.getName(plugin), repoType.getType());
        GHRepository repository = repoType.getRemoteRepository(plugin, this);
        Path localRepository = repoType.getLocalRepository(plugin);
        URIish remoteUri = getRemoteUri(repository);

        // Fetch latest changes
        if (Files.isDirectory(localRepository)) {
            String defaultBranch = repository.getDefaultBranch();
            // Ensure to set the correct remote, reset changes and pull
            try (Git git = Git.open(localRepository.toFile())) {
                git.remoteSetUrl()
                        .setRemoteName("origin")
                        .setRemoteUri(remoteUri)
                        .call();
                git.fetch()
                        .setCredentialsProvider(getCredentialProvider())
                        .setRemote("origin")
                        .call();
                LOG.debug("Resetting changes and pulling latest changes from {}", remoteUri);
                git.reset()
                        .setMode(ResetCommand.ResetType.HARD)
                        .setRef("origin/" + defaultBranch)
                        .call();
                git.clean().setCleanDirectories(true).setDryRun(false).call();
                Ref ref = git.checkout()
                        .setCreateBranch(false)
                        .setName(defaultBranch)
                        .call();
                git.pull()
                        .setCredentialsProvider(getCredentialProvider())
                        .setRemote("origin")
                        .setRemoteBranchName(defaultBranch)
                        .call();
                LOG.info("Fetched {} repository from {} to branch {}", repoType.getType(), remoteUri, ref.getName());
            } catch (RefNotFoundException e) {
                String message =
                        "Unable to find branch %s in repository. Probably the default branch was renamed. You can remove the local repository at %s and try again."
                                .formatted(defaultBranch, localRepository);
                LOG.error(message);
                plugin.addError(message);
                plugin.raiseLastError();
            } catch (IOException e) {
                plugin.addError("Failed fetch" + repoType.getType() + "repository", e);
                plugin.raiseLastError();
            }
        }
        // Clone the repository
        else {
            try {
                cloneRepository(plugin, remoteUri, localRepository.toFile());
            } catch (GitAPIException e) {
                if (e.getCause() instanceof org.apache.sshd.common.SshException) {
                    LOG.warn("SSH authentication failed. Retrying with HTTPS...");
                    remoteUri = new URIish(repository.getHttpTransportUrl());
                    try {
                        cloneRepository(plugin, remoteUri, localRepository.toFile());
                    } catch (GitAPIException ex) {
                        LOG.error("HTTPS clone failed: {}", ex.getMessage());
                        plugin.addError("Failed to fetch the" + repoType.getType() + "repository using HTTPS", ex);
                        plugin.raiseLastError();
                    }
                } else {
                    throw e;
                }
            }
        }
    }

    /**
     * Return the remote URI patched with default SSH 22 port required by apache mina sshd transport
     * @param repository The repository to get the remote URI for
     * @return The patched remote URI HTTP or SSH depending on config
     * @throws URISyntaxException If the URI is invalid
     */
    private URIish getRemoteUri(GHRepository repository) throws URISyntaxException {
        // Get the correct URI
        URIish remoteUri =
                sshKeyAuth ? new URIish(repository.getSshUrl()) : new URIish(repository.getHttpTransportUrl());

        // Ensure to set port 22 if not set on remote URL to work with apache mina sshd
        if (sshKeyAuth) {
            if (remoteUri.getScheme() == null) {
                remoteUri = remoteUri.setScheme("ssh");
                LOG.debug("Setting scheme ssh for remote URI {}", remoteUri);
            }
            if (remoteUri.getPort() == -1) {
                remoteUri = remoteUri.setPort(22);
                LOG.debug("Setting port 22 for remote URI {}", remoteUri);
            }
        }
        return remoteUri;
    }

    /**
     * Clone the repository to the given directory
     *
     * @param plugin The plugin
     * @param remoteUri The remote URI of the repository
     * @param directory The directory to clone the repository to
     * @throws GitAPIException If the clone operation failed
     */
    private void cloneRepository(Plugin plugin, URIish remoteUri, File directory) throws GitAPIException {
        try (Git git = Git.cloneRepository()
                .setCredentialsProvider(getCredentialProvider())
                .setRemote("origin")
                .setURI(remoteUri.toString())
                .setDirectory(directory)
                .call()) {
            LOG.debug("Clone successfully from {}", remoteUri);
        }
    }

    /**
     * Checkout the branch. Creates the branch if not exists
     *
     * @param plugin The plugin
     * @param repoType The repo type to checkout branch for
     */
    public void checkoutBranch(Plugin plugin, RepoType repoType) {
        if (plugin.isLocal()) {
            LOG.info("Plugin {} is local. Not checking out branch for {}", plugin, repoType.getType());
            return;
        }
        String branchName = repoType.getBranchName(plugin, config.getRecipe());
        Path localRepository = repoType.getLocalRepository(plugin);
        GHRepository remoteRepository = repoType.getRemoteRepository(plugin, this);
        try (Git git = Git.open(localRepository.toFile())) {
            try {
                git.checkout().setCreateBranch(true).setName(branchName).call();
            } catch (RefAlreadyExistsException e) {
                String defaultBranch = remoteRepository.getDefaultBranch();
                LOG.debug("Branch already exists. Checking out the branch");
                git.checkout().setName(branchName).call();
                git.reset()
                        .setMode(ResetCommand.ResetType.HARD)
                        .setRef(defaultBranch)
                        .call();
                LOG.debug(
                        "Reseted the branch to {} Checking out the branch to default branch {}",
                        branchName,
                        defaultBranch);
            }
        } catch (IOException | GitAPIException e) {
            plugin.addError("Failed to checkout branch for" + " " + repoType.getType(), e);
            plugin.raiseLastError();
        }
    }

    /**
     * Commit all changes in the repo type directory
     *
     * @param plugin The plugin
     * @param repoType The repo type to commit changes for
     */
    public void commitChanges(Plugin plugin, RepoType repoType) {
        Path localRepository = repoType.getLocalRepository(plugin);
        // Collect local changes
        if ((plugin.isLocal() || config.isDryRun()) && repoType == RepoType.PLUGIN) {
            try (Git git = Git.open(localRepository.toFile())) {
                Status status = git.status().call();
                plugin.addModifiedFiles(status.getUntracked());
                plugin.addModifiedFiles(status.getChanged());
                plugin.addModifiedFiles(status.getModified());
                plugin.addModifiedFiles(status.getMissing());
                plugin.addModifiedFiles(status.getRemoved());

                LOG.debug("Adding untracked files: {}", status.getUntracked());
                LOG.debug("Adding changed files: {}", status.getChanged());
                LOG.debug("Adding changed files: {}", status.getModified());
                LOG.debug("Adding missing files: {}", status.getMissing());
                LOG.debug("Adding removed files: {}", status.getRemoved());
            } catch (IOException | IllegalArgumentException | GitAPIException e) {
                plugin.addError("Failed to commit changes for" + " " + repoType.getType(), e);
                plugin.raiseLastError();
            }
        }

        if (plugin.isLocal()) {
            LOG.info("Plugin {} is local. Not committing changes for {}", plugin, repoType.getType());
            return;
        }
        if (config.isDryRun()) {
            LOG.info("Skipping commits changes for {} {} in dry-run mode", repoType.getType(), plugin);
            return;
        }
        if (plugin.isArchived(this)) {
            LOG.info("Plugin {} is archived. Not committing changes for {}", plugin, repoType.getType());
            return;
        }
        try (Git git = Git.open(localRepository.toFile())) {
            git.getRepository().scanForRepoChanges();
            String commitMessage = repoType.getCommitMessage(plugin, config.getRecipe());
            LOG.debug("Commit message: {}", commitMessage);
            Status status = git.status().call();
            LOG.debug("Untracked before commit: {}", status.getUntracked());
            LOG.debug("Untracked folder commit: {}", status.getUntrackedFolders());
            if (status.hasUncommittedChanges() || !status.getUntracked().isEmpty()) {
                LOG.debug("Changed files before commit: {}", status.getChanged());
                LOG.debug("Untracked before commit: {}", status.getUntracked());
                LOG.debug("Missing before commit {}", status.getMissing());
                // Stage deleted file
                for (String file : status.getMissing()) {
                    git.rm().addFilepattern(file).call();
                }
                // Add the rest of the files
                git.add().addFilepattern(".").call();
                status = git.status().call();
                LOG.debug("Added files after staging: {}", status.getAdded());
                LOG.debug("Changed files to after staging: {}", status.getChanged());
                LOG.debug("Removed files to after staging: {}", status.getRemoved());
                if (repoType == RepoType.PLUGIN) {
                    plugin.addModifiedFiles(status.getAdded());
                    plugin.addModifiedFiles(status.getChanged());
                    plugin.addModifiedFiles(status.getRemoved());
                }
                GHUser user = getCurrentUser();
                String email = getPrimaryEmail(user);
                CommitCommand commit = git.commit()
                        .setAuthor(user.getName() != null ? user.getName() : String.valueOf(user.getId()), email)
                        .setMessage(commitMessage);
                signCommit(commit).call();
                LOG.debug("Changes committed for {} {}", repoType.getType(), plugin.getName());
                repoType.withCommits(plugin);

            } else {
                LOG.debug("No changes to commit for {} {}", repoType.getType(), plugin.getName());
            }
        } catch (IOException | IllegalArgumentException | GitAPIException e) {
            plugin.addError("Failed to commit" + repoType.getType() + "changes", e);
            plugin.raiseLastError();
        }
    }

    /**
     * Sign the commit using SSH key. Set not sign if using GH_TOKEN
     *
     * @param commit The commit command to sign
     * @return The signed commit command
     * @throws IOException If the SSH key reading failed
     */
    private CommitCommand signCommit(CommitCommand commit) throws IOException {
        if (sshKeyAuth) {
            LOG.debug("Signing commit using SSH key");
            commit.setSign(true);
            commit.setSigner(new SshSigner());
            commit.setSigningKey(config.getSshPrivateKey().toAbsolutePath().toString());
            return commit;
        } else {
            LOG.warn("Can only sign commit using SSH key. Skipping sign commit when using GH_TOKEN");
            return commit;
        }
    }

    /**
     * Get the current user
     *
     * @return The current user
     */
    public GHUser getCurrentUser() {
        if (!isConnected()) {
            LOG.debug("Not able to get current user. GitHub client is not connected");
            return null;
        }
        try {
            // Get for token
            if (config.getGithubAppId() == null) {
                if (System.getenv("GITHUB_ACTIONS") == null) {
                    LOG.debug("Getting current user using token...");
                    return github.getMyself();
                }
                // Get the GitHub Actions user
                else {
                    LOG.debug("Getting current user using GitHub Actions...");
                    // Comply with https://api.github.com/users/github-actions%5Bbot%5D
                    return new GHUser() {
                        @Override
                        public String getLogin() {
                            return "github-actions[bot]";
                        }

                        @Override
                        public String getType() throws IOException {
                            return "Bot";
                        }

                        @Override
                        public String getEmail() {
                            return "41898282+github-actions[bot]@users.noreply.github.com";
                        }
                    };
                }
            }
            // Get for app
            else {
                LOG.debug("Getting current user using GitHub App...");
                LOG.debug("GitHub App name: {}", app.getName());
                return github.getUser("%s[bot]".formatted(app.getName()));
            }
        } catch (IOException e) {
            throw new ModernizerException("Failed to get current user", e);
        }
    }

    /**
     * Get the primary email of the user
     *
     * @param user The user to get the primary email for
     * @return The primary email
     */
    public String getPrimaryEmail(GHUser user) {
        try {
            // User
            if (user instanceof GHMyself myself && myself.getType().equalsIgnoreCase("user")) {
                return "%s@users.noreply.github.com".formatted(user.getLogin());
            }
            // Bot
            else if (app != null && user.getType().equalsIgnoreCase("bot")) {
                return "%s+%s@users.noreply.github.com".formatted(user.getId(), user.getLogin());
            }
            // GitHub action
            else if (System.getenv("GITHUB_ACTIONS") != null) {
                return user.getEmail();
            }
            throw new ModernizerException("Unknown user type %s".formatted(user.getType()));
        } catch (IOException e) {
            throw new ModernizerException("Failed to get primary email", e);
        }
    }

    /**
     * Push the changes to the forked repository
     *
     * @param plugin The plugin
     * @param repoType The repo type to push changes for
     */
    public void pushChanges(Plugin plugin, RepoType repoType) {
        if (config.isDryRun()) {
            LOG.info("Skipping push changes for {} {} in dry-run mode", repoType.getType(), plugin);
            return;
        }
        if (config.isFetchMetadataOnly()) {
            LOG.info("Skipping push changes for {} {} in fetch-metadata-only mode", repoType.getType(), plugin);
            return;
        }
        if (!repoType.hasCommits(plugin)) {
            LOG.info("No commits to push for {} {}", repoType.getType(), plugin.getName());
            return;
        }
        if (plugin.isArchived(this)) {
            LOG.info("Plugin {} is archived. Not pushing changes for {}", plugin, repoType.getType());
            return;
        }
        Path localRepository = repoType.getLocalRepository(plugin);
        try (Git git = Git.open(localRepository.toFile())) {
            String branchName = repoType.getBranchName(plugin, config.getRecipe());
            List<PushResult> results = StreamSupport.stream(
                            git.push()
                                    .setForce(true)
                                    .setRemote("origin")
                                    .setCredentialsProvider(getCredentialProvider())
                                    .setRefSpecs(new RefSpec(branchName + ":" + branchName))
                                    .call()
                                    .spliterator(),
                            false)
                    .toList();
            results.forEach(result -> {
                LOG.debug("Push result: {}", result.getMessages());
                if (result.getMessages().contains("error")) {
                    plugin.addError("Unexpected push error: %s".formatted(result.getMessages()));
                    plugin.raiseLastError();
                }
            });

            repoType.withoutCommits(plugin);
            repoType.withChangesPushed(plugin);

            LOG.info("Pushed changes to forked repository for {} {}", repoType.getType(), plugin.getName());
        } catch (IOException | GitAPIException e) {
            plugin.addError("Failed to push" + repoType.getType() + "changes", e);
            plugin.raiseLastError();
        }
    }

    /**
     * Open or update a pull request for the plugin and current recipe or its metadata
     *
     * @param plugin The plugin
     * @param repoType The repo type to open a pull request for
     */
    public void openPullRequest(Plugin plugin, RepoType repoType) {

        // Ensure to refresh client to target installation
        refreshToken(config.getGithubAppTargetInstallationId());

        // Renders parts and log then even if dry-run
        String prTitle = repoType.getPrTitle(plugin, config.getRecipe());
        String prBody = repoType.getPrBody(plugin, config.getRecipe());
        LOG.debug("Pull request title: {}", prTitle);
        LOG.debug("Pull request body: {}", prBody);
        LOG.debug("Draft mode: {}", config.isDraft());

        if (config.isDryRun()) {
            LOG.info("Skipping pull request changes for {} {} in dry-run mode", repoType.getType(), plugin);
            return;
        }
        if (config.isFetchMetadataOnly()) {
            LOG.info("Skipping pull request for {} {} in fetch-metadata-only mode", repoType.getType(), plugin);
            return;
        }
        if (!repoType.hasChangesPushed(plugin)) {
            LOG.info("No changes pushed to open pull request for {} {}", repoType.getType(), plugin.getName());
            return;
        }
        if (plugin.isArchived(this)) {
            LOG.info("Plugin {} is archived. Not opening pull request for {}", plugin, repoType.getType());
            return;
        }

        // Check if existing PR exists
        GHRepository repository = repoType.getRemoteRepository(plugin, this);
        String branchName = repoType.getBranchName(plugin, config.getRecipe());
        String head = getGithubOwner() + ":" + branchName;
        String base = repository.getDefaultBranch();

        Optional<GHPullRequest> existingPR = findExistingPullRequest(repository, head, base);

        if (existingPR.isPresent()) {
            switch (config.getDuplicatePrStrategy()) {
                case SKIP:
                    LOG.info(
                            "Duplicate PR detected: {}. Skipping creation.",
                            existingPR.get().getHtmlUrl());
                    return;
                case REPLACE:
                    LOG.info(
                            "Duplicate PR detected: {}. Closing and creating new.",
                            existingPR.get().getHtmlUrl());
                    try {
                        existingPR.get().close();
                    } catch (IOException e) {
                        LOG.warn(
                                "Failed to close existing PR: {}",
                                existingPR.get().getHtmlUrl(),
                                e);
                    }
                    break;
                case IGNORE:
                    LOG.info(
                            "Duplicate PR detected: {}. Creating new one as per IGNORE strategy.",
                            existingPR.get().getHtmlUrl());
                    break;
            }
        }

        try {
            GHPullRequest pr = repository.createPullRequest(prTitle, head, base, prBody, true, config.isDraft());
            LOG.info("Pull request created: {}", pr.getHtmlUrl());
            repoType.withPullRequest(plugin);
            if (repoType == RepoType.PLUGIN) {
                plugin.setPullRequestUrl(pr.getHtmlUrl().toString());
                deleteLegacyPrs(plugin);
                try {
                    String[] tags = plugin.getTags().stream()
                            .filter(ALLOWED_TAGS::contains)
                            .sorted()
                            .toArray(String[]::new);
                    if (tags.length > 0) {
                        pr.addLabels(tags);
                    }
                } catch (Exception e) {
                    LOG.debug("Failed to add labels to pull request: {}. Probably missing permission.", e.getMessage());
                } finally {
                    plugin.withoutTags();
                }
            }
        } catch (IOException e) {
            plugin.addError("Failed to create pull request for" + " " + repoType.getType(), e);
            plugin.raiseLastError();
        }
    }

    /**
     * Get the current credentials provider
     *
     * @return The credentials provider
     */
    private CredentialsProvider getCredentialProvider() {
        return sshKeyAuth
                ? new SshCredentialsProvider()
                : new UsernamePasswordCredentialsProvider(Settings.GITHUB_TOKEN, "");
    }

    /**
     * Return if the given repository has any pull request originating from it
     * Typically to avoid deleting fork with open pull requests
     *
     * @param plugin The plugin to check
     * @return True if the repository has any pull request
     */
    private boolean hasAnyPullRequestFrom(Plugin plugin) {
        if (config.isDryRun()) {
            LOG.info("Skipping check for pull requests in dry-run mode");
            return false;
        }
        GHRepository originalRepo = plugin.getRemoteRepository(this);
        GHRepository forkRepo = plugin.getRemoteForkRepository(this);

        try {
            boolean hasPullRequest = originalRepo.queryPullRequests().state(GHIssueState.OPEN).list().toList().stream()
                    .peek(pr -> LOG.debug("Checking pull request: {}", pr.getHtmlUrl()))
                    .anyMatch(pr -> pr.getHead().getRepository().getFullName().equals(forkRepo.getFullName()));
            if (hasPullRequest) {
                LOG.debug("Found open pull request from {} to {}", forkRepo.getFullName(), originalRepo.getFullName());
                return true;
            }
        } catch (IOException e) {
            plugin.addError("Failed to check for pull requests", e);
            return false;
        }
        LOG.debug(
                "No open pull requests found for plugin {} targeting {}", plugin.getName(), originalRepo.getFullName());
        return false;
    }

    /**
     * Find existing pull request
     *
     * @param repo The repository
     * @param head The head branch
     * @param base The base branch
     * @return The pull request if it exists
     */
    private Optional<GHPullRequest> findExistingPullRequest(GHRepository repo, String head, String base) {
        try {
            return repo.queryPullRequests().state(GHIssueState.OPEN).head(head).base(base).list().toList().stream()
                    .findFirst();
        } catch (IOException e) {
            LOG.warn("Failed to find existing pull request", e);
            return Optional.empty();
        }
    }

    /**
     * Delete legacy PR open from the plugin-modernizer-tool branch
     * @param plugin The plugin to check
     */
    private void deleteLegacyPrs(Plugin plugin) {
        GHRepository repository = plugin.getRemoteRepository(this);
        try {
            List<GHPullRequest> pullRequests = repository
                    .queryPullRequests()
                    .state(GHIssueState.OPEN)
                    .list()
                    .toList();
            pullRequests.stream()
                    .filter(pr -> pr.getHead().getRef().equals("plugin-modernizer-tool"))
                    .forEach(pr -> {
                        try {
                            pr.close();
                            LOG.info("Deleted legacy pull request: {}", pr.getHtmlUrl());
                        } catch (IOException e) {
                            LOG.debug("Failed to delete legacy pull request");
                        }
                    });
        } catch (IOException e) {
            LOG.warn("Failed to check if legacy pull request exists", e);
        }
    }

    /**
     * Get the diff statistics after modernization
     * @param plugin The plugin after modernization
     * @param dryRun The state of the cli tool
     * @return DiffStats (no. of additions, deletions and changed files)
     */
    public DiffStats getDiffStats(Plugin plugin, boolean dryRun) {
        Path gitDirPath = Settings.DEFAULT_CACHE_PATH
                .resolve(plugin.getName())
                .resolve("sources")
                .resolve(".git")
                .normalize();
        File gitDir = gitDirPath.toFile();

        try (Repository repository = new FileRepositoryBuilder()
                        .setGitDir(gitDir)
                        .readEnvironment()
                        .findGitDir()
                        .build();
                Git git = new Git(repository)) {

            ObjectReader reader = repository.newObjectReader();
            DiffFormatter formatter = new DiffFormatter(new ByteArrayOutputStream());
            formatter.setRepository(repository);
            formatter.setDiffComparator(RawTextComparator.DEFAULT);
            formatter.setDetectRenames(true);

            int additions = 0;
            int deletions = 0;
            int changedFiles = 0;
            if (dryRun) {
                // UNSTAGED: Working Directory vs Index
                DirCacheIterator indexTree = new DirCacheIterator(repository.readDirCache());
                FileTreeIterator workingTree = new FileTreeIterator(repository);

                List<DiffEntry> unstagedDiffs = git.diff()
                        .setOldTree(indexTree)
                        .setNewTree(workingTree)
                        .setShowNameAndStatusOnly(false)
                        .call();

                for (DiffEntry diff : unstagedDiffs) {
                    try {
                        EditList edits = formatter.toFileHeader(diff).toEditList();
                        for (Edit edit : edits) {
                            additions += edit.getEndB() - edit.getBeginB();
                            deletions += edit.getEndA() - edit.getBeginA();
                        }
                        changedFiles++;
                    } catch (MissingObjectException e) {
                        LOG.warn("Skipping diff for {}: {}", diff.getNewPath(), e.getMessage());
                    }
                }
                return new DiffStats(additions, deletions, changedFiles);
            }
            // COMMITTED: HEAD vs default branch or previous commit
            ObjectId head = repository.resolve("HEAD");
            String defaultBranchName = plugin.getRemoteRepository(this).getDefaultBranch();
            ObjectId defaultBranch = repository.resolve("refs/heads/" + defaultBranchName);

            if (defaultBranch == null) {
                throw new IOException("Could not resolve default branch.");
            }

            CanonicalTreeParser oldTree = new CanonicalTreeParser();
            CanonicalTreeParser newTree = new CanonicalTreeParser();
            oldTree.reset(reader, new RevWalk(repository).parseTree(defaultBranch));
            newTree.reset(reader, new RevWalk(repository).parseTree(head));

            List<DiffEntry> committedDiffs = git.diff()
                    .setOldTree(oldTree)
                    .setNewTree(newTree)
                    .setShowNameAndStatusOnly(false)
                    .call();

            for (DiffEntry diff : committedDiffs) {
                EditList edits = formatter.toFileHeader(diff).toEditList();
                for (Edit edit : edits) {
                    additions += edit.getEndB() - edit.getBeginB();
                    deletions += edit.getEndA() - edit.getBeginA();
                }
                changedFiles++;
            }
            reader.close();
            return new DiffStats(additions, deletions, changedFiles);

        } catch (IOException | GitAPIException e) {
            plugin.addError("Failed to get diff stats", e);
            plugin.raiseLastError();
        }
        return null;
    }

    /**
     * Determine the GitHub owner from config or using current token
     *
     * @return The GitHub owner
     */
    public String getGithubOwner() {
        return config.getGithubOwner() != null
                ? config.getGithubOwner()
                : getCurrentUser().getLogin();
    }

    /**
     * Return if SSH auth is used
     *
     * @return True if SSH key is used
     */
    public boolean isSshKeyAuth() {
        return sshKeyAuth;
    }

    /**
     * Ensure the forked reository correspond of the origin parent repository
     *
     * @param originalRepo The original repository
     * @param fork         The forked repository
     * @throws IOException If the check failed
     */
    private void checkSameParentRepository(Plugin plugin, GHRepository originalRepo, GHRepository fork)
            throws IOException {
        if (!fork.getParent().equals(originalRepo)) {
            LOG.warn(
                    "Forked repository {} is not forked from the original repository {}. Please remove forks if changing the source repo",
                    fork.getFullName(),
                    originalRepo.getFullName());
            throw new PluginProcessingException(
                    "Forked repository %s is not forked from the original repository %s but %s. Please remove forks if changing the source repo"
                            .formatted(
                                    fork.getFullName(),
                                    originalRepo.getFullName(),
                                    fork.getParent().getFullName()),
                    plugin);
        }
    }

    /**
     * Set the SSH key authentication if needed
     */
    private void setSshKeyAuth() {
        Path privateKey = config.getSshPrivateKey();
        if (Files.isRegularFile(privateKey)) {
            sshKeyAuth = true;
            LOG.debug("Using SSH private key for git operation: {}", privateKey);
        } else {
            sshKeyAuth = false;
            LOG.debug("SSH private key file {} does not exist. Will use GH_TOKEN for git operation", privateKey);
        }
    }

    /**
     * JGit expect a credential provider even if transport and authentication is none at transport level with
     * Apache Mina SSHD. This is therefor a dummy provider
     */
    private static class SshCredentialsProvider extends CredentialsProvider {
        @Override
        public boolean isInteractive() {
            return false;
        }

        @Override
        public boolean supports(CredentialItem... credentialItems) {
            return false;
        }

        @Override
        public boolean get(URIish uri, CredentialItem... credentialItems) throws UnsupportedCredentialItem {
            return false;
        }
    }
}
