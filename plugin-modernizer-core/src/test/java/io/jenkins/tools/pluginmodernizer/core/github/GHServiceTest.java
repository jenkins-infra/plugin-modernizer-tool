package io.jenkins.tools.pluginmodernizer.core.github;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import com.google.inject.Guice;
import io.jenkins.tools.pluginmodernizer.core.GuiceModule;
import io.jenkins.tools.pluginmodernizer.core.config.Config;
import io.jenkins.tools.pluginmodernizer.core.config.Settings;
import io.jenkins.tools.pluginmodernizer.core.model.Plugin;
import io.jenkins.tools.pluginmodernizer.core.model.PluginProcessingException;
import io.jenkins.tools.pluginmodernizer.core.model.Recipe;
import io.jenkins.tools.pluginmodernizer.core.model.RepoType;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.platform.commons.util.ReflectionUtils;
import org.kohsuke.github.GHCommitPointer;
import org.kohsuke.github.GHIssueState;
import org.kohsuke.github.GHMyself;
import org.kohsuke.github.GHOrganization;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHPullRequestQueryBuilder;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GHRepositoryForkBuilder;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.PagedIterable;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith({MockitoExtension.class})
@Execution(ExecutionMode.CONCURRENT)
public class GHServiceTest {

    @Mock
    private Config config;

    @Mock
    private Plugin plugin;

    @Mock
    private GitHub github;

    @TempDir
    private Path pluginDir;

    @TempDir
    private Path sshDir;

    /**
     * Tested instance
     */
    private GHService service;

    @BeforeEach
    public void setup() throws Exception {

        // Create service
        if (service == null) {
            service = Guice.createInjector(new GuiceModule(config)).getInstance(GHService.class);
            // Set github mock
            Field field = ReflectionUtils.findFields(
                            GHService.class,
                            f -> f.getName().equals("github"),
                            ReflectionUtils.HierarchyTraversalMode.TOP_DOWN)
                    .get(0);
            field.setAccessible(true);
            field.set(service, github);
        }
    }

    @Test
    public void shouldGetRepository() throws Exception {

        // Mock
        GHRepository mock = Mockito.mock(GHRepository.class);
        doReturn("fake-repo").when(plugin).getRepositoryName();
        doReturn(mock).when(github).getRepository(eq("jenkinsci/fake-repo"));

        // Test
        GHRepository repository = service.getRepository(plugin, RepoType.PLUGIN);

        // Verify
        assertSame(mock, repository);
    }

    @Test
    public void shouldGetMetadataRepository() throws Exception {

        // Mock
        GHRepository mock = Mockito.mock(GHRepository.class);
        doReturn(mock).when(github).getRepository(eq("jenkins-infra/metadata-plugin-modernizer"));

        // Test
        GHRepository repository = service.getRepository(plugin, RepoType.METADATA);

        // Verify
        assertSame(mock, repository);
    }

    @Test
    public void shouldFailToGetRepository() throws Exception {

        // Mock
        doReturn("fake-repo").when(plugin).getRepositoryName();
        doThrow(new IOException()).when(github).getRepository(eq("jenkinsci/fake-repo"));

        // Test
        assertThrows(PluginProcessingException.class, () -> {
            service.getRepository(plugin, RepoType.PLUGIN);
        });
    }

    @Test
    public void shouldFailToGetMetadataRepository() throws Exception {

        // Mock
        doThrow(new IOException()).when(github).getRepository(eq("jenkins-infra/metadata-plugin-modernizer"));

        // Test
        assertThrows(PluginProcessingException.class, () -> {
            service.getRepository(plugin, RepoType.METADATA);
        });
    }

    @Test
    public void shouldGetRepositoryFork() throws Exception {

        // Mock
        doReturn("fake-owner").when(config).getGithubOwner();
        GHRepository mock = Mockito.mock(GHRepository.class);
        doReturn("fake-repo").when(plugin).getRepositoryName();
        doReturn(mock).when(github).getRepository(eq("fake-owner/fake-repo"));

        // Test
        GHRepository repository = service.getRepositoryFork(plugin, RepoType.PLUGIN);

        // Verify
        assertSame(mock, repository);
    }

    public void shouldGetMetadataRepositoryFork() throws Exception {

        // Mock
        doReturn("fake-owner").when(config).getGithubOwner();
        GHRepository mock = Mockito.mock(GHRepository.class);
        doReturn(mock).when(github).getRepository(eq("fake-owner/metadata-plugin-modernizer"));

        // Test
        GHRepository repository = service.getRepositoryFork(plugin, RepoType.METADATA);

        // Verify
        assertSame(mock, repository);
    }

    @Test
    public void shouldFailToGetForkRepository() throws Exception {

        // Mock
        doReturn("fake-owner").when(config).getGithubOwner();
        doReturn("fake-repo").when(plugin).getRepositoryName();
        doThrow(new IOException()).when(github).getRepository(eq("fake-owner/fake-repo"));

        // Test
        assertThrows(PluginProcessingException.class, () -> {
            service.getRepositoryFork(plugin, RepoType.PLUGIN);
        });
    }

    @Test
    public void shouldFailToGetForkMetadataRepository() throws Exception {

        // Mock
        doReturn("fake-owner").when(config).getGithubOwner();
        doThrow(new IOException()).when(github).getRepository(eq("fake-owner/metadata-plugin-modernizer"));

        // Test
        assertThrows(PluginProcessingException.class, () -> {
            service.getRepositoryFork(plugin, RepoType.METADATA);
        });
    }

    @Test
    public void isArchivedTest() throws Exception {
        // Mock
        GHRepository repository = Mockito.mock(GHRepository.class);

        doReturn(true).when(repository).isArchived();
        doReturn(repository).when(plugin).getRemoteRepository(eq(service));

        // Test and verify
        assertTrue(service.isArchived(plugin));
        doReturn(false).when(repository).isArchived();
        assertFalse(service.isArchived(plugin));
    }

    @Test
    public void shouldFailToGetForkRepositoryInDryRunMode() throws Exception {

        // Mock
        doReturn(true).when(config).isDryRun();

        // Test
        assertThrows(PluginProcessingException.class, () -> {
            service.getRepositoryFork(plugin, RepoType.PLUGIN);
        });
    }

    @Test
    public void shouldFailToGetForkMetadataRepositoryInDryRunMode() throws Exception {

        // Mock
        doReturn(true).when(config).isDryRun();

        // Test
        assertThrows(PluginProcessingException.class, () -> {
            service.getRepositoryFork(plugin, RepoType.METADATA);
        });
    }

    @Test
    public void isForkedTest() throws Exception {

        // Mock
        doReturn("fake-owner").when(config).getGithubOwner();
        doReturn(null).when(config).getGithubAppId();
        GHRepository fork = Mockito.mock(GHRepository.class);
        GHMyself myself = Mockito.mock(GHMyself.class);

        doReturn("fake-repo").when(plugin).getRepositoryName();
        doReturn(myself).when(github).getMyself();
        doReturn(fork).when(myself).getRepository(eq("fake-repo"));

        // Test and verify
        assertTrue(service.isForked(plugin, RepoType.PLUGIN));
    }

    @Test
    public void isForkedMetadataTest() throws Exception {

        // Mock
        doReturn("fake-owner").when(config).getGithubOwner();
        doReturn(null).when(config).getGithubAppId();
        GHRepository fork = Mockito.mock(GHRepository.class);
        GHMyself myself = Mockito.mock(GHMyself.class);

        doReturn(myself).when(github).getMyself();
        doReturn(fork).when(myself).getRepository(eq("metadata-plugin-modernizer"));

        // Test and verify
        assertTrue(service.isForked(plugin, RepoType.METADATA));
    }

    @Test
    public void isNotForkedTest() throws Exception {

        // Mock
        doReturn("fake-owner").when(config).getGithubOwner();
        doReturn(null).when(config).getGithubAppId();
        GHMyself myself = Mockito.mock(GHMyself.class);

        doReturn("fake-repo").when(plugin).getRepositoryName();
        doReturn(myself).when(github).getMyself();
        doReturn(null).when(myself).getRepository(eq("fake-repo"));

        // Test and verify
        assertFalse(service.isForked(plugin, RepoType.PLUGIN));
    }

    @Test
    public void isNotForkedMetadataTest() throws Exception {

        // Mock
        doReturn("fake-owner").when(config).getGithubOwner();
        doReturn(null).when(config).getGithubAppId();
        GHMyself myself = Mockito.mock(GHMyself.class);

        doReturn(myself).when(github).getMyself();
        doReturn(null).when(myself).getRepository(eq("metadata-plugin-modernizer"));

        // Test and verify
        assertFalse(service.isForked(plugin, RepoType.METADATA));
    }

    @Test
    public void isForkedToOrganisation() throws Exception {

        // Mock
        doReturn("fake-owner").when(config).getGithubOwner();
        GHRepository fork = Mockito.mock(GHRepository.class);
        GHOrganization org = Mockito.mock(GHOrganization.class);

        doReturn("fake-repo").when(plugin).getRepositoryName();
        doReturn(org).when(github).getOrganization(eq("fake-owner"));
        doReturn(fork).when(org).getRepository(eq("fake-repo"));

        // Test and verify
        assertTrue(service.isForked(plugin, RepoType.PLUGIN));
    }

    @Test
    public void isForkedMetadataToOrganisation() throws Exception {

        // Mock
        doReturn("fake-owner").when(config).getGithubOwner();
        GHRepository fork = Mockito.mock(GHRepository.class);
        GHOrganization org = Mockito.mock(GHOrganization.class);

        doReturn(org).when(github).getOrganization(eq("fake-owner"));
        doReturn(fork).when(org).getRepository(eq("metadata-plugin-modernizer"));

        // Test and verify
        assertTrue(service.isForked(plugin, RepoType.METADATA));
    }

    @Test
    public void shouldNotForkInDryRunMode() throws Exception {

        // Mock
        doReturn(true).when(config).isDryRun();

        // Test
        service.fork(plugin, RepoType.PLUGIN);
        verifyNoInteractions(github);
    }

    @Test
    public void shouldNotForkMetadataInDryRunMode() throws Exception {

        // Mock
        doReturn(true).when(config).isDryRun();

        // Test
        service.fork(plugin, RepoType.METADATA);
        verifyNoInteractions(github);
    }

    @Test
    public void shouldNotForkArchivedRepos() throws Exception {

        // Mock
        doReturn(true).when(plugin).isArchived(eq(service));

        // Test
        service.fork(plugin, RepoType.PLUGIN);
        verifyNoInteractions(github);
    }

    @Test
    public void shouldForkRepoToMyself() throws Exception {

        GHRepository repository = Mockito.mock(GHRepository.class);
        GHRepository fork = Mockito.mock(GHRepository.class);
        GHMyself myself = Mockito.mock(GHMyself.class);
        GHRepositoryForkBuilder builder = Mockito.mock(GHRepositoryForkBuilder.class);

        // Mock
        doReturn("fake-owner").when(config).getGithubOwner();
        doReturn(null).when(config).getGithubAppId();
        doReturn("fake-repo").when(repository).getName();
        doReturn(Mockito.mock(URL.class)).when(fork).getHtmlUrl();
        doReturn(repository).when(plugin).getRemoteRepository(eq(service));
        doReturn(myself).when(github).getMyself();
        doReturn(builder).when(repository).createFork();
        doReturn(fork).when(builder).create();

        // Not yet forked
        doReturn(null).when(myself).getRepository(eq("fake-repo"));

        // Directory doesn't exists
        doReturn(Path.of("not-existing-dir")).when(plugin).getLocalRepository();

        // Test
        service.fork(plugin, RepoType.PLUGIN);

        // Verify
        verify(repository, times(1)).createFork();
    }

    @Test
    public void shouldForkMetadataRepoToMyself() throws Exception {

        GHRepository repository = Mockito.mock(GHRepository.class);
        GHRepository fork = Mockito.mock(GHRepository.class);
        GHMyself myself = Mockito.mock(GHMyself.class);
        GHRepositoryForkBuilder builder = Mockito.mock(GHRepositoryForkBuilder.class);

        // Mock
        doReturn("fake-owner").when(config).getGithubOwner();
        doReturn(null).when(config).getGithubAppId();
        doReturn("metadata-plugin-modernizer").when(repository).getName();
        doReturn(Mockito.mock(URL.class)).when(fork).getHtmlUrl();
        doReturn(repository).when(plugin).getRemoteMetadataRepository(eq(service));
        doReturn(myself).when(github).getMyself();
        doReturn(builder).when(repository).createFork();
        doReturn(fork).when(builder).create();

        // Directory doesn't exists
        doReturn(Path.of("not-existing-dir")).when(plugin).getLocalMetadataRepository();

        // Not yet forked
        doReturn(null).when(myself).getRepository(eq("metadata-plugin-modernizer"));

        // Test
        service.fork(plugin, RepoType.METADATA);

        // Verify
        verify(repository, times(1)).createFork();
    }

    @Test
    public void shouldReturnForkWhenAlreadyForkedToMyself() throws Exception {

        GHRepository repository = Mockito.mock(GHRepository.class);
        GHRepository fork = Mockito.mock(GHRepository.class);
        GHMyself myself = Mockito.mock(GHMyself.class);

        // Mock
        doReturn("fake-owner").when(config).getGithubOwner();
        doReturn(null).when(config).getGithubAppId();
        doReturn(repository).when(fork).getParent();
        doReturn("fake-repo").when(repository).getName();
        doReturn(Mockito.mock(URL.class)).when(fork).getHtmlUrl();
        doReturn(repository).when(plugin).getRemoteRepository(eq(service));
        doReturn(myself).when(github).getMyself();

        // Already forked
        doReturn(fork).when(myself).getRepository(eq("fake-repo"));

        // Directory doesn't exists
        doReturn(Path.of("not-existing-dir")).when(plugin).getLocalRepository();

        // Test
        service.fork(plugin, RepoType.PLUGIN);

        // Verify
        verify(repository, times(0)).createFork();
        verify(myself, times(2)).getRepository(eq("fake-repo"));
    }

    @Test
    public void shouldReturnMetadataForkWhenAlreadyForkedToMyself() throws Exception {

        GHRepository repository = Mockito.mock(GHRepository.class);
        GHRepository fork = Mockito.mock(GHRepository.class);
        GHMyself myself = Mockito.mock(GHMyself.class);

        // Mock
        doReturn("fake-owner").when(config).getGithubOwner();
        doReturn(null).when(config).getGithubAppId();
        doReturn(repository).when(fork).getParent();
        doReturn("metadata-plugin-modernizer").when(repository).getName();
        doReturn(Mockito.mock(URL.class)).when(fork).getHtmlUrl();
        doReturn(repository).when(plugin).getRemoteMetadataRepository(eq(service));
        doReturn(myself).when(github).getMyself();

        // Directory doesn't exists
        doReturn(Path.of("not-existing-dir")).when(plugin).getLocalMetadataRepository();

        // Already forked
        doReturn(fork).when(myself).getRepository(eq("metadata-plugin-modernizer"));

        // Test
        service.fork(plugin, RepoType.METADATA);

        // Verify
        verify(repository, times(0)).createFork();
        verify(myself, times(2)).getRepository(eq("metadata-plugin-modernizer"));
    }

    @Test
    public void shouldFailToGetForkWhenAlreadyForkedFromOtherSource() throws Exception {

        GHRepository repository = Mockito.mock(GHRepository.class);
        GHRepository other = Mockito.mock(GHRepository.class);
        GHRepository fork = Mockito.mock(GHRepository.class);
        GHMyself myself = Mockito.mock(GHMyself.class);

        // Mock
        doReturn("fake-owner").when(config).getGithubOwner();
        doReturn(null).when(config).getGithubAppId();
        doReturn(other).when(fork).getParent();
        doReturn("fake-repo").when(repository).getName();
        doReturn(repository).when(plugin).getRemoteRepository(eq(service));
        doReturn(myself).when(github).getMyself();

        // Already forked
        doReturn(fork).when(myself).getRepository(eq("fake-repo"));

        assertThrows(PluginProcessingException.class, () -> {
            service.fork(plugin, RepoType.PLUGIN);
        });
    }

    @Test
    public void shouldFailToGetMetadataForkWhenAlreadyForkedFromOtherSource() throws Exception {

        GHRepository repository = Mockito.mock(GHRepository.class);
        GHRepository other = Mockito.mock(GHRepository.class);
        GHRepository fork = Mockito.mock(GHRepository.class);
        GHMyself myself = Mockito.mock(GHMyself.class);

        // Mock
        doReturn("fake-owner").when(config).getGithubOwner();
        doReturn(null).when(config).getGithubAppId();
        doReturn(other).when(fork).getParent();
        doReturn("metadata-plugin-modernizer").when(repository).getName();
        doReturn(repository).when(plugin).getRemoteMetadataRepository(eq(service));
        doReturn(myself).when(github).getMyself();

        // Already forked
        doReturn(fork).when(myself).getRepository(eq("metadata-plugin-modernizer"));

        assertThrows(PluginProcessingException.class, () -> {
            service.fork(plugin, RepoType.METADATA);
        });
    }

    @Test
    public void shouldForkRepoToOrganisation() throws Exception {

        GHRepository repository = Mockito.mock(GHRepository.class);
        GHRepository fork = Mockito.mock(GHRepository.class);
        GHOrganization org = Mockito.mock(GHOrganization.class);
        GHRepositoryForkBuilder builder = Mockito.mock(GHRepositoryForkBuilder.class);

        // Mock
        doReturn("fake-owner").when(config).getGithubOwner();
        doReturn("fake-repo").when(repository).getName();
        doReturn(Mockito.mock(URL.class)).when(fork).getHtmlUrl();
        doReturn(repository).when(plugin).getRemoteRepository(eq(service));
        doReturn(org).when(github).getOrganization("fake-owner");
        doReturn(builder).when(repository).createFork();
        doReturn(builder).when(builder).organization(eq(org));
        doReturn(fork).when(builder).create();

        // Not yet forked
        doReturn(null).when(org).getRepository(eq("fake-repo"));

        // Directory doesn't exists
        doReturn(Path.of("not-existing-dir")).when(plugin).getLocalRepository();

        // Test
        service.fork(plugin, RepoType.PLUGIN);

        // Verify
        verify(repository, times(1)).createFork();
    }

    @Test
    public void shouldForkMetadataRepoToOrganisation() throws Exception {

        GHRepository repository = Mockito.mock(GHRepository.class);
        GHRepository fork = Mockito.mock(GHRepository.class);
        GHOrganization org = Mockito.mock(GHOrganization.class);
        GHRepositoryForkBuilder builder = Mockito.mock(GHRepositoryForkBuilder.class);

        // Mock
        doReturn("fake-owner").when(config).getGithubOwner();
        doReturn("metadata-plugin-modernizer").when(repository).getName();
        doReturn(Mockito.mock(URL.class)).when(fork).getHtmlUrl();
        doReturn(repository).when(plugin).getRemoteMetadataRepository(eq(service));
        doReturn(org).when(github).getOrganization("fake-owner");
        doReturn(builder).when(repository).createFork();
        doReturn(builder).when(builder).organization(eq(org));
        doReturn(fork).when(builder).create();

        // Directory doesn't exists
        doReturn(Path.of("not-existing-dir")).when(plugin).getLocalMetadataRepository();

        // Not yet forked
        doReturn(null).when(org).getRepository(eq("metadata-plugin-modernizer"));

        // Test
        service.fork(plugin, RepoType.METADATA);

        // Verify
        verify(repository, times(1)).createFork();
    }

    @Test
    public void shouldReturnForkWhenAlreadyForkedToOrganisation() throws Exception {

        GHRepository repository = Mockito.mock(GHRepository.class);
        GHRepository fork = Mockito.mock(GHRepository.class);
        GHOrganization org = Mockito.mock(GHOrganization.class);

        // Mock
        doReturn("fake-owner").when(config).getGithubOwner();
        doReturn(repository).when(fork).getParent();
        doReturn("fake-repo").when(repository).getName();
        doReturn(Mockito.mock(URL.class)).when(fork).getHtmlUrl();
        doReturn(repository).when(plugin).getRemoteRepository(eq(service));
        doReturn(org).when(github).getOrganization("fake-owner");

        // Already forked to org
        doReturn(fork).when(org).getRepository(eq("fake-repo"));

        // Directory doesn't exists
        doReturn(Path.of("not-existing-dir")).when(plugin).getLocalRepository();

        // Test
        service.fork(plugin, RepoType.PLUGIN);

        // Verify
        verify(repository, times(0)).createFork();
        verify(org, times(2)).getRepository(eq("fake-repo"));
    }

    @Test
    public void shouldReturnMetadataForkWhenAlreadyForkedToOrganisation() throws Exception {

        GHRepository repository = Mockito.mock(GHRepository.class);
        GHRepository fork = Mockito.mock(GHRepository.class);
        GHOrganization org = Mockito.mock(GHOrganization.class);

        // Mock
        doReturn("fake-owner").when(config).getGithubOwner();
        doReturn(repository).when(fork).getParent();
        doReturn("metadata-plugin-modernizer").when(repository).getName();
        doReturn(Mockito.mock(URL.class)).when(fork).getHtmlUrl();
        doReturn(repository).when(plugin).getRemoteMetadataRepository(eq(service));
        doReturn(org).when(github).getOrganization("fake-owner");

        // Directory doesn't exists
        doReturn(Path.of("not-existing-dir")).when(plugin).getLocalMetadataRepository();

        // Already forked to org
        doReturn(fork).when(org).getRepository(eq("metadata-plugin-modernizer"));

        // Test
        service.fork(plugin, RepoType.METADATA);

        // Verify
        verify(repository, times(0)).createFork();
        verify(org, times(2)).getRepository(eq("metadata-plugin-modernizer"));
    }

    @Test
    public void shouldFailtToGetForkWhenAlreadyForkedToOrganisationOfOtherSource() throws Exception {

        GHRepository repository = Mockito.mock(GHRepository.class);
        GHRepository other = Mockito.mock(GHRepository.class);
        GHRepository fork = Mockito.mock(GHRepository.class);
        GHOrganization org = Mockito.mock(GHOrganization.class);

        // Mock
        doReturn("fake-owner").when(config).getGithubOwner();
        doReturn(other).when(fork).getParent();
        doReturn("fake-repo").when(repository).getName();
        doReturn(repository).when(plugin).getRemoteRepository(eq(service));
        doReturn(org).when(github).getOrganization("fake-owner");

        // Already forked to org
        doReturn(fork).when(org).getRepository(eq("fake-repo"));

        // Test
        assertThrows(PluginProcessingException.class, () -> {
            service.fork(plugin, RepoType.PLUGIN);
        });
    }

    @Test
    public void shouldFailtToGetMetadataForkWhenAlreadyForkedToOrganisationOfOtherSource() throws Exception {

        GHRepository repository = Mockito.mock(GHRepository.class);
        GHRepository other = Mockito.mock(GHRepository.class);
        GHRepository fork = Mockito.mock(GHRepository.class);
        GHOrganization org = Mockito.mock(GHOrganization.class);

        // Mock
        doReturn("fake-owner").when(config).getGithubOwner();
        doReturn(other).when(fork).getParent();
        doReturn("metadata-plugin-modernizer").when(repository).getName();
        doReturn(repository).when(plugin).getRemoteMetadataRepository(eq(service));
        doReturn(org).when(github).getOrganization("fake-owner");

        // Already forked to org
        doReturn(fork).when(org).getRepository(eq("metadata-plugin-modernizer"));

        // Test
        assertThrows(PluginProcessingException.class, () -> {
            service.fork(plugin, RepoType.METADATA);
        });
    }

    @Test
    public void shouldNotDeleteForkIsDryRunMode() throws Exception {

        // Mock
        GHRepository fork = Mockito.mock(GHRepository.class);

        doReturn(true).when(config).isDryRun();

        // Test
        service.deleteFork(plugin);
        verifyNoInteractions(github);
        verify(fork, never()).delete();
    }

    @Test
    public void shouldNotAttemptToDeleteNonFork() throws Exception {

        // Mock
        doReturn("fake-owner").when(config).getGithubOwner();
        doReturn(null).when(config).getGithubAppId();
        GHRepository fork = Mockito.mock(GHRepository.class);
        GHMyself myself = Mockito.mock(GHMyself.class);

        doReturn("fake-repo").when(plugin).getRepositoryName();
        doReturn(myself).when(github).getMyself();
        doReturn(null).when(myself).getRepository(eq("fake-repo"));

        // Test
        service.deleteFork(plugin);
        verify(fork, never()).delete();
    }

    @Test
    public void shouldNotDeleteForkWithOpenPullRequestSource() throws Exception {

        // Mock
        doReturn("fake-owner").when(config).getGithubOwner();
        doReturn(null).when(config).getGithubAppId();
        GHRepository repository = Mockito.mock(GHRepository.class);
        GHRepository fork = Mockito.mock(GHRepository.class);
        GHMyself myself = Mockito.mock(GHMyself.class);
        GHPullRequest pr = Mockito.mock(GHPullRequest.class);
        GHCommitPointer head = Mockito.mock(GHCommitPointer.class);
        GHPullRequestQueryBuilder prQuery = Mockito.mock(GHPullRequestQueryBuilder.class);
        PagedIterable<?> prQueryList = Mockito.mock(PagedIterable.class);

        doReturn("fake-owner/fake-repo").when(fork).getFullName();
        doReturn("fake-repo").when(plugin).getRepositoryName();
        doReturn(myself).when(github).getMyself();
        doReturn(fork).when(myself).getRepository(eq("fake-repo"));
        doReturn(repository).when(plugin).getRemoteRepository(eq(service));
        doReturn(fork).when(plugin).getRemoteForkRepository(eq(service));

        // Return at least one PR open
        doReturn(head).when(pr).getHead();
        doReturn(fork).when(head).getRepository();
        doReturn(prQuery).when(repository).queryPullRequests();
        doReturn(prQuery).when(prQuery).state(eq(GHIssueState.OPEN));
        doReturn(prQueryList).when(prQuery).list();
        doReturn(List.of(pr)).when(prQueryList).toList();

        // Test
        service.deleteFork(plugin);
        verify(fork, never()).delete();
    }

    @Test
    public void shouldNotDeleteRepoIfNotAFork() throws Exception {

        // Mock
        doReturn("fake-owner").when(config).getGithubOwner();
        doReturn(null).when(config).getGithubAppId();
        GHRepository repository = Mockito.mock(GHRepository.class);
        GHRepository fork = Mockito.mock(GHRepository.class);
        GHMyself myself = Mockito.mock(GHMyself.class);
        GHPullRequest pr = Mockito.mock(GHPullRequest.class);
        GHCommitPointer head = Mockito.mock(GHCommitPointer.class);
        GHPullRequestQueryBuilder prQuery = Mockito.mock(GHPullRequestQueryBuilder.class);
        PagedIterable<?> prQueryList = Mockito.mock(PagedIterable.class);

        doReturn(false).when(fork).isFork();
        doReturn(fork).when(github).getRepository(eq("fake-owner/fake-repo"));
        doReturn("fake-owner/fake-repo").when(fork).getFullName();
        doReturn("fake-repo").when(plugin).getRepositoryName();
        doReturn(myself).when(github).getMyself();
        doReturn(fork).when(myself).getRepository(eq("fake-repo"));
        doReturn(repository).when(plugin).getRemoteRepository(eq(service));
        doReturn(fork).when(plugin).getRemoteForkRepository(eq(service));

        // One PR open but not from the fork
        doReturn(head).when(pr).getHead();
        GHRepository otherFork = Mockito.mock(GHRepository.class);
        doReturn("an/other").when(otherFork).getFullName();
        doReturn(otherFork).when(head).getRepository();
        doReturn(prQuery).when(repository).queryPullRequests();
        doReturn(prQuery).when(prQuery).state(eq(GHIssueState.OPEN));
        doReturn(prQueryList).when(prQuery).list();
        doReturn(List.of(pr)).when(prQueryList).toList();

        // Test
        service.deleteFork(plugin);
        verify(fork, never()).delete();
    }

    @Test
    public void shouldNotDeleteRepoForkNotDetachedFromJenkinsOrg() throws Exception {

        // Mock
        doReturn("fake-owner").when(config).getGithubOwner();
        doReturn(null).when(config).getGithubAppId();
        GHRepository repository = Mockito.mock(GHRepository.class);
        GHRepository fork = Mockito.mock(GHRepository.class);
        GHMyself myself = Mockito.mock(GHMyself.class);
        GHPullRequest pr = Mockito.mock(GHPullRequest.class);
        GHCommitPointer head = Mockito.mock(GHCommitPointer.class);
        GHPullRequestQueryBuilder prQuery = Mockito.mock(GHPullRequestQueryBuilder.class);
        PagedIterable<?> prQueryList = Mockito.mock(PagedIterable.class);

        doReturn(true).when(fork).isFork();
        doReturn(fork).when(github).getRepository(eq("fake-owner/fake-repo"));
        doReturn("fake-owner/fake-repo").when(fork).getFullName();
        doReturn("fake-repo").when(plugin).getRepositoryName();
        doReturn(myself).when(github).getMyself();
        doReturn(fork).when(myself).getRepository(eq("fake-repo"));
        doReturn(repository).when(plugin).getRemoteRepository(eq(service));
        doReturn(fork).when(plugin).getRemoteForkRepository(eq(service));

        // One PR open but not from the fork
        doReturn(head).when(pr).getHead();
        GHRepository otherFork = Mockito.mock(GHRepository.class);
        doReturn("an/other").when(otherFork).getFullName();
        doReturn(otherFork).when(head).getRepository();
        doReturn(prQuery).when(repository).queryPullRequests();
        doReturn(prQuery).when(prQuery).state(eq(GHIssueState.OPEN));
        doReturn(prQueryList).when(prQuery).list();
        doReturn(List.of(pr)).when(prQueryList).toList();

        // Owner of the fork is jenkinsci
        doReturn(Settings.ORGANIZATION).when(fork).getOwnerName();

        // Test
        service.deleteFork(plugin);
        verify(fork, never()).delete();
    }

    @Test
    public void shouldDeleteForkIfAllConditionsMet() throws Exception {

        // Mock
        doReturn("fake-owner").when(config).getGithubOwner();
        doReturn(null).when(config).getGithubAppId();
        GHRepository repository = Mockito.mock(GHRepository.class);
        GHRepository fork = Mockito.mock(GHRepository.class);
        GHMyself myself = Mockito.mock(GHMyself.class);
        GHPullRequest pr = Mockito.mock(GHPullRequest.class);
        GHCommitPointer head = Mockito.mock(GHCommitPointer.class);
        GHPullRequestQueryBuilder prQuery = Mockito.mock(GHPullRequestQueryBuilder.class);
        PagedIterable<?> prQueryList = Mockito.mock(PagedIterable.class);

        doReturn(true).when(fork).isFork();
        doReturn(fork).when(github).getRepository(eq("fake-owner/fake-repo"));
        doReturn("fake-owner").when(fork).getOwnerName();
        doReturn("fake-owner/fake-repo").when(fork).getFullName();
        doReturn("fake-repo").when(plugin).getRepositoryName();
        doReturn(myself).when(github).getMyself();
        doReturn(fork).when(myself).getRepository(eq("fake-repo"));
        doReturn(repository).when(plugin).getRemoteRepository(eq(service));
        doReturn(fork).when(plugin).getRemoteForkRepository(eq(service));

        // One PR open but not from the fork
        doReturn(head).when(pr).getHead();
        GHRepository otherFork = Mockito.mock(GHRepository.class);
        doReturn("an/other").when(otherFork).getFullName();
        doReturn(otherFork).when(head).getRepository();
        doReturn(prQuery).when(repository).queryPullRequests();
        doReturn(prQuery).when(prQuery).state(eq(GHIssueState.OPEN));
        doReturn(prQueryList).when(prQuery).list();
        doReturn(List.of(pr)).when(prQueryList).toList();

        // Test
        service.deleteFork(plugin);
        verify(fork, times(1)).delete();
        verify(plugin, times(1)).withoutCommits();
        verify(plugin, times(1)).withoutChangesPushed();
    }

    @Test
    public void shouldSshFetchOriginalRepoInDryRunModeToNewFolder() throws Exception {

        // Mock
        GHRepository repository = Mockito.mock(GHRepository.class);
        Git git = Mockito.mock(Git.class);
        CloneCommand cloneCommand = Mockito.mock(CloneCommand.class);

        // Use SSH key auth
        Field field = ReflectionUtils.findFields(
                        GHService.class,
                        f -> f.getName().equals("sshKeyAuth"),
                        ReflectionUtils.HierarchyTraversalMode.TOP_DOWN)
                .get(0);
        field.setAccessible(true);
        field.set(service, true);

        doReturn(repository).when(plugin).getRemoteRepository(eq(service));
        doReturn(git).when(cloneCommand).call();
        doReturn("fake-url").when(repository).getSshUrl();
        doReturn(cloneCommand).when(cloneCommand).setRemote(eq("origin"));
        doReturn(cloneCommand).when(cloneCommand).setURI(eq("ssh:///fake-url"));
        doReturn(cloneCommand).when(cloneCommand).setCredentialsProvider(any(CredentialsProvider.class));
        doReturn(cloneCommand).when(cloneCommand).setDirectory(any(File.class));

        // Directory doesn't exists
        doReturn(Path.of("not-existing-dir")).when(plugin).getLocalRepository();

        // Test
        try (MockedStatic<Git> mockStaticGit = mockStatic(Git.class)) {
            mockStaticGit.when(Git::cloneRepository).thenReturn(cloneCommand);
            service.fetch(plugin, RepoType.PLUGIN);
            verify(cloneCommand, times(1)).call();
            verifyNoMoreInteractions(cloneCommand);
        }
    }

    @Test
    public void shouldSshFetchOriginalMetadataRepoInDryRunModeToNewFolder() throws Exception {

        // Mock
        GHRepository repository = Mockito.mock(GHRepository.class);
        Git git = Mockito.mock(Git.class);
        CloneCommand cloneCommand = Mockito.mock(CloneCommand.class);

        // Use SSH key auth
        Field field = ReflectionUtils.findFields(
                        GHService.class,
                        f -> f.getName().equals("sshKeyAuth"),
                        ReflectionUtils.HierarchyTraversalMode.TOP_DOWN)
                .get(0);
        field.setAccessible(true);
        field.set(service, true);

        doReturn(repository).when(plugin).getRemoteMetadataRepository(eq(service));
        doReturn(git).when(cloneCommand).call();
        doReturn("fake-url").when(repository).getSshUrl();
        doReturn(cloneCommand).when(cloneCommand).setRemote(eq("origin"));
        doReturn(cloneCommand).when(cloneCommand).setURI(eq("ssh:///fake-url"));
        doReturn(cloneCommand).when(cloneCommand).setCredentialsProvider(any(CredentialsProvider.class));
        doReturn(cloneCommand).when(cloneCommand).setDirectory(any(File.class));

        // Directory doesn't exists
        doReturn(Path.of("not-existing-dir")).when(plugin).getLocalMetadataRepository();

        // Test
        try (MockedStatic<Git> mockStaticGit = mockStatic(Git.class)) {
            mockStaticGit.when(Git::cloneRepository).thenReturn(cloneCommand);
            service.fetch(plugin, RepoType.METADATA);
            verify(cloneCommand, times(1)).call();
            verifyNoMoreInteractions(cloneCommand);
        }
    }

    @Test
    public void shouldHttpFetchOriginalRepoInDryRunModeToNewFolder() throws Exception {

        // Mock
        GHRepository repository = Mockito.mock(GHRepository.class);
        Git git = Mockito.mock(Git.class);
        CloneCommand cloneCommand = Mockito.mock(CloneCommand.class);

        doReturn(repository).when(plugin).getRemoteRepository(eq(service));
        doReturn(git).when(cloneCommand).call();
        doReturn("fake-url").when(repository).getHttpTransportUrl();
        doReturn(cloneCommand).when(cloneCommand).setRemote(eq("origin"));
        doReturn(cloneCommand).when(cloneCommand).setURI(eq("fake-url"));
        doReturn(cloneCommand).when(cloneCommand).setCredentialsProvider(any(CredentialsProvider.class));
        doReturn(cloneCommand).when(cloneCommand).setDirectory(any(File.class));

        // Directory doesn't exists
        doReturn(Path.of("not-existing-dir")).when(plugin).getLocalRepository();

        // Test
        try (MockedStatic<Git> mockStaticGit = mockStatic(Git.class)) {
            mockStaticGit.when(Git::cloneRepository).thenReturn(cloneCommand);
            service.fetch(plugin, RepoType.PLUGIN);
            verify(cloneCommand, times(1)).call();
            verifyNoMoreInteractions(cloneCommand);
        }
    }

    @Test
    public void shouldHttpFetchOriginalMetadataRepoInDryRunModeToNewFolder() throws Exception {

        // Mock
        GHRepository repository = Mockito.mock(GHRepository.class);
        Git git = Mockito.mock(Git.class);
        CloneCommand cloneCommand = Mockito.mock(CloneCommand.class);

        doReturn(repository).when(plugin).getRemoteMetadataRepository(eq(service));
        doReturn(git).when(cloneCommand).call();
        doReturn("fake-url").when(repository).getHttpTransportUrl();
        doReturn(cloneCommand).when(cloneCommand).setRemote(eq("origin"));
        doReturn(cloneCommand).when(cloneCommand).setURI(eq("fake-url"));
        doReturn(cloneCommand).when(cloneCommand).setCredentialsProvider(any(CredentialsProvider.class));
        doReturn(cloneCommand).when(cloneCommand).setDirectory(any(File.class));

        // Directory doesn't exists
        doReturn(Path.of("not-existing-dir")).when(plugin).getLocalMetadataRepository();

        // Test
        try (MockedStatic<Git> mockStaticGit = mockStatic(Git.class)) {
            mockStaticGit.when(Git::cloneRepository).thenReturn(cloneCommand);
            service.fetch(plugin, RepoType.METADATA);
            verify(cloneCommand, times(1)).call();
            verifyNoMoreInteractions(cloneCommand);
        }
    }

    @Test
    public void shouldSshFetchOriginalRepoInMetaDataOnlyModeToNewFolder() throws Exception {

        // Mock
        GHRepository repository = Mockito.mock(GHRepository.class);
        Git git = Mockito.mock(Git.class);
        CloneCommand cloneCommand = Mockito.mock(CloneCommand.class);

        // Use SSH key auth
        Field field = ReflectionUtils.findFields(
                        GHService.class,
                        f -> f.getName().equals("sshKeyAuth"),
                        ReflectionUtils.HierarchyTraversalMode.TOP_DOWN)
                .get(0);
        field.setAccessible(true);
        field.set(service, true);

        // doReturn("fake-repo").when(plugin).getRepositoryName();
        doReturn(repository).when(plugin).getRemoteRepository(eq(service));
        doReturn(git).when(cloneCommand).call();
        doReturn("fake-url").when(repository).getSshUrl();
        doReturn(cloneCommand).when(cloneCommand).setRemote(eq("origin"));
        doReturn(cloneCommand).when(cloneCommand).setURI(eq("ssh:///fake-url"));
        doReturn(cloneCommand).when(cloneCommand).setDirectory(any(File.class));
        doReturn(cloneCommand).when(cloneCommand).setCredentialsProvider(any(CredentialsProvider.class));

        // Directory doesn't exists
        doReturn(Path.of("not-existing-dir")).when(plugin).getLocalRepository();

        // Test
        try (MockedStatic<Git> mockStaticGit = mockStatic(Git.class)) {
            mockStaticGit.when(Git::cloneRepository).thenReturn(cloneCommand);
            service.fetch(plugin, RepoType.PLUGIN);
            verify(cloneCommand, times(1)).call();
            verifyNoMoreInteractions(cloneCommand);
        }
    }

    @Test
    public void shouldHttpFetchOriginalRepoInMetaDataOnlyModeToNewFolder() throws Exception {

        // Mock
        GHRepository repository = Mockito.mock(GHRepository.class);
        Git git = Mockito.mock(Git.class);
        CloneCommand cloneCommand = Mockito.mock(CloneCommand.class);

        // doReturn("fake-repo").when(plugin).getRepositoryName();
        doReturn(repository).when(plugin).getRemoteRepository(eq(service));
        doReturn(git).when(cloneCommand).call();
        doReturn("fake-url").when(repository).getHttpTransportUrl();
        doReturn(cloneCommand).when(cloneCommand).setRemote(eq("origin"));
        doReturn(cloneCommand).when(cloneCommand).setURI(eq("fake-url"));
        doReturn(cloneCommand).when(cloneCommand).setDirectory(any(File.class));
        doReturn(cloneCommand).when(cloneCommand).setCredentialsProvider(any(CredentialsProvider.class));

        // Directory doesn't exists
        doReturn(Path.of("not-existing-dir")).when(plugin).getLocalRepository();

        // Test
        try (MockedStatic<Git> mockStaticGit = mockStatic(Git.class)) {
            mockStaticGit.when(Git::cloneRepository).thenReturn(cloneCommand);
            service.fetch(plugin, RepoType.PLUGIN);
            verify(cloneCommand, times(1)).call();
            verifyNoMoreInteractions(cloneCommand);
        }
    }

    @Test
    public void shouldOpenPullRequest() throws Exception {

        // Mocks
        Recipe recipe = Mockito.mock(Recipe.class);
        GHRepository repository = Mockito.mock(GHRepository.class);
        GHPullRequest pr = Mockito.mock(GHPullRequest.class);
        GHPullRequestQueryBuilder prQuery = Mockito.mock(GHPullRequestQueryBuilder.class);
        GHPullRequest toDeletePr = Mockito.mock(GHPullRequest.class);
        PagedIterable<?> prQueryList = Mockito.mock(PagedIterable.class);

        doReturn(recipe).when(config).getRecipe();
        doReturn("recipe1").when(recipe).getName();
        doReturn("test").when(config).getGithubOwner();
        doReturn(null).when(config).getGithubAppTargetInstallationId();
        doReturn(false).when(config).isDraft();
        doReturn(true).when(plugin).hasChangesPushed();
        doReturn("main").when(repository).getDefaultBranch();
        doReturn(repository).when(plugin).getRemoteRepository(eq(service));
        doReturn(Set.of("dependencies", "skip-build", "foo, bar", "developer"))
                .when(plugin)
                .getTags();
        GHCommitPointer toDeleteHead = Mockito.mock(GHCommitPointer.class);

        // PR to delete
        doReturn("plugin-modernizer-tool").when(toDeleteHead).getRef();
        doReturn(toDeleteHead).when(toDeletePr).getHead();

        // Return just one PR to deleete
        doReturn(prQuery).when(repository).queryPullRequests();
        doReturn(prQuery).when(prQuery).state(eq(GHIssueState.OPEN));

        // Match head/base filter for findExistingPullRequest (return empty)
        GHPullRequestQueryBuilder prQueryWithFilter = Mockito.mock(GHPullRequestQueryBuilder.class);
        doReturn(prQueryWithFilter).when(prQuery).head(any());
        doReturn(prQueryWithFilter).when(prQueryWithFilter).base(any());
        PagedIterable<?> emptyIterable = Mockito.mock(PagedIterable.class);
        doReturn(List.of()).when(emptyIterable).toList();
        doReturn(emptyIterable).when(prQueryWithFilter).list();

        // Match no-filter for deleteLegacyPrs (return match)
        doReturn(prQueryList).when(prQuery).list();
        doReturn(List.of(toDeletePr)).when(prQueryList).toList();

        doReturn(pr)
                .when(repository)
                .createPullRequest(anyString(), anyString(), any(), anyString(), eq(true), eq(false));

        doReturn(new URL("https://github.com/owner/repo/pull/123")).when(pr).getHtmlUrl();

        // Test
        service.openPullRequest(plugin, RepoType.PLUGIN);

        // We delete a PR
        verify(toDeletePr, times(1)).close();
    }

    @Test
    public void shouldOpenMetadataPullRequest() throws Exception {

        // Mocks

        GHRepository repository = Mockito.mock(GHRepository.class);
        GHPullRequest pr = Mockito.mock(GHPullRequest.class);
        GHPullRequestQueryBuilder prQuery = Mockito.mock(GHPullRequestQueryBuilder.class);

        doReturn("example").when(plugin).getName();
        doReturn(null).when(config).getGithubAppTargetInstallationId();
        doReturn(false).when(config).isDraft();
        doReturn(true).when(plugin).hasMetadataChangesPushed();
        doReturn(repository).when(plugin).getRemoteMetadataRepository(eq(service));

        // Return just one PR to deleete
        doReturn(prQuery).when(repository).queryPullRequests();
        doReturn(prQuery).when(prQuery).state(eq(GHIssueState.OPEN));

        // Match head/base filter for findExistingPullRequest (return empty)
        GHPullRequestQueryBuilder prQueryWithFilter = Mockito.mock(GHPullRequestQueryBuilder.class);
        doReturn(prQueryWithFilter).when(prQuery).head(any());
        doReturn(prQueryWithFilter).when(prQueryWithFilter).base(any());
        PagedIterable<?> emptyIterable = Mockito.mock(PagedIterable.class);
        doReturn(List.of()).when(emptyIterable).toList();
        doReturn(emptyIterable).when(prQueryWithFilter).list();

        // Match no-filter for deleteLegacyPrs (return match)

        doReturn("main").when(repository).getDefaultBranch();

        doReturn("test").when(config).getGithubOwner();

        doReturn(pr)
                .when(repository)
                .createPullRequest(anyString(), anyString(), any(), anyString(), eq(true), eq(false));
        Mockito.lenient()
                .doReturn(new URL("https://github.com/owner/repo/pull/123"))
                .when(pr)
                .getHtmlUrl();

        // Test
        service.openPullRequest(plugin, RepoType.METADATA);
        verify(repository).createPullRequest(anyString(), anyString(), any(), anyString(), eq(true), eq(false));
    }

    @Test
    public void shouldUpdatePullRequest() throws Exception {
        // Test removed as updating PRs is no longer supported (idempotency skips
        // creation)
    }

    @Test
    public void shouldUpdateMetadataPullRequest() throws Exception {
        // Test removed as updating PRs is no longer supported (idempotency skips
        // creation)
    }

    @Test
    public void shouldOpenDraftPullRequest() throws Exception {

        // Mocks
        Recipe recipe = Mockito.mock(Recipe.class);
        GHRepository repository = Mockito.mock(GHRepository.class);
        GHPullRequest pr = Mockito.mock(GHPullRequest.class);
        GHPullRequestQueryBuilder prQuery = Mockito.mock(GHPullRequestQueryBuilder.class);
        PagedIterable<?> prQueryList = Mockito.mock(PagedIterable.class);

        doReturn(recipe).when(config).getRecipe();
        doReturn("recipe1").when(recipe).getName();
        doReturn("test").when(config).getGithubOwner();
        doReturn(null).when(config).getGithubAppTargetInstallationId();
        doReturn(true).when(config).isDraft();
        doReturn(true).when(plugin).hasChangesPushed();
        doReturn(repository).when(plugin).getRemoteRepository(eq(service));

        // Return no open PR
        doReturn(prQuery).when(repository).queryPullRequests();
        doReturn(prQuery).when(prQuery).state(eq(GHIssueState.OPEN));
        doReturn(prQuery).when(prQuery).head(any());
        doReturn(prQuery).when(prQuery).base(any());
        doReturn(prQueryList).when(prQuery).list();
        doReturn("main").when(repository).getDefaultBranch();
        doReturn(List.of()).when(prQueryList).toList();

        doReturn(pr)
                .when(repository)
                .createPullRequest(anyString(), anyString(), any(), anyString(), eq(true), eq(true));

        doReturn(new URL("https://github.com/owner/repo/pull/123")).when(pr).getHtmlUrl();

        // Test
        service.openPullRequest(plugin, RepoType.PLUGIN);
    }

    @Test
    public void testCreatePullRequest_SkippedIfDuplicateExists() throws Exception {
        // Mocks
        Recipe recipe = Mockito.mock(Recipe.class);
        GHRepository repository = Mockito.mock(GHRepository.class);
        GHPullRequest existingPr = Mockito.mock(GHPullRequest.class);
        GHPullRequestQueryBuilder prQuery = Mockito.mock(GHPullRequestQueryBuilder.class);
        PagedIterable<?> prQueryList = Mockito.mock(PagedIterable.class);

        doReturn(recipe).when(config).getRecipe();
        doReturn("recipe1").when(recipe).getName();
        doReturn("test").when(config).getGithubOwner();
        doReturn("main").when(repository).getDefaultBranch();
        doReturn(true).when(plugin).hasChangesPushed();

        doReturn(repository).when(plugin).getRemoteRepository(eq(service));
        Mockito.lenient().doReturn(null).when(config).getGithubAppTargetInstallationId();
        doReturn(Config.DuplicatePrStrategy.SKIP).when(config).getDuplicatePrStrategy();

        // Mock finding existing PR
        doReturn(prQuery).when(repository).queryPullRequests();
        doReturn(prQuery).when(prQuery).state(eq(GHIssueState.OPEN));
        doReturn(prQuery).when(prQuery).head(any());
        doReturn(prQuery).when(prQuery).base(any());
        doReturn(prQueryList).when(prQuery).list();
        doReturn(List.of(existingPr)).when(prQueryList).toList();
        doReturn(URI.create("https://github.com/owner/repo/pull/123").toURL())
                .when(existingPr)
                .getHtmlUrl();

        // Test
        service.openPullRequest(plugin, RepoType.PLUGIN);

        // Verify createPullRequest is NOT called
        verify(repository, never())
                .createPullRequest(anyString(), anyString(), anyString(), anyString(), anyBoolean(), anyBoolean());
    }

    @Test
    public void testCreatePullRequest_CreatesNew() throws Exception {
        // Mocks
        Recipe recipe = Mockito.mock(Recipe.class);
        GHRepository repository = Mockito.mock(GHRepository.class);
        GHPullRequest pr = Mockito.mock(GHPullRequest.class);
        GHPullRequestQueryBuilder prQuery = Mockito.mock(GHPullRequestQueryBuilder.class);
        PagedIterable<?> prQueryList = Mockito.mock(PagedIterable.class);

        doReturn(recipe).when(config).getRecipe();
        doReturn("recipe1").when(recipe).getName();
        doReturn("test").when(config).getGithubOwner();
        doReturn("main").when(repository).getDefaultBranch();
        doReturn(true).when(plugin).hasChangesPushed();
        doReturn(repository).when(plugin).getRemoteRepository(eq(service));
        doReturn(null).when(config).getGithubAppTargetInstallationId();

        // Mock NOT finding existing PR
        doReturn(prQuery).when(repository).queryPullRequests();
        doReturn(prQuery).when(prQuery).state(eq(GHIssueState.OPEN));
        doReturn(prQuery).when(prQuery).head(any());
        doReturn(prQuery).when(prQuery).base(any());
        doReturn(prQueryList).when(prQuery).list();
        doReturn(List.of()).when(prQueryList).toList();

        doReturn(pr)
                .when(repository)
                .createPullRequest(anyString(), anyString(), anyString(), anyString(), anyBoolean(), anyBoolean());
        doReturn(URI.create("https://github.com/owner/repo/pull/124").toURL())
                .when(pr)
                .getHtmlUrl();

        // Test
        service.openPullRequest(plugin, RepoType.PLUGIN);

        // Verify createPullRequest IS called
        verify(repository, times(1))
                .createPullRequest(anyString(), anyString(), anyString(), anyString(), anyBoolean(), anyBoolean());
    }
}
