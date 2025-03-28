package io.jenkins.tools.pluginmodernizer.core.model;

import io.jenkins.tools.pluginmodernizer.core.config.Config;
import io.jenkins.tools.pluginmodernizer.core.config.Settings;
import io.jenkins.tools.pluginmodernizer.core.extractor.MetadataFlag;
import io.jenkins.tools.pluginmodernizer.core.extractor.PluginMetadata;
import io.jenkins.tools.pluginmodernizer.core.github.GHService;
import io.jenkins.tools.pluginmodernizer.core.impl.CacheManager;
import io.jenkins.tools.pluginmodernizer.core.impl.MavenInvoker;
import io.jenkins.tools.pluginmodernizer.core.utils.PluginService;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;
import org.kohsuke.github.GHRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import org.w3c.dom.Document;

/**
 * Mutable class representing a Jenkins plugin to modernize and refactor
 */
public class Plugin {

    private static final Logger LOG = LoggerFactory.getLogger(Plugin.class);

    /**
     * The configuration to use
     */
    private Config config;

    /**
     * The plugin name
     */
    private String name;

    /**
     * Flag to indicate if the plugin is local
     */
    private boolean local;

    /**
     * Local repository path if local plugin
     */
    private Path localRepository;

    /**
     * Repository name under the jenkinsci organization
     */
    private String repositoryName;

    /**
     * The JDK to use
     */
    private JDK jdk;

    /**
     * The metadata of the plugin
     */
    private PluginMetadata metadata;

    /**
     * Flag to indicate if the plugin has any commits to be pushed
     */
    private boolean hasCommits;

    /**
     * Flag to indicate if the plugin has any changes pushed and ready to be merged
     */
    private boolean hasChangesPushed;

    /**
     * Flag to indicate if the plugin has any pull request open
     */
    private boolean hasPullRequest;

    /**
     * Return if the plugin has any error
     */
    private final List<PluginProcessingException> errors = new LinkedList<>();

    /**
     * List of modified files(added, modified, deleted) in the plugin
     */
    private final List<String> modifiedFiles = new LinkedList<>();

    /**
     * Tags to apply on pull request for the applied changes
     */
    private final Set<String> tags = new HashSet<>();

    /**
     * Prefix for the log file names that record modernization failures.
     */
    private static final String MODERNIZATION_FAILURES_LOG_PREFIX = "modernization-failures-";

    /**
     * Prefix for the log file names that record network-related failures.
     */
    private static final String NETWORK_FAILURES_LOG_PREFIX = "network-failures-";

    /**
     * Extension for the log files.
     */
    private static final String LOG_FILE_EXTENSION = ".log";

    private Plugin() {}

    /**
     * Build a minimal plugin object with name
     * @param name Name of the plugin
     * @return Plugin object
     */
    public static Plugin build(String name) {
        return new Plugin().withName(name);
    }

    /**
     * Build a local plugin object with name and location
     * @param name Name of the plugin
     * @param location Location of the plugin
     * @return Plugin object
     */
    public static Plugin build(String name, Path location) {
        return new Plugin().withName(name).withLocal(true).withLocalRepository(location);
    }

    /**
     * Set the config of the plugin
     * @param config The config
     * @return Plugin object
     */
    public Plugin withConfig(Config config) {
        this.config = config;
        return this;
    }

    /**
     * Set the name of the plugin
     * @param name Name of the plugin
     * @return Plugin object
     */
    public Plugin withName(String name) {
        this.name = name;
        return this;
    }

    /**
     * Set the local flag of the plugin
     * @param local Local flag
     * @return Plugin object
     */
    public Plugin withLocal(boolean local) {
        this.local = local;
        return this;
    }

    /**
     * Set the local repository path of the plugin
     * @param localRepository Local repository path
     * @return Plugin object
     */
    public Plugin withLocalRepository(Path localRepository) {
        this.localRepository = localRepository.toAbsolutePath();
        return this;
    }

    /***
     * Set the repository name of the plugin
     * @param repositoryName Repository name of the plugin
     * @return Plugin object
     */
    public Plugin withRepositoryName(String repositoryName) {
        this.repositoryName = repositoryName;
        return this;
    }

    /***
     * Set the current JDK
     * @param jdk The JDK
     * @return Plugin object
     */
    public Plugin withJDK(JDK jdk) {
        this.jdk = jdk;
        return this;
    }

    /**
     * Indicate that the plugin has commits to be pushed
     * @return Plugin object
     */
    public Plugin withCommits() {
        this.hasCommits = true;
        return this;
    }

    /**
     * Indicate that the plugin has no commits to be pushed
     * @return Plugin object
     */
    public Plugin withoutCommits() {
        this.hasCommits = false;
        return this;
    }

    /**
     * Indicate that the plugin has changes pushed and ready to be merged
     * @return Plugin object
     */
    public Plugin withChangesPushed() {
        this.hasChangesPushed = true;
        return this;
    }

    /**
     * Indicate that the plugin has no changes pushed and ready to be merged
     * @return Plugin object
     */
    public Plugin withoutChangesPushed() {
        this.hasChangesPushed = false;
        return this;
    }

    /**
     * Indicate that the plugin has a pull request open
     * @return Plugin object
     */
    public Plugin withPullRequest() {
        this.hasPullRequest = true;
        return this;
    }

    /**
     * Indicate that the plugin has no pull request open
     * @return Plugin object
     */
    public Plugin withoutPullRequest() {
        this.hasPullRequest = false;
        return this;
    }

    /**
     * Return if the plugin has any commits
     * @return True if the plugin has commits
     */
    public boolean hasCommits() {
        return hasCommits;
    }

    /**
     * Return if the plugin has any changes pushed and ready to be merged
     * @return True if the plugin has changes pushed
     */
    public boolean hasChangesPushed() {
        return hasChangesPushed;
    }

    /**
     * Return if the plugin has any changes pushed and ready to be merged
     * @return True if the plugin has changes pushed
     */
    public boolean hasPullRequest() {
        return hasPullRequest;
    }

    /**
     * Convenience method to check if the plugin is using Spotless
     * @return True if the plugin is using Spotless
     */
    public boolean isUsingSpotless() {
        return metadata != null
                && metadata.getProperties().get("spotless.check.skip") != null
                && metadata.getProperties().get("spotless.check.skip").equals("false");
    }

    /**
     * Return if the plugin has any errors
     * @return True if the plugin has errors
     */
    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    /**
     * Return if the plugin has any precondition errors
     * @return True if the plugin has precondition errors
     */
    public boolean hasPreconditionErrors() {
        return metadata != null
                && metadata.getErrors() != null
                && !metadata.getErrors().isEmpty();
    }

    /**
     * Get the precondition errors of the plugin
     * @return Set of precondition errors
     */
    public Set<PreconditionError> getPreconditionErrors() {
        return Collections.unmodifiableSet(metadata.getErrors());
    }

    /**
     * Add precondition errors to the plugin errors
     */
    public void addPreconditionErrors(PluginMetadata metadata) {
        if (metadata == null) {
            return;
        }
        metadata.getErrors().forEach(error -> addError(error.getError()));
    }

    /**
     * Remove a precondition error from the metadata errors
     * @param preconditionError Precondition error to remove
     */
    public void removePreconditionError(PreconditionError preconditionError) {
        if (metadata == null) {
            return;
        }
        metadata.setErrors(metadata.getErrors().stream()
                .filter(error -> !error.equals(preconditionError))
                .collect(Collectors.toSet()));
    }

    /**
     * Get the errors of the plugin
     * @return List of errors
     */
    public List<PluginProcessingException> getErrors() {
        return Collections.unmodifiableList(errors);
    }

    /**
     * Add an error to the plugin
     * @param message The message
     * @param e The exception
     */
    public void addError(String message, Exception e) {
        LOG.error(getMarker(), message, e);
        if (config.isDebug()) {
            LOG.error(message, e);
        } else {
            LOG.error(message);
        }
        errors.add(new PluginProcessingException(message, e, this));
        logFailure();
    }

    /**
     * Add an error to the plugin
     * @param message The message
     */
    public void addError(String message) {
        LOG.error(message);
        errors.add(new PluginProcessingException(message, this));
        logFailure();
    }

    /**
     * Create a log file path with the given prefix and current date
     * @param prefix The log file prefix
     * @return Path to the log file
     */
    private Path createLogFilePath(String prefix) {
        String timestamp =
                java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
        return Path.of(
                System.getProperty("user.home"),
                ".cache",
                "jenkins-plugin-modernizer-cli",
                prefix + "-" + timestamp + LOG_FILE_EXTENSION);
    }

    /**
     * Extracts the last part of the recipe name after the last dot.
     *
     * @param recipeName The full recipe name
     * @return The last part of the recipe name
     */
    private String getLastPartOfRecipeName(String recipeName) {
        int lastDotIndex = recipeName.lastIndexOf('.');
        return lastDotIndex != -1 ? recipeName.substring(lastDotIndex + 1) : recipeName;
    }

    /**
     * Logs a failure event to the modernization failures log file.
     * The log file name includes the last part of the recipe name and a timestamp.
     */
    private void logFailure() {
        String recipeName = getLastPartOfRecipeName(getConfig().getRecipe().getName());
        Path failureLogPath = createLogFilePath(MODERNIZATION_FAILURES_LOG_PREFIX + recipeName);

        ensureLogDirectoryExists(failureLogPath);
        try {
            if (!Files.exists(failureLogPath)) {
                Files.createFile(failureLogPath);
            }
            Set<String> existingEntries = new HashSet<>(Files.readAllLines(failureLogPath));
            String entry = name + ":" + getConfig().getVersion();
            if (!existingEntries.contains(entry)) {
                Files.writeString(
                        failureLogPath,
                        entry + "\n",
                        java.nio.file.StandardOpenOption.CREATE,
                        java.nio.file.StandardOpenOption.APPEND);
            }
        } catch (IOException e) {
            LOG.error("Failed to write to failure log file: " + failureLogPath, e);
        }
    }

    /**
     * Logs a network failure event to the network failures log file.
     * The log file name includes the last part of the recipe name and a timestamp.
     *
     * @param message The network failure message
     */
    public void logNetworkFailure(String message) {
        String recipeName = getLastPartOfRecipeName(getConfig().getRecipe().getName());
        Path networkFailureLogPath = createLogFilePath(NETWORK_FAILURES_LOG_PREFIX + recipeName);

        ensureLogDirectoryExists(networkFailureLogPath);
        try {
            Files.writeString(
                    networkFailureLogPath,
                    name + ": " + message + "\n",
                    java.nio.file.StandardOpenOption.CREATE,
                    java.nio.file.StandardOpenOption.APPEND);
        } catch (IOException e) {
            LOG.error("Failed to write to network failure log file: " + networkFailureLogPath, e);
        }
    }

    /**
     * Raise the last error as exception to the plugin
     * Do nothing if no errors
     */
    public void raiseLastError() throws PluginProcessingException {
        if (!hasErrors()) {
            return;
        }
        throw errors.get(errors.size() - 1);
    }

    /**
     * Remove all errors from the plugin
     */
    public void removeErrors() {
        errors.clear();
    }

    /**
     * Add a tag to the plugin
     * @param tag Tag to add
     * @return Plugin object
     */
    public Plugin addTag(String tag) {
        tags.add(tag);
        return this;
    }

    /**
     * Add tags to the plugin
     * @param tags Tags to add
     * @return Plugin object
     */
    public Plugin addTags(Set<String> tags) {
        this.tags.addAll(tags);
        return this;
    }

    /**
     * Remove tags from the plugin
     * @return Plugin object
     */
    public Plugin withoutTags() {
        tags.clear();
        return this;
    }

    /**
     * Remove errors from the plugin
     * @return Plugin object
     */
    public Plugin withoutErrors() {
        errors.clear();
        return this;
    }

    /**
     * Get the configuration of the plugin
     * @return Configuration
     */
    public Config getConfig() {
        return config;
    }

    /**
     * Get the name of the plugin
     * @return Name of the plugin
     */
    public String getName() {
        return name;
    }

    /**
     * Get the repository name of the plugin
     * @return Repository name of the plugin
     */
    public String getRepositoryName() {
        return repositoryName;
    }

    /**
     * Return if the plugin is local to the system
     * @return True if the plugin is local
     */
    public boolean isLocal() {
        return local;
    }

    /**
     * Get the local repository path
     * @return Local repository path
     */
    public Path getLocalRepository() {
        // We work directly on the location
        if (isLocal()) {
            return localRepository;
        }
        return Settings.getPluginsDirectory(this).resolve("sources");
    }

    /**
     * Get the URI of the repository on the given organization
     * @param organization Organization name (e.g. jenkinsci)
     * @return URI of the repository
     */
    public URI getGitRepositoryURI(String organization) {
        return URI.create("https://github.com/" + organization + "/" + repositoryName + ".git");
    }

    /**
     * Get the path of the JDK directory
     * @return Path of the JDK directory
     */
    public JDK getJDK() {
        return jdk;
    }

    /**
     * Get the path of the log file for the plugin
     * @return Path of the log file
     */
    public Path getLogFile() {
        return Path.of(getName(), "logs", "invoker.logs");
    }

    /**
     * Get the login marker for the plugin
     * @return Marker object
     */
    public Marker getMarker() {
        return MarkerFactory.getMarker(name);
    }

    /**
     * Ensures that the directories exist to store the logs.
     * @param logPath The path to check for the missing directories
     */
    private void ensureLogDirectoryExists(Path logPath) {
        Path parentPath = logPath.getParent();
        if (parentPath != null) {
            try {
                Files.createDirectories(parentPath);
            } catch (IOException e) {
                throw new RuntimeException("Failed to create log directory: " + parentPath, e);
            }
        } else {
            throw new RuntimeException("Parent path is null for log path: " + logPath);
        }
    }

    /**
     * Get the list of tags for the plugin
     * @return List of tags
     */
    public Set<String> getTags() {
        return tags;
    }

    /**
     * Execute maven clean on this plugin
     * @param maven The maven invoker instance
     */
    public void clean(MavenInvoker maven) {
        maven.invokeGoal(this, "clean");
    }

    /**
     * Execute maven compile on this plugin. Compile is skipped if only metadata is required
     * @param maven The maven invoker instance
     */
    public void compile(MavenInvoker maven) {
        if (config.isFetchMetadataOnly()) {
            LOG.info("Skipping compilation for plugin {} as only metadata is required", name);
            return;
        }
        LOG.info(
                "Compiling plugin {} with JDK {} ... Please be patient",
                name,
                this.getJDK().getMajor());
        maven.invokeGoal(this, "compile");
        if (!hasErrors()) {
            LOG.info("Done");
        }
    }

    /**
     * Verify the plugin without tests and quick build using the given maven invoker and JDK.
     * This is useful to run recipes on very outdated plugin
     * @param maven The maven invoker instance
     * @param jdk The JDK to use
     */
    public void verifyQuickBuild(MavenInvoker maven, JDK jdk) {
        LOG.info("Quick build without tests {} using with JDK {} ... Please be patient", name, jdk.getMajor());
        this.withJDK(jdk);
        maven.invokeGoal(this, "verify", "-DskipTests", "-Pquick-build", "-Denforcer.skip=true");
        if (!hasErrors()) {
            LOG.info("Done");
        }
    }

    /**
     * Execute maven verify on this plugin
     * @param maven The maven invoker instance
     */
    public void verify(MavenInvoker maven) {
        if (config.isFetchMetadataOnly()) {
            LOG.info("Skipping verification for plugin {} as only metadata is required", name);
            return;
        }
        LOG.info(
                "Verifying plugin {} with JDK {}... Please be patient",
                name,
                this.getJDK().getMajor());
        maven.invokeGoal(this, "verify");
        LOG.info("Done");
    }

    /**
     * Format the plugin using spotless
     * @param maven The maven invoker instance
     */
    public void format(MavenInvoker maven) {
        if (!isUsingSpotless()) {
            LOG.info("Skipping formatting for plugin {} as it is not using Spotless", name);
            return;
        }
        if (config.isFetchMetadataOnly()) {
            LOG.info("Skipping formatting for plugin {} as only metadata is required", name);
            return;
        }
        LOG.info(
                "Formatting plugin {} with JDK {}... Please be patient",
                name,
                this.getJDK().getMajor());
        maven.invokeGoal(this, "spotless:apply");
        LOG.info("Done");
    }

    /**
     * Enrich the metadata of the plugin and save it
     * @param pluginService The update center service
     */
    public void enrichMetadata(PluginService pluginService) {
        LOG.debug("Setting extra flags for plugin {}", name);
        if (metadata == null) {
            throw new IllegalStateException("Metadata not found for plugin " + name);
        }
        Arrays.stream(MetadataFlag.values())
                .filter(flag -> flag.isApplicable(this, pluginService))
                .forEach(metadata::addFlag);
        this.metadata.save();
    }

    /**
     * Collect plugin metadata
     * @param maven The maven invoker instance
     */
    public void collectMetadata(MavenInvoker maven) {

        XPathFactory xPathFactory = XPathFactory.newInstance();
        XPath xpath = xPathFactory.newXPath();

        // Static parse of the pom file and check for pattern preventing minimal build
        Path pom = getLocalRepository().resolve("pom.xml");
        if (!getLocalRepository().resolve("target").toFile().mkdir()) {
            LOG.trace("Failed to create target directory for plugin {}", name);
        }
        Document document = staticPomParse(pom);

        // Collect precondition errors
        PluginMetadata pluginMetadata = new PluginMetadata();
        pluginMetadata.setCacheManager(buildPluginTargetDirectoryCacheManager());
        pluginMetadata.setErrors(Arrays.stream(PreconditionError.values())
                .filter(error -> error.isApplicable(document, xpath))
                .collect(Collectors.toSet()));

        if (!pluginMetadata.getErrors().isEmpty()) {
            LOG.debug("Precondition errors found for plugin {}", name);
            pluginMetadata.save();
            return;
        }

        // Collect using OpenRewrite
        maven.collectMetadata(this);
    }

    /**
     * Run the openrewrite plugin on this plugin
     * @param maven The maven invoker instance
     */
    public void runOpenRewrite(MavenInvoker maven) {
        withJDK(JDK.JAVA_17);
        if (config.isFetchMetadataOnly()) {
            LOG.info("Skipping OpenRewrite recipe application for plugin {} as only metadata is required", name);
            return;
        }
        maven.invokeRewrite(this);
    }

    /**
     * Fork this plugin
     * @param service The GitHub service
     */
    public void fork(GHService service) {
        if (config.isFetchMetadataOnly()) {
            LOG.debug("Skipping fork for plugin {} as only metadata is required", name);
            return;
        }
        service.fork(this);
    }

    /**
     * Fork sync this plugin
     * @param service The GitHub service
     */
    public void sync(GHService service) {
        if (config.isFetchMetadataOnly()) {
            LOG.debug("Skipping sync for plugin {} as only metadata is required", name);
            return;
        }
        service.sync(this);
    }

    /**
     * Return if this plugin is forked
     * @param service The GitHub service
     */
    public boolean isForked(GHService service) {
        return service.isForked(this);
    }

    /**
     * Return if this plugin is archived
     * @param service The GitHub service
     * @return True if the plugin is archived
     */
    public boolean isArchived(GHService service) {
        return service.isArchived(this);
    }

    /**
     * Return if this plugin is deprecated in the update center
     * @param pluginService The update center service
     * @return True if the plugin is deprecated
     */
    public boolean isDeprecated(PluginService pluginService) {
        return pluginService.isDeprecated(this);
    }

    /**
     * Return if this plugin is deprecated in the update center
     * @return True if the plugin is deprecated
     */
    public boolean isDeprecated() {
        return hasMetadata() && metadata.hasFlag(MetadataFlag.IS_DEPRECATED);
    }

    /**
     * Return if this plugin is an API plugin
     * @param pluginService The update center service
     * @return True if the plugin is an API plugin
     */
    public boolean isApiPlugin(PluginService pluginService) {
        return pluginService.isApiPlugin(this);
    }

    /**
     * Return if this plugin is an API plugin
     * @return True if the plugin is an API plugin
     */
    public boolean isApiPlugin() {
        return hasMetadata() && metadata.hasFlag(MetadataFlag.IS_API_PLUGIN);
    }

    /**
     * Delete the plugin fork
     * @param service  The GitHub service
     */
    public void deleteFork(GHService service) {
        service.deleteFork(this);
    }

    /**
     * Checkout the plugin branch
     * @param service The GitHub service
     */
    public void checkoutBranch(GHService service) {
        service.checkoutBranch(this);
    }

    /**
     * Commit the changes to the plugin repository
     * @param service The GitHub service
     */
    public void commit(GHService service) {
        service.commitChanges(this);
    }

    /**
     * Push the changes to the plugin repository
     * @param service The GitHub service
     */
    public void push(GHService service) {
        service.pushChanges(this);
    }

    /**
     * Open a pull request for the plugin
     * @param service The GitHub service
     */
    public void openPullRequest(GHService service) {
        service.openPullRequest(this);
    }

    /**
     * Fetch the plugin code into local directory
     * @param service The GitHub service
     */
    public void fetch(GHService service) {
        service.fetch(this);
    }

    /**
     * Get the associated repository for this plugin
     * @param service The GitHub service
     * @return The repository object
     */
    public GHRepository getRemoteRepository(GHService service) {
        return service.getRepository(this);
    }

    /**
     * Get the associated fork repository for this plugin
     * @param service The GitHub service
     * @return The repository object
     */
    public GHRepository getRemoteForkRepository(GHService service) {
        return service.getRepositoryFork(this);
    }

    /**
     * Get the metadata of the plugin
     * @return Plugin metadata
     */
    public PluginMetadata getMetadata() {
        return metadata;
    }

    /**
     * Return if the plugin has metadata loaded in memory
     * @return True if the plugin has metadata loaded
     */
    public boolean hasMetadata() {
        return metadata != null;
    }

    /**
     * Set the metadata of the plugin
     * @param metadata Plugin metadata
     */
    public void setMetadata(PluginMetadata metadata) {
        this.metadata = metadata;
    }

    /**
     * Load metadata from cache
     * @param cacheManager The cache manager
     */
    public void loadMetadata(CacheManager cacheManager) {
        setMetadata(cacheManager.get(Path.of(getName()), CacheManager.PLUGIN_METADATA_CACHE_KEY, PluginMetadata.class));
    }

    /**
     * Copy metadata from plugin target directory to cache
     * @param cacheManager The cache manager
     */
    public void copyMetadata(CacheManager cacheManager) {
        CacheManager pluginCacheManager = buildPluginTargetDirectoryCacheManager();
        setMetadata(pluginCacheManager.copy(
                cacheManager,
                Path.of(getName()),
                CacheManager.PLUGIN_METADATA_CACHE_KEY,
                new PluginMetadata(pluginCacheManager)));
        LOG.debug(
                "Copied plugin {} metadata to cache: {}",
                getName(),
                getMetadata().getLocation().toAbsolutePath());
    }

    /**
     * Add a modified file to the plugin
     * @param files The files to add
     */
    public void addModifiedFiles(Collection<String> files) {
        modifiedFiles.addAll(files);
    }

    /**
     * Return a set of modified files in the plugin
     * @return Set of modified files
     */
    public Set<String> getModifiedFiles() {
        return Set.of(modifiedFiles.toArray(new String[0]));
    }

    /**
     * Build cache manager for this plugin
     * @return Cache manager
     */
    private CacheManager buildPluginTargetDirectoryCacheManager() {
        // This is an absolute path
        if (isLocal()) {
            return new CacheManager(getLocalRepository().resolve("target"));
        }
        // This is a relative path to the cache manager root
        return new CacheManager(
                Settings.getPluginsDirectory(this).resolve(getLocalRepository().resolve("target")));
    }

    /**
     * Static parse of the pom file to a XML document
     * @param pom The path to the pom file
     * @return The XML document
     */
    private Document staticPomParse(Path pom) {
        if (pom == null || !pom.toFile().exists()) {
            addError("No pom file found");
            raiseLastError();
            return null;
        }
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            return builder.parse(pom.toFile());
        } catch (Exception e) {
            addError("Failed to parse pom file: " + pom, e);
            raiseLastError();
            return null;
        }
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Plugin plugin = (Plugin) o;
        return Objects.equals(name, plugin.name);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(name);
    }
}
