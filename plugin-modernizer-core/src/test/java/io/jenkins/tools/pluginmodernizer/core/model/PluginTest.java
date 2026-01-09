package io.jenkins.tools.pluginmodernizer.core.model;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.jenkins.tools.pluginmodernizer.core.config.Config;
import io.jenkins.tools.pluginmodernizer.core.config.Settings;
import io.jenkins.tools.pluginmodernizer.core.github.GHService;
import io.jenkins.tools.pluginmodernizer.core.impl.MavenInvoker;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

@ExtendWith(MockitoExtension.class)
@Execution(ExecutionMode.CONCURRENT)
public class PluginTest {

    @Mock
    private MavenInvoker mavenInvoker;

    @Mock
    private GHService ghService;

    @Mock
    private Config config;

    @Test
    public void testPluginName() {
        Plugin plugin = Plugin.build("example");
        assertEquals("example", plugin.getName());
        plugin.withName("new-name");
        assertEquals("new-name", plugin.getName());
    }

    @Test
    public void testRepositoryName() {
        Plugin plugin = Plugin.build("example");
        assertNull(plugin.getRepositoryName());
        plugin.withRepositoryName("new-repo");
        assertEquals("new-repo", plugin.getRepositoryName());
    }

    @Test
    public void testDefaultLocalRepository() {
        Plugin plugin = mock(Plugin.class);
        doReturn("example").when(plugin).getName();
        Config config = mock(Config.class);
        doReturn(Settings.DEFAULT_CACHE_PATH).when(config).getCachePath();
        doReturn(config).when(plugin).getConfig();
        assertEquals(
                Settings.getPluginsDirectory(plugin).resolve("sources").toString(),
                Settings.DEFAULT_CACHE_PATH
                        .resolve("example")
                        .resolve("sources")
                        .toString());
    }

    @Test
    public void testCustomLocalRepository() {
        Plugin plugin = mock(Plugin.class);
        doReturn("example").when(plugin).getName();
        Config config = mock(Config.class);
        doReturn(Path.of("my-cache")).when(config).getCachePath();
        doReturn(config).when(plugin).getConfig();
        assertEquals(
                Settings.getPluginsDirectory(plugin).resolve("sources").toString(),
                Path.of("my-cache").resolve("example").resolve("sources").toString());
    }

    @Test
    public void testGetGitHubRepository() {
        Plugin plugin = Plugin.build("example");
        plugin.withRepositoryName("repo-name");
        assertEquals(
                "https://github.com/foobar/repo-name.git",
                plugin.getGitRepositoryURI("foobar").toString());
    }

    @Test
    public void testGetDiffStats() {
        Plugin plugin = Plugin.build("example");
        plugin.withConfig(config);
        // dry-run true
        doReturn(true).when(config).isDryRun();
        plugin.getDiffStats(ghService, config.isDryRun());
        verify(ghService).getDiffStats(plugin, true);
        // dry-run false
        doReturn(false).when(config).isDryRun();
        plugin.getDiffStats(ghService, config.isDryRun());
        verify(ghService).getDiffStats(plugin, false);
        verifyNoMoreInteractions(ghService);
    }

    @Test
    public void testHasCommits() {
        Plugin plugin = Plugin.build("example");
        assertFalse(plugin.hasCommits());
        plugin.withCommits();
        assertTrue(plugin.hasCommits());
        plugin.withoutCommits();
        assertFalse(plugin.hasCommits());
    }

    @Test
    public void testHasMetadataCommits() {
        Plugin plugin = Plugin.build("example");
        assertFalse(plugin.hasMetadataCommits());
        plugin.withMetadataCommits();
        assertTrue(plugin.hasMetadataCommits());
        plugin.withoutMetadataCommits();
        assertFalse(plugin.hasMetadataCommits());
    }

    @Test
    public void testClean() {
        Plugin plugin = Plugin.build("example");
        plugin.clean(mavenInvoker);
        verify(mavenInvoker).invokeGoal(plugin, "clean");
        verifyNoMoreInteractions(mavenInvoker);
    }

    @Test
    public void testCompile() {
        Plugin plugin = Plugin.build("example");
        plugin.withConfig(config);
        doReturn(false).when(config).isFetchMetadataOnly();
        plugin.withJDK(JDK.JAVA_21);
        plugin.compile(mavenInvoker);
        verify(mavenInvoker).invokeGoal(plugin, "compile", "-Dhpi.validate.skip=true", "-Dmaven.antrun.skip=true");
        verifyNoMoreInteractions(mavenInvoker);
    }

    @Test
    public void shouldSkipCompileInFetchMetadata() {
        Plugin plugin = Plugin.build("example");
        plugin.withConfig(config);
        doReturn(true).when(config).isFetchMetadataOnly();
        plugin.withJDK(JDK.JAVA_21);
        plugin.compile(mavenInvoker);
        verify(mavenInvoker, times(0)).invokeGoal(plugin, "compile");
        verifyNoMoreInteractions(mavenInvoker);
    }

    @Test
    public void testVerify() {
        Plugin plugin = Plugin.build("example");
        plugin.withConfig(config);
        doReturn(false).when(config).isFetchMetadataOnly();
        plugin.withJDK(JDK.JAVA_21);
        plugin.verify(mavenInvoker);
        verify(mavenInvoker).invokeGoal(plugin, "verify");
        verifyNoMoreInteractions(mavenInvoker);
    }

    @Test
    public void shouldNotVerifyInFetchMetadataMode() {
        Plugin plugin = Plugin.build("example");
        plugin.withConfig(config);
        doReturn(true).when(config).isFetchMetadataOnly();
        plugin.withJDK(JDK.JAVA_21);
        plugin.verify(mavenInvoker);
        verify(mavenInvoker, times(0)).invokeGoal(plugin, "verify");
        verifyNoMoreInteractions(mavenInvoker);
    }

    @Test
    public void testRewrite() {
        Plugin plugin = Plugin.build("example");
        plugin.withConfig(config);
        doReturn(false).when(config).isFetchMetadataOnly();
        plugin.runOpenRewrite(mavenInvoker);
        verify(mavenInvoker).invokeRewrite(plugin);
        verifyNoMoreInteractions(mavenInvoker);
    }

    @Test
    public void shouldSkipRewriteInFetchMetadataMode() {
        Plugin plugin = Plugin.build("example");
        plugin.withConfig(config);
        doReturn(true).when(config).isFetchMetadataOnly();
        plugin.runOpenRewrite(mavenInvoker);
        verify(mavenInvoker, times(0)).invokeRewrite(plugin);
        verifyNoMoreInteractions(mavenInvoker);
    }

    @Test
    public void testFork() {
        Plugin plugin = Plugin.build("example");
        plugin.withConfig(config);
        doReturn(false).when(config).isFetchMetadataOnly();
        plugin.fork(ghService);
        verify(ghService).fork(plugin, RepoType.PLUGIN);
        verifyNoMoreInteractions(ghService);
    }

    @Test
    public void testForkMetadata() {
        Plugin plugin = Plugin.build("example");
        plugin.withConfig(config);
        doReturn(false).when(config).isFetchMetadataOnly();
        plugin.forkMetadata(ghService);
        verify(ghService).fork(plugin, RepoType.METADATA);
        verifyNoMoreInteractions(ghService);
    }

    @Test
    public void shouldNotForkInFetchMetadataMode() {
        Plugin plugin = Plugin.build("example");
        plugin.withConfig(config);
        doReturn(true).when(config).isFetchMetadataOnly();
        plugin.fork(ghService);
        verify(ghService, times(0)).fork(plugin, RepoType.PLUGIN);
    }

    @Test
    public void testSync() {
        Plugin plugin = Plugin.build("example");
        plugin.withConfig(config);
        doReturn(false).when(config).isFetchMetadataOnly();
        plugin.sync(ghService);
        verify(ghService).sync(plugin, RepoType.PLUGIN);
        verifyNoMoreInteractions(ghService);
    }

    @Test
    public void testSyncMetadata() {
        Plugin plugin = Plugin.build("example");
        plugin.withConfig(config);
        doReturn(false).when(config).isFetchMetadataOnly();
        plugin.syncMetadata(ghService);
        verify(ghService).sync(plugin, RepoType.METADATA);
        verifyNoMoreInteractions(ghService);
    }

    @Test
    public void shouldNotSyncInFetchMetadataMode() {
        Plugin plugin = Plugin.build("example");
        plugin.withConfig(config);
        doReturn(true).when(config).isFetchMetadataOnly();
        plugin.sync(ghService);
        verify(ghService, times(0)).sync(plugin, RepoType.PLUGIN);
    }

    @Test
    public void testIsFork() {
        Plugin plugin = Plugin.build("example");
        plugin.isForked(ghService);
        verify(ghService).isForked(plugin, RepoType.PLUGIN);
        verifyNoMoreInteractions(ghService);
    }

    @Test
    public void testIsMetadataFork() {
        Plugin plugin = Plugin.build("example");
        plugin.isForkedMetadata(ghService);
        verify(ghService).isForked(plugin, RepoType.METADATA);
        verifyNoMoreInteractions(ghService);
    }

    @Test
    public void testDeleteFork() {
        Plugin plugin = Plugin.build("example");
        plugin.deleteFork(ghService);
        verify(ghService).deleteFork(plugin);
        verifyNoMoreInteractions(ghService);
    }

    @Test
    public void testIsArchived() {
        Plugin plugin = Plugin.build("example");
        plugin.isArchived(ghService);
        verify(ghService).isArchived(plugin);
        verifyNoMoreInteractions(ghService);
    }

    @Test
    public void testCheckoutBranch() {
        Plugin plugin = Plugin.build("example");
        plugin.checkoutBranch(ghService);
        verify(ghService).checkoutBranch(plugin, RepoType.PLUGIN);
        verifyNoMoreInteractions(ghService);
    }

    @Test
    public void testMetadataCheckoutBranch() {
        Plugin plugin = Plugin.build("example");
        plugin.checkoutMetadataBranch(ghService);
        verify(ghService).checkoutBranch(plugin, RepoType.METADATA);
        verifyNoMoreInteractions(ghService);
    }

    @Test
    public void testCommit() {
        Plugin plugin = Plugin.build("example");
        plugin.commit(ghService);
        verify(ghService).commitChanges(plugin, RepoType.PLUGIN);
        verifyNoMoreInteractions(ghService);
    }

    @Test
    public void testMetadataCommit() {
        Plugin plugin = Plugin.build("example");
        plugin.commitMetadata(ghService);
        verify(ghService).commitChanges(plugin, RepoType.METADATA);
        verifyNoMoreInteractions(ghService);
    }

    @Test
    public void testPush() {
        Plugin plugin = Plugin.build("example");
        plugin.push(ghService);
        verify(ghService).pushChanges(plugin, RepoType.PLUGIN);
        verifyNoMoreInteractions(ghService);
    }

    @Test
    public void testMetadataPush() {
        Plugin plugin = Plugin.build("example");
        plugin.pushMetadata(ghService);
        verify(ghService).pushChanges(plugin, RepoType.METADATA);
        verifyNoMoreInteractions(ghService);
    }

    @Test
    public void testOpenPullRequest() {
        Plugin plugin = Plugin.build("example");
        plugin.openPullRequest(ghService);
        verify(ghService).openPullRequest(plugin, RepoType.PLUGIN);
        verifyNoMoreInteractions(ghService);
    }

    @Test
    public void testOpenMetadataPullRequest() {
        Plugin plugin = Plugin.build("example");
        plugin.openMetadataPullRequest(ghService);
        verify(ghService).openPullRequest(plugin, RepoType.METADATA);
        verifyNoMoreInteractions(ghService);
    }

    @Test
    public void testFetch() {
        Plugin plugin = Plugin.build("example");
        plugin.fetch(ghService);
        verify(ghService).fetch(plugin, RepoType.PLUGIN);
        verifyNoMoreInteractions(ghService);
    }

    @Test
    public void testFetchMetadata() {
        Plugin plugin = Plugin.build("example");
        plugin.fetchMetadata(ghService);
        verify(ghService).fetch(plugin, RepoType.METADATA);
        verifyNoMoreInteractions(ghService);
    }

    @Test
    public void testGetRemoteRepository() {
        Plugin plugin = Plugin.build("example");
        plugin.withRepositoryName("repo-name");
        plugin.getRemoteRepository(ghService);
        verify(ghService).getRepository(plugin, RepoType.PLUGIN);
    }

    @Test
    public void testGetRemoteMetadataRepository() {
        Plugin plugin = Plugin.build("example");
        plugin.getRemoteMetadataRepository(ghService);
        verify(ghService).getRepository(plugin, RepoType.METADATA);
    }

    @Test
    public void testGetRemoteForkRepository() {
        Plugin plugin = Plugin.build("example");
        plugin.withRepositoryName("repo-name");
        plugin.getRemoteForkRepository(ghService);
        verify(ghService).getRepositoryFork(plugin, RepoType.PLUGIN);
    }

    @Test
    public void testGetRemoteMetadataForkRepository() {
        Plugin plugin = Plugin.build("example");
        plugin.getRemoteMetadataForkRepository(ghService);
        verify(ghService).getRepositoryFork(plugin, RepoType.METADATA);
    }

    @Test
    public void testHasErrors() {
        Plugin plugin = Plugin.build("example").withConfig(mock(Config.class));
        assertFalse(plugin.hasErrors());
        plugin.addError("error", new Exception("error"));
        assertTrue(plugin.hasErrors());
    }

    @Test
    public void testToString() {
        Plugin plugin = Plugin.build("example");
        assertEquals("example", plugin.toString());
    }

    @Test
    public void testGetMarker() {
        Plugin plugin = Plugin.build("example");
        Marker expectedMarker = MarkerFactory.getMarker("example");
        Marker actualMarker = plugin.getMarker();
        assertEquals(expectedMarker, actualMarker);
    }

    @Test
    public void testModernizationMetadataInitiallyNull() {
        Plugin plugin = Plugin.build("example");
        plugin.withConfig(config);
        assertNull(plugin.getModernizationMetadata());
    }
}
