package io.jenkins.tools.pluginmodernizer.core.impl;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.jenkins.tools.pluginmodernizer.core.config.Config;
import io.jenkins.tools.pluginmodernizer.core.config.Settings;
import io.jenkins.tools.pluginmodernizer.core.extractor.PluginMetadata;
import io.jenkins.tools.pluginmodernizer.core.github.GHService;
import io.jenkins.tools.pluginmodernizer.core.model.JDK;
import io.jenkins.tools.pluginmodernizer.core.model.ModernizerException;
import io.jenkins.tools.pluginmodernizer.core.model.Plugin;
import io.jenkins.tools.pluginmodernizer.core.model.PluginProcessingException;
import io.jenkins.tools.pluginmodernizer.core.utils.PluginService;
import io.jenkins.tools.pluginmodernizer.core.utils.StaticPomParser;
import jakarta.inject.Inject;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressFBWarnings(value = "CRLF_INJECTION_LOGS", justification = "safe because versions from pom.xml")
public class PluginModernizer {

    private static final Logger LOG = LoggerFactory.getLogger(PluginModernizer.class);

    @Inject
    private Config config;

    @Inject
    private MavenInvoker mavenInvoker;

    @Inject
    private GHService ghService;

    @Inject
    private PluginService pluginService;

    @Inject
    private CacheManager cacheManager;

    /**
     * Validate the configuration
     */
    public void validate() {
        mavenInvoker.validateMaven();
        mavenInvoker.validateMavenVersion();
        if (!ghService.isConnected()) {
            ghService.connect();
            ghService.validate();
        }
    }

    /**
     * List available recipes
     */
    public void listRecipes() {
        Settings.AVAILABLE_RECIPES.stream()
                .sorted()
                .forEach(recipe -> LOG.info(
                        "{} - {}",
                        recipe.getName().replaceAll(Settings.RECIPE_FQDN_PREFIX + ".", ""),
                        recipe.getDescription()));
    }

    /**
     * Expoose the dry run option
     * @return If the tool is running in dry run mode
     */
    public Boolean isDryRun() {
        return config.isDryRun();
    }

    /**
     * Expose the effective GitHub owner from either config or current owner of token
     * @return The GitHub owner
     */
    public String getGithubOwner() {
        return ghService.getGithubOwner();
    }

    /**
     * Expose the effective SSH private key path
     * @return The SSH private key path
     */
    public String getSshPrivateKeyPath() {
        return config.getSshPrivateKey().toString();
    }

    /**
     * Expose the effective Maven version
     * @return The Maven version
     */
    public String getMavenVersion() {
        return mavenInvoker.getMavenVersion() != null
                ? mavenInvoker.getMavenVersion().toString()
                : "unknown";
    }

    /**
     * Expose the effective Maven home
     * @return The Maven home
     */
    public String getMavenHome() {
        return config.getMavenHome().toString();
    }

    /**
     * Expose the effective Maven local repository
     * @return The Maven local repository
     */
    public String getMavenLocalRepo() {
        return config.getMavenLocalRepo().toString();
    }

    /**
     * Expose the effective cache path
     * @return The cache path
     */
    public String getCachePath() {
        return config.getCachePath().toString();
    }

    /**
     * Expose the effective Java version
     * @return The Java version
     */
    public String getJavaVersion() {
        return System.getProperty("java.version");
    }

    /**
     * Clean the cache
     */
    public void cleanCache() {
        cacheManager.wipe();
    }

    /**
     * Entry point to start the plugin modernization process
     */
    public void start() {

        validate();
        cacheManager.init();

        // Debug config
        LOG.debug("Plugins: {}", config.getPlugins());
        LOG.debug("Recipe: {}", config.getRecipe().getName());
        LOG.debug("GitHub owner: {}", getGithubOwner());
        if (ghService.isSshKeyAuth()) {
            LOG.debug("SSH private key: {}", getSshPrivateKeyPath());
        } else {
            LOG.debug("Using GitHub token for git authentication");
        }
        LOG.debug("Update Center Url: {}", config.getJenkinsUpdateCenter());
        LOG.debug("Plugin versions Url: {}", config.getJenkinsPluginVersions());
        LOG.debug("Plugin Health Score Url: {}", config.getPluginHealthScore());
        LOG.debug("Installation Stats Url: {}", config.getPluginStatsInstallations());
        LOG.debug("Cache Path: {}", config.getCachePath());
        LOG.debug("Maven Home: {}", config.getMavenHome());
        LOG.debug("Maven Local Repository: {}", config.getMavenLocalRepo());
        LOG.debug("Dry Run: {}", config.isDryRun());
        LOG.debug("Maven rewrite plugin version: {}", Settings.MAVEN_REWRITE_PLUGIN_VERSION);

        // Fetch plugin versions
        pluginService.getPluginVersionData();

        List<Plugin> plugins = config.getPlugins();
        plugins.forEach(this::process);
        printResults(plugins);
    }

    /**
     * Process a plugin
     * @param plugin The plugin to process
     */
    private void process(Plugin plugin) {
        try {

            // Set config
            plugin.withConfig(config);

            // Determine repo name
            plugin.withRepositoryName(pluginService.extractRepoName(plugin));

            LOG.debug("Repository name: {}", plugin.getRepositoryName());
            LOG.debug("Plugin {} latest version: {}", plugin.getName(), pluginService.extractVersion(plugin));
            LOG.debug("Plugin {} health score: {}", plugin.getName(), pluginService.extractScore(plugin));
            LOG.debug("Plugin {} installations: {}", plugin.getName(), pluginService.extractInstallationStats(plugin));
            LOG.debug("Is API plugin {} : {}", plugin.getName(), plugin.isApiPlugin(pluginService));
            if (plugin.isDeprecated(pluginService)) {
                LOG.info("Plugin {} is deprecated. Skipping.", plugin.getName());
                plugin.addError("Plugin is deprecated");
                return;
            }
            if (plugin.isArchived(ghService)) {
                LOG.info("Plugin {} is archived. Skipping.", plugin.getName());
                plugin.addError("Plugin is archived");
                return;
            }
            if (config.isSkipVerification()) {
                LOG.info("Skipping verification for plugin {}", plugin.getName());
            }

            if (config.isRemoveForks()) {
                plugin.deleteFork(ghService);
            }

            plugin.fetch(ghService);
            if (plugin.hasErrors()) {
                LOG.info("Plugin {} has errors. Will not process this plugin.", plugin.getName());
            }

            // Set the metadata from cache if available
            plugin.loadMetadata(cacheManager);

            // Compile only if we are able to find metadata
            // For the moment it's local cache only but later will fetch on remote storage
            if (!config.isFetchMetadataOnly() && !config.isSkipVerification()) {
                if (plugin.getMetadata() != null && !plugin.hasPreconditionErrors()) {
                    JDK jdk = compilePlugin(plugin);
                    LOG.debug("Plugin {} compiled successfully with JDK {}", plugin.getName(), jdk.getMajor());
                } else {
                    LOG.debug(
                            "No metadata or precondition errors found for plugin {}. Skipping initial compilation.",
                            plugin.getName());
                }
            }

            plugin.checkoutBranch(ghService);

            // Minimum JDK to run openrewrite
            plugin.withJDK(JDK.JAVA_17);

            // Collect metadata and move metadata from the target directory of the plugin to the common cache
            if (!plugin.hasMetadata() || config.isFetchMetadataOnly()) {
                collectMetadata(plugin, true);

            } else {
                LOG.debug("Metadata already computed for plugin {}. Using cached metadata.", plugin.getName());
            }

            // Try to remediate precondition errors
            if (plugin.hasPreconditionErrors()) {
                plugin.getPreconditionErrors().forEach(preconditionError -> {
                    if (preconditionError.remediate(plugin)) {
                        plugin.removePreconditionError(preconditionError);
                        LOG.info(
                                "Precondition error {} was remediated for plugin {}",
                                preconditionError,
                                plugin.getName());
                    } else {
                        LOG.info(
                                "Precondition error {} was not remediated for plugin {}",
                                preconditionError,
                                plugin.getName());
                    }
                });

                // Retry to collect metadata after remediation to get up-to-date results
                if (!config.isFetchMetadataOnly()) {
                    collectMetadata(plugin, true);
                }
            }

            // Check if we still have errors and abort if not remediation is possible
            if (plugin.hasErrors() || plugin.hasPreconditionErrors()) {
                plugin.addPreconditionErrors(plugin.getMetadata());
                LOG.info(
                        "Skipping plugin {} due to metadata/precondition errors. Check logs for more details.",
                        plugin.getName());
                return;
            }

            // Handle outdated plugin or unparsable Jenkinsfile
            if (plugin.getMetadata().getJdks().stream().allMatch(jdk -> jdk.equals(JDK.getImplicit()))) {
                LOG.info(
                        "Plugin look outdated or without Jenkinsfile. Or fail it's parsing, falling back to jenkins.version");
                StaticPomParser parser = new StaticPomParser(
                        plugin.getLocalRepository().resolve("pom.xml").toString());
                String jenkinsVersion = parser.getJenkinsVersion();
                String baseline = parser.getBaseline();
                if (baseline != null && jenkinsVersion != null && jenkinsVersion.contains("${jenkins.baseline}")) {
                    jenkinsVersion = jenkinsVersion.replace("${jenkins.baseline}", baseline);
                }
                JDK jdk = JDK.get(jenkinsVersion).stream().findFirst().orElse(JDK.min());
                LOG.info("Found jenkins version {} from pom which support Java {}", jenkinsVersion, jdk.getMajor());
                plugin.getMetadata().setJdks(Set.of(jdk));
                plugin.getMetadata().save();
                LOG.debug("Metadata after fallback: {}", plugin.getMetadata().toJson());
                if (jdk.getMajor() <= 8) {
                    LOG.info("Need a first compile to generate classes due to Java 8 and lower");
                    plugin.verifyQuickBuild(mavenInvoker, jdk);
                    if (plugin.hasErrors()) {
                        if (!config.isSkipVerification()) {
                            plugin.raiseLastError();
                        } else {
                            LOG.info(
                                    "Quick build failed for plugin {}. Skip verification is enabled, trying to run recipe any.",
                                    plugin.getName());
                            plugin.removeErrors();
                        }
                    }

                    // Ensure we recollect metadata
                    collectMetadata(plugin, false);
                }

                // Reset the repo to not keep changes for build-metadata
                // and try to set the right JDK and jenkins version
                if (config.isFetchMetadataOnly()) {
                    plugin.fetch(ghService);
                }
            }

            // Run OpenRewrite
            plugin.runOpenRewrite(mavenInvoker);
            if (plugin.hasErrors()) {
                LOG.warn(
                        "Skipping plugin {} due to openrewrite recipes errors. Check logs for more details.",
                        plugin.getName());
                return;
            }

            // Verify plugin
            if (!config.isFetchMetadataOnly() && !config.isSkipVerification()) {
                JDK jdk = verifyPlugin(plugin);
                LOG.info("Plugin {} verified successfully with JDK {}", plugin.getName(), jdk.getMajor());
            }

            if (plugin.hasErrors()) {
                LOG.warn(
                        "Skipping plugin {} due to verification errors after modernization. Check logs for more details.",
                        plugin.getName());
                return;
            }

            // Recollect metadata after modernization
            if (!config.isFetchMetadataOnly()) {
                plugin.withJDK(JDK.JAVA_17);
                plugin.clean(mavenInvoker);
                collectMetadata(plugin, false);
                LOG.debug(
                        "Plugin {} metadata after modernization: {}",
                        plugin.getName(),
                        plugin.getMetadata().toJson());

                // Clean target folder before committing changes
                if (!config.isDryRun()) {
                    plugin.clean(mavenInvoker);
                }
                plugin.commit(ghService);

                // Only fork/push/PR if we have any changes
                if (!plugin.getModifiedFiles().isEmpty()) {
                    plugin.fork(ghService);
                    plugin.sync(ghService);
                    plugin.push(ghService);
                    plugin.openPullRequest(ghService);
                } else {
                    LOG.info("No changes were made for plugin {}", plugin.getName());
                }

                if (config.isRemoveForks()) {
                    plugin.deleteFork(ghService);
                }
            }

        }
        // Uncatched plugin processing errors
        catch (PluginProcessingException e) {
            if (!plugin.hasErrors()) {
                plugin.addError("Plugin processing error. Check the logs at " + plugin.getLogFile(), e);
            }
        }
        // Catch any unexpected exception here
        catch (Exception e) {
            if (!plugin.hasErrors()) {
                plugin.addError("Unexpected processing error. Check the logs at " + plugin.getLogFile(), e);
            }
        }
    }

    /**
     * Collect metadata for a plugin
     * @param plugin The plugin
     */
    private void collectMetadata(Plugin plugin, boolean retryAfterFirstCompile) {
        LOG.trace("Collecting metadata for plugin {}... Please be patient", plugin.getName());
        plugin.withJDK(JDK.JAVA_17);
        try {
            plugin.collectMetadata(mavenInvoker);
            if (plugin.hasErrors()) {
                plugin.raiseLastError();
            }
        } catch (ModernizerException e) {
            if (retryAfterFirstCompile) {
                plugin.removeErrors();
                LOG.warn(
                        "Failed to collect metadata for plugin {}. Will retry after a first compile using lowest JDK",
                        plugin.getName());
                plugin.verifyQuickBuild(mavenInvoker, JDK.JAVA_8);
                if (plugin.hasErrors()) {
                    LOG.debug(
                            "Plugin {} failed to compile with JDK 8. Skipping metadata collection after retry",
                            plugin.getName());
                    plugin.raiseLastError();
                }
                plugin.withJDK(JDK.JAVA_17);
                plugin.collectMetadata(mavenInvoker);
            } else {
                LOG.info("Failed to collect metadata for plugin {}. Not retrying.", plugin.getName());
                throw e;
            }
        }
        plugin.copyMetadata(cacheManager);
        plugin.loadMetadata(cacheManager);
        plugin.enrichMetadata(pluginService);
    }

    /**
     * Compile a plugin
     * @param plugin The plugin to compile
     */
    private JDK compilePlugin(Plugin plugin) {
        PluginMetadata metadata = plugin.getMetadata();
        JDK jdk = JDK.min(metadata.getJdks(), metadata.getJenkinsVersion());
        plugin.withJDK(jdk);
        plugin.clean(mavenInvoker);
        plugin.compile(mavenInvoker);
        return jdk;
    }

    /**
     * Verify a plugin and return the first JDK that successfully verifies it, starting from the target JDK and moving backward
     * @param plugin The plugin to verify
     * @return The JDK that verifies the plugin
     */
    private JDK verifyPlugin(Plugin plugin) {
        PluginMetadata metadata = plugin.getMetadata();

        // Determine the JDK
        JDK jdk;
        if (metadata.getJdks() == null || metadata.getJdks().isEmpty()) {
            jdk = JDK.JAVA_17;
            LOG.info(
                    "No JDKs found in metadata for plugin {}. Using same JDK as rewrite for verification",
                    plugin.getName());
        } else {
            jdk = JDK.min(metadata.getJdks(), metadata.getJenkinsVersion());
            LOG.info("Using minimum JDK {} from metadata for plugin {}", jdk.getMajor(), plugin.getName());
        }
        // If the plugin was modernized we should find next JDK compatible
        // For example a Java 8 plugin was modernized to Java 11
        while (JDK.hasNext(jdk) && !jdk.supported(metadata.getJenkinsVersion())) {
            jdk = jdk.next();
        }

        // Build it
        plugin.withJDK(jdk);
        plugin.clean(mavenInvoker);
        plugin.format(mavenInvoker);
        plugin.verify(mavenInvoker);
        if (plugin.hasErrors()) {
            LOG.info("Plugin {} failed to verify with JDK {}", plugin.getName(), jdk.getMajor());
            plugin.withoutErrors();
        }
        plugin.withoutErrors();

        return jdk;
    }

    /**
     * Collect results from the plugins and display a summary
     * @param plugins The plugins
     */
    private void printResults(List<Plugin> plugins) {
        for (Plugin plugin : plugins) {
            LOG.info("*************");
            LOG.info("Plugin: {}", plugin.getName());

            // Display error
            if (plugin.hasErrors()) {
                for (PluginProcessingException error : plugin.getErrors()) {
                    LOG.error("Error: {}", error.getMessage());
                    if (config.isDebug()) {
                        LOG.error("Stacktrace: ", error);
                        break;
                    }
                }

            }
            // Display what's done
            else {
                if (config.isFetchMetadataOnly()) {
                    LOG.info(
                            "Metadata was fetched for plugin {} and is available at {}",
                            plugin.getName(),
                            plugin.getMetadata().getLocation().toAbsolutePath());
                } else if (config.isDryRun()) {
                    LOG.info("Dry run mode. Changes were made on " + plugin.getLocalRepository() + " but not committed");
                    printModifiedFiles(plugin);
                } else if (plugin.isLocal()) {
                    LOG.info("Changes were made on " + plugin.getLocalRepository());
                    printModifiedFiles(plugin);
                } else if (!plugin.hasErrors()) {
                    //Changes were made
                    LOG.info("Pull request was open on "
                            + plugin.getRemoteRepository(this.ghService).getHtmlUrl());
                    printModifiedFiles(plugin);
                }
            }
            LOG.info("*************");
        }
    }

    private void printModifiedFiles(Plugin plugin) {
        if (plugin.getModifiedFiles().isEmpty()) {
            LOG.info("Recipe didn't make any change.");
            return;
        }
        for (String modification : plugin.getModifiedFiles()) {
            LOG.info("Modified file: {}", modification);
        }
    }
}
