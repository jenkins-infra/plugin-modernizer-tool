package io.jenkins.tools.pluginmodernizer.cli;

import static org.junit.jupiter.api.Assertions.*;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import io.jenkins.tools.pluginmodernizer.cli.utils.GitHubServerContainer;
import io.jenkins.tools.pluginmodernizer.cli.utils.ModernizerTestWatcher;
import io.jenkins.tools.pluginmodernizer.core.extractor.ArchetypeCommonFile;
import io.jenkins.tools.pluginmodernizer.core.extractor.PluginMetadata;
import io.jenkins.tools.pluginmodernizer.core.impl.CacheManager;
import io.jenkins.tools.pluginmodernizer.core.model.Plugin;
import io.jenkins.tools.pluginmodernizer.core.utils.JsonUtils;
import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.Security;
import java.util.Properties;
import java.util.stream.Stream;
import org.apache.commons.io.FileUtils;
import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.DefaultInvoker;
import org.apache.maven.shared.invoker.InvocationRequest;
import org.apache.maven.shared.invoker.InvocationResult;
import org.apache.maven.shared.invoker.Invoker;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.CleanupMode;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.shaded.org.bouncycastle.jce.provider.BouncyCastleProvider;

/**
 * Integration test for the command line interface
 */
@WireMockTest
@Testcontainers(disabledWithoutDocker = true)
@ExtendWith(ModernizerTestWatcher.class)
@Execution(ExecutionMode.SAME_THREAD) // Need split or not using shared resources like logs folder
public class CommandLineITCase {

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    /**
     * Logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(CommandLineITCase.class);

    /**
     * Tests plugins
     * @return the plugins
     */
    private static Stream<Arguments> testsPlugins() {
        return Stream.of(
                Arguments.of(new PluginMetadata() {
                    {
                        setPluginName("empty");
                        setJenkinsVersion("2.452.4");
                    }
                }),
                Arguments.of(new PluginMetadata() {
                    {
                        setPluginName("empty-no-bom");
                        setJenkinsVersion("2.452.4");
                    }
                }),
                Arguments.of(new PluginMetadata() {
                    {
                        setPluginName("replace-by-api-plugins");
                        setJenkinsVersion("2.452.4");
                    }
                }),
                // Similar to vagrant plugin pom
                Arguments.of(new PluginMetadata() {
                    {
                        setPluginName("jenkins-1.5");
                        setJenkinsVersion("1.532.3");
                    }
                }),
                // Based on pyenv-pipeline plugin pom
                Arguments.of(new PluginMetadata() {
                    {
                        setPluginName("jenkins-1.6");
                        setJenkinsVersion("1.651.2");
                    }
                }),
                // Based on adaptive-disconnector plugin pom
                Arguments.of(new PluginMetadata() {
                    {
                        setPluginName("missing-relative-path-and-http-url");
                        setJenkinsVersion("1.609");
                    }
                }));
    }

    // Path for logs
    private final Path logFolder = Path.of("logs");

    // Allow to debug source code after the test as run
    @TempDir(cleanup = CleanupMode.NEVER)
    private Path cachePath;

    @TempDir
    private Path keysPath;

    @BeforeEach
    public void beforeEach() throws Exception {
        if (!Files.isDirectory(logFolder)) {
            Files.createDirectory(logFolder);
        }
    }

    @Test
    @Tag("Always")
    public void testVersion() throws Exception {
        Path logFile = setupLogs("testVersion");
        Invoker invoker = buildInvoker();
        InvocationResult result1 = invoker.execute(buildRequest("--version", logFile));
        assertAll(
                () -> assertEquals(0, result1.getExitCode()),
                () -> assertTrue(Files.readAllLines(logFile).stream()
                        .anyMatch(line -> line.matches("plugin modernizer ([a-zA-Z0-9.\\-_]+) (.*)"))));

        InvocationResult result2 = invoker.execute(buildRequest("version", logFile));
        assertAll(
                () -> assertEquals(0, result2.getExitCode()),
                () -> assertTrue(Files.readAllLines(logFile).stream()
                        .anyMatch(line -> line.matches("plugin modernizer ([a-zA-Z0-9.\\-_]+) (.*)"))));
        InvocationResult result3 = invoker.execute(buildRequest("version --short", logFile));
        assertAll(
                () -> assertEquals(0, result3.getExitCode()),
                () -> assertTrue(Files.readAllLines(logFile).stream()
                        .anyMatch(line -> line.matches("(.*)\\s*[a-zA-Z0-9.\\-_]+\\s*"))));
    }

    @Test
    @Tag("Always")
    public void testHelp() throws Exception {
        Path logFile = setupLogs("testHelp");
        Invoker invoker = buildInvoker();
        InvocationRequest request = buildRequest("--help", logFile);
        InvocationResult result = invoker.execute(request);
        assertAll(
                () -> assertEquals(0, result.getExitCode()),
                () -> assertTrue(Files.readAllLines(logFile).stream()
                        .anyMatch(line -> line.matches("(.*)Usage: plugin-modernizer (.*) COMMAND(.*)"))));
    }

    @Test
    @Tag("Slow")
    public void testCleanupWithDryRun() throws Exception {
        Path logFile = setupLogs("testCleanupWithDryRun");
        Invoker invoker = buildInvoker();
        InvocationRequest request = buildRequest("cleanup --cache-path %s --dry-run".formatted(cachePath), logFile);
        InvocationResult result = invoker.execute(request);
        assertAll(
                () -> assertEquals(0, result.getExitCode()),
                () -> assertTrue(Files.readAllLines(logFile).stream()
                        .anyMatch(line -> line.matches("(.*)Would remove path: (.*)"))));
    }

    @Test
    @Tag("Slow")
    public void testCleanup() throws Exception {
        Path logFile = setupLogs("testCleanup");
        Invoker invoker = buildInvoker();
        InvocationRequest request = buildRequest("cleanup --cache-path %s".formatted(cachePath), logFile);
        InvocationResult result = invoker.execute(request);
        assertAll(
                () -> assertEquals(0, result.getExitCode()),
                () -> assertTrue(
                        Files.readAllLines(logFile).stream().anyMatch(line -> line.matches("(.*)Removed path: (.*)"))));
    }

    @Test
    @Tag("Slow")
    public void testValidateWithSshKey(WireMockRuntimeInfo wmRuntimeInfo) throws Exception {

        Path logFile = setupLogs("testValidateWithSshKey");

        // Setup
        WireMock wireMock = wmRuntimeInfo.getWireMock();
        wireMock.register(WireMock.get(WireMock.urlEqualTo("/api/user"))
                .willReturn(
                        WireMock.jsonResponse(new GitHubServerContainer.UserApiResponse("fake-owner", "User"), 200)));

        Invoker invoker = buildInvoker();
        InvocationRequest request = buildRequest(
                "validate --maven-home %s --ssh-private-key %s --debug --github-api-url %s/api"
                        .formatted(
                                getModernizerMavenHome(),
                                generatePrivateKey("testValidateWithSshKey"),
                                wmRuntimeInfo.getHttpBaseUrl()),
                logFile);
        InvocationResult result = invoker.execute(request);
        assertAll(
                () -> assertEquals(0, result.getExitCode()),
                () -> assertTrue(Files.readAllLines(logFile).stream()
                        .anyMatch(line -> line.matches("(.*)GitHub owner: fake-owner(.*)"))),
                () -> assertTrue(Files.readAllLines(logFile).stream()
                        .anyMatch(line -> line.matches("(.*)Validation successful(.*)"))));
    }

    @Test
    @Tag("Slow")
    public void testValidate(WireMockRuntimeInfo wmRuntimeInfo) throws Exception {

        Path logFile = setupLogs("testValidate");

        // Setup
        WireMock wireMock = wmRuntimeInfo.getWireMock();
        wireMock.register(WireMock.get(WireMock.urlEqualTo("/api/user"))
                .willReturn(
                        WireMock.jsonResponse(new GitHubServerContainer.UserApiResponse("fake-owner", "User"), 200)));

        Invoker invoker = buildInvoker();
        InvocationRequest request = buildRequest(
                "validate --debug --github-api-url %s/api".formatted(wmRuntimeInfo.getHttpBaseUrl()), logFile);
        InvocationResult result = invoker.execute(request);
        assertAll(
                () -> assertEquals(0, result.getExitCode()),
                () -> assertTrue(Files.readAllLines(logFile).stream()
                        .anyMatch(line -> line.matches("(.*)GitHub owner: fake-owner(.*)"))),
                () -> assertTrue(Files.readAllLines(logFile).stream()
                        .anyMatch(line -> line.matches("(.*)Validation successful(.*)"))));
    }

    @Test
    @Tag("Slow")
    public void testListRecipes() throws Exception {
        Path logFile = setupLogs("testListRecipes");
        Invoker invoker = buildInvoker();
        InvocationRequest request = buildRequest("recipes", logFile);
        InvocationResult result = invoker.execute(request);
        assertAll(
                () -> assertEquals(0, result.getExitCode()),
                () -> assertTrue(
                        Files.readAllLines(logFile).stream().noneMatch(line -> line.matches("conditions\\..*"))),
                () -> assertTrue(Files.readAllLines(logFile).stream()
                        .anyMatch(
                                line -> line.matches(".*FetchMetadata - Extracts metadata from a Jenkins plugin.*"))));
    }

    @ParameterizedTest
    @MethodSource("testsPlugins")
    @Tag("Slow")
    public void testBuildMetadata(PluginMetadata expectedMetadata, WireMockRuntimeInfo wmRuntimeInfo) throws Exception {

        Path logFile = logFolder.resolve("testBuildMetadata-%s.txt".formatted(expectedMetadata.getPluginName()));
        Files.deleteIfExists(logFile);
        Files.createFile(logFile);
        String plugin = expectedMetadata.getPluginName();

        // Junit attachment with logs file for the plugin build
        System.out.printf(
                "[[ATTACHMENT|%s]]%n", Plugin.build(plugin).getLogFile().toAbsolutePath());
        System.out.printf("[[ATTACHMENT|%s]]%n", logFile.toAbsolutePath());

        try (GitHubServerContainer gitRemote = new GitHubServerContainer(wmRuntimeInfo, keysPath, plugin, "main")) {

            gitRemote.start();

            Invoker invoker = buildInvoker();
            InvocationRequest request = buildRequest(
                    "build-metadata %s".formatted(getRunArgs(wmRuntimeInfo, Plugin.build(plugin))), logFile);
            InvocationResult result = invoker.execute(request);

            // Assert output
            assertAll(
                    () -> assertEquals(0, result.getExitCode()),
                    () -> assertTrue(Files.readAllLines(logFile).stream()
                            .anyMatch(line -> line.matches("(.*)GitHub owner: fake-owner(.*)"))),
                    () -> assertTrue(Files.readAllLines(logFile).stream()
                            .anyMatch(line ->
                                    line.matches(".*Metadata was fetched for plugin (.*) and is available at.*"))));

            // Assert some metadata
            PluginMetadata metadata = JsonUtils.fromJson(
                    cachePath
                            .resolve("jenkins-plugin-modernizer-cli")
                            .resolve(plugin)
                            .resolve(CacheManager.PLUGIN_METADATA_CACHE_KEY),
                    PluginMetadata.class);

            // Metadata should be still present on target folder (it should be copied to be reused by recipes not aware
            // of the
            // cache or external storage)
            assertTrue(Files.exists(cachePath
                    .resolve("jenkins-plugin-modernizer-cli")
                    .resolve(plugin)
                    .resolve("sources")
                    .resolve("target")
                    .resolve(CacheManager.PLUGIN_METADATA_CACHE_KEY)));

            assertEquals(expectedMetadata.getJenkinsVersion(), metadata.getJenkinsVersion());
        }
    }

    @Test
    @Tag("Slow")
    public void testDryRunReplaceLibrariesWithApiPlugin(WireMockRuntimeInfo wmRuntimeInfo) throws Exception {

        Path logFile1 = setupLogs("testDryRunReplaceLibrariesWithApiPlugin1");
        Path logFile2 = setupLogs("testDryRunReplaceLibrariesWithApiPlugin2");

        final String plugin = "replace-by-api-plugins";
        final String recipe = "ReplaceLibrariesWithApiPlugin";

        // Junit attachment with logs file for the plugin build
        System.out.printf(
                "[[ATTACHMENT|%s]]%n", Plugin.build(plugin).getLogFile().toAbsolutePath());
        System.out.printf("[[ATTACHMENT|%s]]%n", logFile1.toAbsolutePath());
        System.out.printf("[[ATTACHMENT|%s]]%n", logFile2.toAbsolutePath());

        try (GitHubServerContainer gitRemote = new GitHubServerContainer(wmRuntimeInfo, keysPath, plugin, "main")) {

            gitRemote.start();

            Invoker invoker = buildInvoker();
            InvocationRequest request = buildRequest(
                    "dry-run --recipe %s %s".formatted(recipe, getRunArgs(wmRuntimeInfo, Plugin.build(plugin))),
                    logFile1);
            InvocationResult result = invoker.execute(request);

            // Assert output
            assertAll(
                    () -> assertEquals(0, result.getExitCode()),
                    () -> assertTrue(Files.readAllLines(logFile1).stream()
                            .anyMatch(line -> line.matches("(.*)Dry run mode. Changes were commited on (.*)"))));

            // Delete target folder to use data from cache
            File targetDirectory = cachePath
                    .resolve("jenkins-plugin-modernizer-cli")
                    .resolve(plugin)
                    .resolve("sources")
                    .resolve("target")
                    .toFile();
            FileUtils.deleteDirectory(targetDirectory);

            // Ensure metadata is still present on cache
            assertTrue(Files.exists(cachePath
                    .resolve("jenkins-plugin-modernizer-cli")
                    .resolve(plugin)
                    .resolve(CacheManager.PLUGIN_METADATA_CACHE_KEY)));

            // Ensure it still works after running again (with caching)
            InvocationRequest request2 = buildRequest(
                    "dry-run --recipe %s %s".formatted(recipe, getRunArgs(wmRuntimeInfo, Plugin.build(plugin))),
                    logFile2);
            InvocationResult result2 = invoker.execute(request2);
            assertAll(
                    () -> assertEquals(0, result2.getExitCode()),
                    () -> assertTrue(Files.readAllLines(logFile2).stream()
                            .anyMatch(line -> line.matches("(.*)Dry run mode. Changes were commited on (.*)"))));
        }
    }

    @Test
    @Tag("Slow")
    public void testRunAddDependabot(WireMockRuntimeInfo wmRuntimeInfo) throws Exception {

        Path logFile = setupLogs("testRunAddDependabot");

        final String plugin = "empty";
        final String recipe = "SetupDependabot";

        // Junit attachment with logs file for the plugin build
        System.out.printf(
                "[[ATTACHMENT|%s]]%n", Plugin.build(plugin).getLogFile().toAbsolutePath());
        System.out.printf("[[ATTACHMENT|%s]]%n", logFile.toAbsolutePath());

        try (GitHubServerContainer gitRemote = new GitHubServerContainer(wmRuntimeInfo, keysPath, plugin, "main")) {

            gitRemote.start();

            Invoker invoker = buildInvoker();
            InvocationRequest request = buildRequest(
                    "run --recipe %s %s".formatted(recipe, getRunArgs(wmRuntimeInfo, Plugin.build(plugin))), logFile);
            InvocationResult result = invoker.execute(request);

            // Assert output
            assertAll(
                    () -> assertEquals(0, result.getExitCode()),
                    () -> assertTrue(Files.readAllLines(logFile).stream()
                            .anyMatch(line -> line.matches("(.*)Skipping verification (.*)"))),
                    () -> assertTrue(Files.readAllLines(logFile).stream()
                            .anyMatch(line -> line.matches("(.*)Pull request was open on (.*)"))));

            // Check that new file was created
            assertTrue(
                    Files.exists(cachePath
                            .resolve("jenkins-plugin-modernizer-cli")
                            .resolve(plugin)
                            .resolve("sources")
                            .resolve(".github")
                            .resolve("dependabot.yml")),
                    "Dependabot file was not created");
        }
    }

    @Test
    @Tag("Slow")
    public void testDryRunAddDependabot(WireMockRuntimeInfo wmRuntimeInfo) throws Exception {

        Path logFile = setupLogs("testDryRunAddDependabot");

        final String plugin = "empty";
        final String recipe = "SetupDependabot";

        // Junit attachment with logs file for the plugin build
        System.out.printf(
                "[[ATTACHMENT|%s]]%n", Plugin.build(plugin).getLogFile().toAbsolutePath());
        System.out.printf("[[ATTACHMENT|%s]]%n", logFile.toAbsolutePath());

        try (GitHubServerContainer gitRemote = new GitHubServerContainer(wmRuntimeInfo, keysPath, plugin, "main")) {

            gitRemote.start();

            Invoker invoker = buildInvoker();
            InvocationRequest request = buildRequest(
                    "dry-run --recipe %s %s".formatted(recipe, getRunArgs(wmRuntimeInfo, Plugin.build(plugin))),
                    logFile);
            InvocationResult result = invoker.execute(request);

            // Assert output
            assertAll(
                    () -> assertEquals(0, result.getExitCode()),
                    () -> assertTrue(Files.readAllLines(logFile).stream()
                            .anyMatch(line -> line.matches("(.*)Dry run mode. Changes were commited on (.*)"))));

            // Check that new file was created
            assertTrue(
                    Files.exists(cachePath
                            .resolve("jenkins-plugin-modernizer-cli")
                            .resolve(plugin)
                            .resolve("sources")
                            .resolve(".github")
                            .resolve("dependabot.yml")),
                    "Dependabot file was not created");
        }
    }

    @Test
    @Tag("Slow")
    public void testRecipeOnLocalPlugin(WireMockRuntimeInfo wmRuntimeInfo) throws Exception {

        Path logFile = setupLogs("testRecipeOnLocalPlugin");

        // Copy empty plugin to cache and use as local plugin
        final String plugin = "empty";
        final Path pluginPath = Path.of("src/test/resources").resolve(plugin);
        Path targetPath = cachePath
                .resolve("jenkins-plugin-modernizer-cli")
                .resolve(plugin)
                .resolve("sources");
        FileUtils.copyDirectory(pluginPath.toFile(), targetPath.toFile());

        final String recipe = "SetupSecurityScan";

        try (GitHubServerContainer gitRemote = new GitHubServerContainer(wmRuntimeInfo, keysPath, plugin, "main")) {

            gitRemote.start();

            // Junit attachment with logs file for the plugin build
            System.out.printf(
                    "[[ATTACHMENT|%s]]%n", Plugin.build(plugin).getLogFile().toAbsolutePath());
            System.out.printf("[[ATTACHMENT|%s]]%n", logFile.toAbsolutePath());

            Invoker invoker = buildInvoker();
            InvocationRequest request = buildRequest(
                    "dry-run --recipe %s %s"
                            .formatted(recipe, getRunArgs(wmRuntimeInfo, Plugin.build(plugin, targetPath))),
                    logFile);
            InvocationResult result = invoker.execute(request);

            // Assert output
            assertAll(
                    () -> assertEquals(0, result.getExitCode()),
                    () -> assertTrue(Files.readAllLines(logFile).stream()
                            .anyMatch(line -> line.matches("(.*)Dry run mode. Changes were commited on (.*)"))));

            // Check that new file was created
            assertTrue(
                    Files.exists(targetPath.resolve(ArchetypeCommonFile.WORKFLOW_SECURITY.getPath())),
                    "Workflow security file was not created");
        }
    }

    @Test
    @Tag("Slow")
    public void testRecipeOnLocalPluginWithRunMode(WireMockRuntimeInfo wmRuntimeInfo) throws Exception {

        Path logFile = setupLogs("testRecipeOnLocalPluginWithRunMode");

        // Copy empty plugin to cache and use as local plugin
        final String plugin = "empty";
        final Path pluginPath = Path.of("src/test/resources").resolve(plugin);
        Path targetPath = cachePath
                .resolve("jenkins-plugin-modernizer-cli")
                .resolve(plugin)
                .resolve("sources");
        FileUtils.copyDirectory(pluginPath.toFile(), targetPath.toFile());

        final String recipe = "AddCodeOwner";

        try (GitHubServerContainer gitRemote = new GitHubServerContainer(wmRuntimeInfo, keysPath, plugin, "main")) {

            gitRemote.start();

            // Junit attachment with logs file for the plugin build
            System.out.printf(
                    "[[ATTACHMENT|%s]]%n", Plugin.build(plugin).getLogFile().toAbsolutePath());
            System.out.printf("[[ATTACHMENT|%s]]%n", logFile.toAbsolutePath());

            Invoker invoker = buildInvoker();
            InvocationRequest request = buildRequest(
                    "run --recipe %s %s".formatted(recipe, getRunArgs(wmRuntimeInfo, Plugin.build(plugin, targetPath))),
                    logFile);
            InvocationResult result = invoker.execute(request);

            // Assert output
            assertAll(
                    () -> assertEquals(0, result.getExitCode()),
                    () -> assertTrue(Files.readAllLines(logFile).stream()
                            .anyMatch(line -> line.matches("(.*)Dry run mode. Changes were commited on (.*)"))));

            // Check that new file was created
            assertTrue(
                    Files.exists(targetPath.resolve(ArchetypeCommonFile.CODEOWNERS.getPath())),
                    "Code owner file was not created");
        }
    }

    /**
     * Build the invoker
     * @return the invoker
     */
    private Invoker buildInvoker() {
        String mavenHomeEnv = System.getenv("MAVEN_HOME");
        assertNotNull(mavenHomeEnv, "MAVEN_HOME is not set");
        Path mavenHome = Path.of(mavenHomeEnv);
        assertTrue(Files.exists(mavenHome), "MAVEN_HOME does not exist");
        Invoker invoker = new DefaultInvoker();
        invoker.setMavenHome(mavenHome.toFile());
        return invoker;
    }

    /**
     * Build the request
     * @return the request
     */
    private InvocationRequest buildRequest(String args, Path logFile) {
        String javaHomeEnv = System.getenv("JAVA_HOME");
        assertNotNull(javaHomeEnv, "JAVA_HOME is not set");
        Path javaHome = Path.of(javaHomeEnv);
        assertTrue(Files.exists(javaHome), "JAVA_HOME does not exist");

        InvocationRequest request = new DefaultInvocationRequest();
        request.setPomFile(new File("pom-it.xml"));
        request.addArg("verify");

        // Add properties
        Properties properties = new Properties();
        String changeList = System.getProperty("set.changelist");
        if (changeList != null) {
            properties.put("set.changelist", "true");
        }
        properties.put("exec.executable", javaHome.resolve("bin/java").toString());
        properties.put("test.cliArgs", args);
        request.setProperties(properties);

        // Other options
        request.setBatchMode(true);
        request.setNoTransferProgress(true);
        request.setJavaHome(javaHome.toFile());
        request.setOutputHandler(line -> {
            try {
                Files.write(logFile, (line + System.lineSeparator()).getBytes(), StandardOpenOption.APPEND);
            } catch (Exception e) {
                LOG.error("Error writing to stdout", e);
                throw new RuntimeException(e);
            }
            LOG.info(line);
        });
        request.setErrorHandler(line -> {
            try {
                Files.write(logFile, (line + System.lineSeparator()).getBytes(), StandardOpenOption.APPEND);
            } catch (Exception e) {
                LOG.error("Error writing to stderr", e);
                throw new RuntimeException(e);
            }
            LOG.error(line);
        });
        return request;
    }

    /**
     * Generate a Ed25519 private key and save it
     * @param name The name of the key
     * @throws Exception If an error occurs
     */
    private Path generatePrivateKey(String name) throws Exception {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("Ed25519", "BC");
        KeyPair pair = keyPairGenerator.generateKeyPair();
        PrivateKey privateKey = pair.getPrivate();
        File privateKeyFile = keysPath.resolve(name).toFile();
        try (FileWriter fileWriter = new FileWriter(privateKeyFile);
                JcaPEMWriter pemWriter = new JcaPEMWriter(fileWriter)) {
            pemWriter.writeObject(privateKey);
        }
        return privateKeyFile.toPath();
    }

    /**
     * Get the modernizer maven home
     * @return Use version from the target directory
     */
    private Path getModernizerMavenHome() {
        return Path.of("target/apache-maven-3.9.9").toAbsolutePath();
    }

    /**
     * Setup log file for a given test
     * @param test The test name
     * @throws Exception If an error occurs
     */
    private Path setupLogs(String test) throws Exception {
        Path logFile = logFolder.resolve("%s.txt".formatted(test));
        Files.deleteIfExists(logFile);
        Files.createFile(logFile);
        LOG.debug("Created log file: {}", logFile.toAbsolutePath());
        System.out.printf("[[ATTACHMENT|%s]]%n", logFile.toAbsolutePath());
        return logFile;
    }

    /**
     * Get the URL arguments
     * @param wmRuntimeInfo The WireMock runtime info
     * @param plugin The plugin
     * @return the URL arguments
     */
    private String getRunArgs(WireMockRuntimeInfo wmRuntimeInfo, Plugin plugin) {

        String args = "";
        String mavenLocalRepo = System.getProperty("maven.repo.local");
        if (mavenLocalRepo != null) {
            args += "--maven-local-repo %s ".formatted(mavenLocalRepo);
        }
        if (plugin.isLocal()) {
            args += "--plugin-path %s ".formatted(plugin.getLocalRepository().toAbsolutePath());
        } else {
            args += "--plugins %s ".formatted(plugin.getName());
        }
        args +=
                """
        --debug
        --maven-home %s
        --ssh-private-key %s
        --cache-path %s
        --github-api-url %s
        --jenkins-update-center %s
        --jenkins-plugin-info %s
        --plugin-health-score %s
        --jenkins-plugins-stats-installations-url %s
        """
                        .formatted(
                                getModernizerMavenHome(),
                                keysPath.resolve(plugin.getName()),
                                cachePath,
                                wmRuntimeInfo.getHttpBaseUrl() + "/api",
                                wmRuntimeInfo.getHttpBaseUrl() + "/update-center.json",
                                wmRuntimeInfo.getHttpBaseUrl() + "/plugin-versions.json",
                                wmRuntimeInfo.getHttpBaseUrl() + "/scores",
                                wmRuntimeInfo.getHttpBaseUrl() + "/jenkins-stats/svg/202406-plugins.csv")
                        .replaceAll("\\s+", " ");
        LOG.debug("Run args: {}", args);
        return args;
    }
}
