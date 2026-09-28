package io.jenkins.tools.pluginmodernizer.core.impl;

import edu.umd.cs.findbugs.annotations.Nullable;
import io.jenkins.tools.pluginmodernizer.core.config.Config;
import io.jenkins.tools.pluginmodernizer.core.config.Settings;
import io.jenkins.tools.pluginmodernizer.core.model.JDK;
import io.jenkins.tools.pluginmodernizer.core.model.ModernizerException;
import io.jenkins.tools.pluginmodernizer.core.model.Plugin;
import io.jenkins.tools.pluginmodernizer.core.model.PluginProcessingException;
import io.jenkins.tools.pluginmodernizer.core.model.Recipe;
import io.jenkins.tools.pluginmodernizer.core.utils.JdkFetcher;
import jakarta.inject.Inject;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.InvocationRequest;
import org.apache.maven.shared.invoker.InvocationResult;
import org.apache.maven.shared.invoker.Invoker;
import org.apache.maven.shared.invoker.MavenInvocationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MavenInvoker {

    /**
     * The logger to use
     */
    private static final Logger LOG = LoggerFactory.getLogger(MavenInvoker.class);

    /**
     * The configuration to use
     */
    @Inject
    private Config config;

    /**
     * The JDK fetcher to use
     */
    @Inject
    private JdkFetcher jdkFetcher;

    @Inject
    private Invoker invoker;

    /**
     * Get the maven version
     * @return The maven version
     */
    public @Nullable ComparableVersion getMavenVersion() {
        AtomicReference<String> version = new AtomicReference<>();
        try {
            InvocationRequest request = new DefaultInvocationRequest();
            request.setMavenHome(getEffectiveMavenHome().toFile());
            request.setBatchMode(true);
            request.addArg("-q");
            request.addArg("--version");
            request.setOutputHandler(version::set);
            invoker.execute(request);
            String versionValue = version.get();
            if (versionValue == null) {
                LOG.error("Failed to check for maven version. Make sure Maven and Java are installed correctly.");
                return null;
            }
            return new ComparableVersion(versionValue);
        } catch (MavenInvocationException e) {
            LOG.error("Failed to check for maven version", e);
            return null;
        }
    }

    /**
     * Invoke a goal on a plugin
     * @param plugin The plugin to run the goal on
     * @param goals The goals to run. For example, "clean"
     */
    public void invokeGoal(Plugin plugin, String... goals) {
        LOG.debug("Running {} phase for plugin {}", goals, plugin.getName());
        LOG.debug(
                "Running maven on directory {}",
                plugin.getLocalRepository().toAbsolutePath().toFile());
        invokeGoals(plugin, goals);
    }

    /**
     * Invoke the rewrite modernization for a given plugin
     * @param plugin The plugin to run the rewrite on
     */
    public void collectMetadata(Plugin plugin) {
        LOG.info("Collecting metadata for plugin {}... Please be patient", plugin);
        invokeGoals(plugin, getSingleRecipeArgs(Settings.FETCH_METADATA_RECIPE));
        LOG.info("Done");
    }

    /**
     * Invoke the rewrite modernization for a given plugin
     * @param plugin The plugin to run the rewrite on
     */
    public void invokeRewrite(Plugin plugin) {
        plugin.addTags(config.getRecipe().getTags());
        LOG.info(
                "Running recipes {} for plugin {}... Please be patient",
                config.getRecipe().getName(),
                plugin);
        invokeGoals(plugin, getSingleRecipeArgs(config.getRecipe()));
        LOG.info("Done");
    }

    /**
     * Get the rewrite arguments to be executed for metadata collection
     * @return The list of arguments to be passed to the rewrite plugin
     */
    private String[] getSingleRecipeArgs(Recipe recipe) {
        List<String> goals = new ArrayList<>();
        String goal = recipe.isSkipVerification() ? "runNoFork" : "run";
        goals.add("org.openrewrite.maven:rewrite-maven-plugin:" + Settings.MAVEN_REWRITE_PLUGIN_VERSION + ":" + goal);
        goals.add("-Denforcer.skip=true");
        goals.add("-Dhpi.validate.skip=true");
        goals.add("-Dmaven.antrun.skip=true");
        goals.add("-Dmaven.repo.local=%s".formatted(config.getMavenLocalRepo()));
        goals.add("-Drewrite.activeRecipes=" + recipe.getName());
        goals.add("-Drewrite.recipeArtifactCoordinates=io.jenkins.plugin-modernizer:plugin-modernizer-core:"
                + config.getVersion());
        return goals.toArray(String[]::new);
    }

    /**
     * Invoke a list of maven goal on the plugin
     * @param plugin The plugin to run the goals on
     * @param goals The list of goals to run
     */
    private void invokeGoals(Plugin plugin, String... goals) {
        validatePom(plugin);
        try {
            InvocationRequest request = createInvocationRequest(plugin, goals);
            JDK jdk = plugin.getJDK();
            if (jdk != null) {
                Path jdkPath = jdk.getHome(jdkFetcher);
                request.setJavaHome(jdkPath.toFile());
                LOG.debug("JDK home: {}", jdkPath);

                // In order to rewrite on outdated plugins set add-opens
                if (jdk.getMajor() >= 17) {
                    LOG.debug("Adding --add-opens for JDK 17+");
                    request.setMavenOpts(
                            "--add-opens java.base/java.lang=ALL-UNNAMED --add-opens java.base/java.io=ALL-UNNAMED --add-opens java.base/java.util=ALL-UNNAMED");
                }
            }
            request.setBatchMode(true);
            request.setNoTransferProgress(false);
            request.setErrorHandler((message) -> {
                LOG.error(plugin.getMarker(), String.format("Something went wrong when running maven: %s", message));
            });
            request.setOutputHandler((message) -> {
                LOG.info(plugin.getMarker(), message);
            });
            InvocationResult result = invoker.execute(request);
            handleInvocationResult(plugin, result);
        } catch (MavenInvocationException | InterruptedException | IOException e) {
            plugin.addError("Maven invocation failed", e);
        }
    }

    /**
     * Validate a pom exist for the given plugin
     * @param plugin The plugin to validate
     */
    private void validatePom(Plugin plugin) {
        LOG.debug("Check POM exist for plugin: {}", plugin);
        if (!plugin.getLocalRepository().resolve("pom.xml").toFile().isFile()) {
            plugin.addError("POM file not found");
            throw new PluginProcessingException("POM file not found", plugin);
        }
    }

    /**
     * Validate the Maven home and local repo directory.
     * @throws IllegalArgumentException if the Maven home directory is not set or invalid.
     */
    public void validateMaven() {
        Path mavenHome = getEffectiveMavenHome();

        Path mvnUnix = mavenHome.resolve("bin/mvn");
        Path mvnCmd = mavenHome.resolve("bin/mvn.cmd");
        Path mvnBat = mavenHome.resolve("bin/mvn.bat");

        if (!Files.isDirectory(mavenHome)
                || (!mvnUnix.toFile().canExecute()
                        && !mvnCmd.toFile().exists()
                        && !mvnBat.toFile().exists())) {
            throw new ModernizerException("Invalid Maven home directory at '%s'.".formatted(mavenHome));
        }

        Path mavenLocalRepo = config.getMavenLocalRepo();
        if (mavenLocalRepo == null) {
            throw new ModernizerException("Maven local repository is not set.");
        }
        if (!Files.isDirectory(mavenLocalRepo)) {
            throw new ModernizerException("Invalid Maven local repository at '%s'.".formatted(mavenLocalRepo));
        }
    }

    @SuppressWarnings("OS_COMMAND_INJECTION")
    @Nullable
    private Path detectMavenHome() {
        String os = System.getProperty("os.name");
        if (os == null) {
            os = "";
        }

        ProcessBuilder processBuilder;
        if (os.toLowerCase().contains("win")) {
            processBuilder = new ProcessBuilder("where", "mvn");
        } else {
            processBuilder = new ProcessBuilder("which", "mvn");
        }
        processBuilder.redirectErrorStream(true);

        String mvnPath = null;
        StringBuilder output = new StringBuilder();
        Process process = null;

        try {
            process = processBuilder.start();

            try (BufferedReader reader =
                    new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (output.length() > 0) {
                        output.append('\n');
                    }
                    output.append(line);

                    if (mvnPath == null && !line.isBlank()) {
                        mvnPath = line;
                    }
                }
            }

            int exitCode = process.waitFor();
            if (exitCode != 0 || mvnPath == null || mvnPath.isBlank()) {
                LOG.debug("Maven not found in PATH (exitCode=" + exitCode + ", output=" + sanitize(output.toString())
                        + ")");
                return null;
            }

            Path mvn = Path.of(mvnPath).toRealPath();
            Path binDir = mvn.getParent();
            if (binDir == null) {
                LOG.debug("Failed to detect Maven home from mvn path (no parent): " + sanitize(mvnPath));
                return null;
            }

            Path mavenHome = binDir.getParent();
            if (mavenHome == null) {
                LOG.debug("Failed to detect Maven home from mvn path (no grandparent): " + sanitize(mvnPath));
                return null;
            }
            return mavenHome;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOG.debug("Interrupted while detecting Maven from PATH", e);
            return null;
        } catch (Exception e) {
            LOG.debug("Failed to detect Maven from PATH", e);
            return null;
        } finally {
            if (process != null) {
                process.destroy();
            }
        }
    }

    private boolean isValidMavenHome(Path mavenHome) {
        if (mavenHome == null || !Files.isDirectory(mavenHome)) {
            return false;
        }

        Path mvnUnix = mavenHome.resolve("bin/mvn");
        Path mvnCmd = mavenHome.resolve("bin/mvn.cmd");
        Path mvnBat = mavenHome.resolve("bin/mvn.bat");
        return mvnUnix.toFile().canExecute()
                || mvnCmd.toFile().exists()
                || mvnBat.toFile().exists();
    }

    private String sanitize(String input) {
        return input == null ? null : input.replaceAll("[\\r\\n]", "");
    }

    private Path getEffectiveMavenHome() {
        Path configured = config.getConfiguredMavenHome();
        if (configured != null) {
            if (isValidMavenHome(configured)) {
                return configured;
            }
            LOG.warn("Configured Maven home is invalid: " + sanitize(configured.toString())
                    + ". Falling back to PATH detection.");
        }

        Path cachedDetected = config.getDetectedMavenHome();
        if (cachedDetected != null) {
            if (isValidMavenHome(cachedDetected)) {
                return cachedDetected;
            }
            LOG.debug("Cached detected Maven home is invalid: " + sanitize(cachedDetected.toString()));
        }

        Path detected = detectMavenHome();
        if (detected != null && isValidMavenHome(detected)) {
            LOG.info("Detected Maven home from PATH: " + sanitize(detected.toString()));
            config.setMavenHome(detected);
            return detected;
        }

        throw new ModernizerException("Maven not found. Please set MAVEN_HOME or ensure 'mvn' is available in PATH.");
    }

    /**
     * Validate the Maven version.
     * @throws IllegalArgumentException if the Maven version is too old or cannot be determined.
     */
    public void validateMavenVersion() {
        ComparableVersion mavenVersion = getMavenVersion();
        LOG.debug("Maven version detected: {}", mavenVersion);
        if (mavenVersion == null) {
            LOG.error("Failed to check Maven version. Aborting build.");
            throw new ModernizerException("Failed to check Maven version.");
        }
        if (mavenVersion.compareTo(Settings.MAVEN_MINIMAL_VERSION) < 0) {
            LOG.error(
                    "Maven version detected {}, is too old. Please use at least version {}",
                    mavenVersion,
                    Settings.MAVEN_MINIMAL_VERSION);
            throw new ModernizerException("Maven version is too old.");
        }
    }

    /**
     * Create an invocation request for the plugin.
     * @param plugin The plugin to run the goals on
     * @param args The list of args
     * @return The invocation request
     */
    private InvocationRequest createInvocationRequest(Plugin plugin, String... args) {
        InvocationRequest request = new DefaultInvocationRequest();
        request.setMavenHome(getEffectiveMavenHome().toFile());
        request.setPomFile(plugin.getLocalRepository().resolve("pom.xml").toFile());
        request.addArgs(List.of(args));
        if (Config.isDebug()) {
            request.addArg("-X");
        }
        return request;
    }

    /**
     * Handle invocation result for the plugin
     * @param plugin The plugin
     * @param result The invocation result
     */
    private void handleInvocationResult(Plugin plugin, InvocationResult result) {
        if (result.getExitCode() != 0) {
            LOG.error(plugin.getMarker(), "Build failed with code: {}", result.getExitCode());
            if (result.getExecutionException() != null) {
                plugin.addError("Maven generic exception occurred", result.getExecutionException());
            } else {
                String errorMessage;
                if (Config.isDebug()) {
                    errorMessage = "Build failed with code: " + result.getExitCode();
                } else {
                    errorMessage = "Build failed";
                }
                plugin.addError(errorMessage);
            }
        }
    }
}
