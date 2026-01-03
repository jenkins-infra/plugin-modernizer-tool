package io.jenkins.tools.pluginmodernizer.core.config;

import io.jenkins.tools.pluginmodernizer.core.model.Plugin;
import io.jenkins.tools.pluginmodernizer.core.model.Recipe;
import java.net.URL;
import java.nio.file.Path;
import java.util.List;

public class Config {

    private static boolean DEBUG = false;
    private final boolean allowDeprecatedPlugins;

    public static void setDebug(boolean debug) {
        DEBUG = debug;
    }

    private final String version;
    private final List<Plugin> plugins;
    private final Recipe recipe;
    private final URL jenkinsUpdateCenter;
    private final URL jenkinsPluginVersions;
    private final URL pluginHealthScore;
    private final URL pluginStatsInstallations;
    private final URL optOutPlugins;
    private final URL githubApiUrl;
    private final Path cachePath;
    private final Path mavenHome;
    private final Path mavenLocalRepo;
    private final boolean skipMetadata;
    private final boolean overrideOptOutPlugins;
    private final boolean dryRun;
    private final boolean draft;
    private final boolean removeForks;
    private final String githubOwner;
    private final Long githubAppId;
    private final Long githubAppSourceInstallationId;
    private final Long githubAppTargetInstallationId;
    private final Path sshPrivateKey;
    private final DuplicatePrStrategy duplicatePrStrategy;

    private Config(
            String version,
            String githubOwner,
            Long githubAppId,
            Long githubAppSourceInstallationId,
            Long githubAppTargetInstallationId,
            Path sshPrivateKey,
            List<Plugin> plugins,
            Recipe recipe,
            URL jenkinsUpdateCenter,
            URL jenkinsPluginVersions,
            URL pluginHealthScore,
            URL pluginStatsInstallations,
            URL optOutPlugins,
            URL githubApiUrl,
            Path cachePath,
            Path mavenHome,
            Path mavenLocalRepo,
            boolean skipMetadata,
            boolean overrideOptOutPlugins,
            boolean dryRun,
            boolean draft,
            boolean removeForks,
            boolean allowDeprecatedPlugins,
            DuplicatePrStrategy duplicatePrStrategy) {
        this.version = version;
        this.githubOwner = githubOwner;
        this.githubAppId = githubAppId;
        this.githubAppSourceInstallationId = githubAppSourceInstallationId;
        this.githubAppTargetInstallationId = githubAppTargetInstallationId;
        this.sshPrivateKey = sshPrivateKey;
        this.plugins = plugins;
        this.recipe = recipe;
        this.jenkinsUpdateCenter = jenkinsUpdateCenter;
        this.jenkinsPluginVersions = jenkinsPluginVersions;
        this.pluginHealthScore = pluginHealthScore;
        this.pluginStatsInstallations = pluginStatsInstallations;
        this.optOutPlugins = optOutPlugins;
        this.githubApiUrl = githubApiUrl;
        this.cachePath = cachePath;
        this.mavenHome = mavenHome;
        this.mavenLocalRepo = mavenLocalRepo;
        this.skipMetadata = skipMetadata;
        this.overrideOptOutPlugins = overrideOptOutPlugins;
        this.dryRun = dryRun;
        this.draft = draft;
        this.removeForks = removeForks;
        this.allowDeprecatedPlugins = allowDeprecatedPlugins;
        this.duplicatePrStrategy = duplicatePrStrategy;
    }

    public String getVersion() {
        return version;
    }

    public String getGithubOwner() {
        return githubOwner;
    }

    public Long getGithubAppId() {
        return githubAppId;
    }

    public Long getGithubAppSourceInstallationId() {
        return githubAppSourceInstallationId;
    }

    public Long getGithubAppTargetInstallationId() {
        return githubAppTargetInstallationId;
    }

    public Path getSshPrivateKey() {
        return sshPrivateKey;
    }

    public List<Plugin> getPlugins() {
        return plugins;
    }

    public Recipe getRecipe() {
        return recipe;
    }

    /**
     * Return if only fetching metadata (skips compile/verify).
     *
     * @return True if only fetching metadata
     */
    public boolean isFetchMetadataOnly() {
        return recipe != null && recipe.getName().equals(Settings.FETCH_METADATA_RECIPE.getName());
    }

    /**
     * Return if recipe execution will be skipped.
     *
     * @return True if the recipe will skip verification
     */
    public boolean isSkipVerification() {
        return recipe.isSkipVerification();
    }

    public URL getJenkinsUpdateCenter() {
        return jenkinsUpdateCenter;
    }

    public URL getJenkinsPluginVersions() {
        return jenkinsPluginVersions;
    }

    public URL getPluginHealthScore() {
        return pluginHealthScore;
    }

    public URL getPluginStatsInstallations() {
        return pluginStatsInstallations;
    }

    public URL getOptOutPlugins() {
        return optOutPlugins;
    }

    public URL getGithubApiUrl() {
        return githubApiUrl;
    }

    public Path getCachePath() {
        return cachePath.toAbsolutePath();
    }

    public Path getMavenHome() {
        if (mavenHome == null) {
            return null;
        }
        return mavenHome.toAbsolutePath();
    }

    public Path getMavenLocalRepo() {
        if (mavenLocalRepo == null) {
            return Settings.DEFAULT_MAVEN_LOCAL_REPO;
        }
        return mavenLocalRepo.toAbsolutePath();
    }

    public boolean isSkipMetadata() {
        return skipMetadata;
    }

    public boolean isOverrideOptOutPlugins() {
        return overrideOptOutPlugins;
    }

    public boolean isDryRun() {
        return dryRun;
    }

    public static boolean isDebug() {
        return DEBUG;
    }

    public boolean isDraft() {
        return draft;
    }

    public boolean isRemoveForks() {
        return removeForks;
    }

    public boolean isAllowDeprecatedPlugins() {
        return allowDeprecatedPlugins;
    }

    public DuplicatePrStrategy getDuplicatePrStrategy() {
        return duplicatePrStrategy;
    }

    public enum DuplicatePrStrategy {
        SKIP,
        REPLACE,
        IGNORE
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String version;
        private String githubOwner = Settings.GITHUB_OWNER;
        private Long githubAppId;
        private Long githubAppSourceInstallationId;
        private Long githubAppTargetInstallationId;
        private Path sshPrivateKey = Settings.SSH_PRIVATE_KEY;
        private List<Plugin> plugins;
        private Recipe recipe;
        private URL jenkinsUpdateCenter = Settings.DEFAULT_UPDATE_CENTER_URL;
        private URL jenkinsPluginVersions = Settings.DEFAULT_PLUGIN_VERSIONS;
        private URL pluginStatsInstallations = Settings.DEFAULT_PLUGINS_STATS_INSTALLATIONS_URL;
        private URL pluginHealthScore = Settings.DEFAULT_HEALTH_SCORE_URL;
        private URL optOutPlugins = Settings.OPT_OUT_PLUGINS_URL;
        private URL githubApiUrl = Settings.GITHUB_API_URL;
        private Path cachePath = Settings.DEFAULT_CACHE_PATH;
        private Path mavenHome = Settings.DEFAULT_MAVEN_HOME;
        private Path mavenLocalRepo = Settings.DEFAULT_MAVEN_LOCAL_REPO;
        private boolean skipMetadata = false;
        private boolean overrideOptOutPlugins = false;
        private boolean dryRun = false;
        private boolean draft = false;
        public boolean removeForks = false;
        private boolean allowDeprecatedPlugins = false;
        private DuplicatePrStrategy duplicatePrStrategy = DuplicatePrStrategy.SKIP;

        public Builder withVersion(String version) {
            this.version = version;
            return this;
        }

        public Builder withGitHubOwner(String githubOwner) {
            this.githubOwner = githubOwner;
            return this;
        }

        public Builder withGitHubAppId(Long githubAppId) {
            this.githubAppId = githubAppId;
            return this;
        }

        public Builder withGitHubAppSourceInstallationId(Long githubAppInstallationId) {
            this.githubAppSourceInstallationId = githubAppInstallationId;
            return this;
        }

        public Builder withGitHubAppTargetInstallationId(Long githubAppInstallationId) {
            this.githubAppTargetInstallationId = githubAppInstallationId;
            return this;
        }

        public Builder withSshPrivateKey(Path sshPrivateKey) {
            this.sshPrivateKey = sshPrivateKey;
            return this;
        }

        public Builder withPlugins(List<Plugin> plugins) {
            this.plugins = plugins;
            return this;
        }

        public Builder withRecipe(Recipe recipe) {
            this.recipe = recipe;
            return this;
        }

        public Builder withJenkinsUpdateCenter(URL jenkinsUpdateCenter) {
            if (jenkinsUpdateCenter != null) {
                this.jenkinsUpdateCenter = jenkinsUpdateCenter;
            }
            return this;
        }

        public Builder withJenkinsPluginVersions(URL jenkinsPluginVersions) {
            if (jenkinsPluginVersions != null) {
                this.jenkinsPluginVersions = jenkinsPluginVersions;
            }
            return this;
        }

        public Builder withPluginHealthScore(URL pluginHealthScore) {
            if (pluginHealthScore != null) {
                this.pluginHealthScore = pluginHealthScore;
            }
            return this;
        }

        public Builder withPluginStatsInstallations(URL pluginStatsInstallations) {
            if (pluginStatsInstallations != null) {
                this.pluginStatsInstallations = pluginStatsInstallations;
            }
            return this;
        }

        public Builder withOptOutPlugins(URL optOutPlugins) {
            if (optOutPlugins != null) {
                this.optOutPlugins = optOutPlugins;
            }
            return this;
        }

        public Builder withGithubApiUrl(URL githubApiUrl) {
            if (githubApiUrl != null) {
                this.githubApiUrl = githubApiUrl;
            }
            return this;
        }

        public Builder withCachePath(Path cachePath) {
            if (cachePath != null) {
                this.cachePath = cachePath;
            }
            return this;
        }

        public Builder withMavenHome(Path mavenHome) {
            if (mavenHome != null) {
                this.mavenHome = mavenHome;
            }
            return this;
        }

        public Builder withMavenLocalRepo(Path mavenLocalRepo) {
            if (mavenLocalRepo != null) {
                this.mavenLocalRepo = mavenLocalRepo;
            }
            return this;
        }

        public Builder withSkipMetadata(boolean skipMetadata) {
            this.skipMetadata = skipMetadata;
            return this;
        }

        public Builder withOverrideOptOutPlugins(boolean overrideOptOutPlugins) {
            this.overrideOptOutPlugins = overrideOptOutPlugins;
            return this;
        }

        public Builder withDryRun(boolean dryRun) {
            this.dryRun = dryRun;
            return this;
        }

        public Builder withDraft(boolean draft) {
            this.draft = draft;
            return this;
        }

        public Builder withRemoveForks(boolean removeForks) {
            this.removeForks = removeForks;
            return this;
        }

        public Builder withAllowDeprecatedPlugins(boolean allowDeprecatedPlugins) {
            this.allowDeprecatedPlugins = allowDeprecatedPlugins;
            return this;
        }

        public Builder withDuplicatePrStrategy(DuplicatePrStrategy duplicatePrStrategy) {
            this.duplicatePrStrategy = duplicatePrStrategy;
            return this;
        }

        public Config build() {
            return new Config(
                    version,
                    githubOwner,
                    githubAppId,
                    githubAppSourceInstallationId,
                    githubAppTargetInstallationId,
                    sshPrivateKey,
                    plugins,
                    recipe,
                    jenkinsUpdateCenter,
                    jenkinsPluginVersions,
                    pluginHealthScore,
                    pluginStatsInstallations,
                    optOutPlugins,
                    githubApiUrl,
                    cachePath,
                    mavenHome,
                    mavenLocalRepo,
                    skipMetadata,
                    overrideOptOutPlugins,
                    dryRun,
                    draft,
                    removeForks,
                    allowDeprecatedPlugins,
                    duplicatePrStrategy);
        }
    }
}
