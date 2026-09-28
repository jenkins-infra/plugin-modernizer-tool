package io.jenkins.tools.pluginmodernizer.core.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.jenkins.tools.pluginmodernizer.core.config.Config;
import io.jenkins.tools.pluginmodernizer.core.config.Settings;
import io.jenkins.tools.pluginmodernizer.core.model.Plugin;
import io.jenkins.tools.pluginmodernizer.core.model.Recipe;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import org.apache.maven.shared.invoker.InvocationRequest;
import org.apache.maven.shared.invoker.InvocationResult;
import org.apache.maven.shared.invoker.Invoker;
import org.apache.maven.shared.invoker.MavenInvocationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Test for {@link MavenInvoker}.
 */
@ExtendWith(MockitoExtension.class)
@Execution(ExecutionMode.CONCURRENT)
public class MavenInvokerTest {

    @Mock
    private Config config;

    @Mock
    private Invoker invoker;

    @Mock
    private InvocationResult invocationResult;

    @Mock
    private Plugin plugin;

    @InjectMocks
    private MavenInvoker mavenInvoker;

    @TempDir
    private Path tempDir;

    private Path sources;

    @BeforeEach
    void setUp() throws IOException, MavenInvocationException {
        sources = Files.createDirectories(tempDir.resolve("sources"));
        Files.writeString(sources.resolve("pom.xml"), "<project/>");

        // isValidMavenHome only requires bin/mvn.cmd to exist, so no real Maven is needed
        Files.createDirectories(tempDir.resolve("maven/bin"));
        Files.writeString(tempDir.resolve("maven/bin/mvn.cmd"), "");

        when(plugin.getLocalRepository()).thenReturn(sources);
        when(config.getConfiguredMavenHome()).thenReturn(tempDir.resolve("maven"));
        when(config.getMavenLocalRepo()).thenReturn(tempDir.resolve("repo"));
        when(config.getVersion()).thenReturn("999999-SNAPSHOT");
        when(invoker.execute(any())).thenReturn(invocationResult);
    }

    private static Recipe recipe(String name, String... tags) {
        Recipe recipe = new Recipe();
        recipe.setName(name);
        recipe.setTags(Set.of(tags));
        return recipe;
    }

    private String capturedRewriteGoal() throws MavenInvocationException {
        ArgumentCaptor<InvocationRequest> captor = ArgumentCaptor.forClass(InvocationRequest.class);
        verify(invoker).execute(captor.capture());
        return captor.getValue().getArgs().stream()
                .filter(arg -> arg.startsWith("org.openrewrite.maven:rewrite-maven-plugin:"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("no rewrite goal was passed to maven"));
    }

    @Test
    void usesRunNoForkForSkipVerificationRecipe() throws MavenInvocationException {
        when(config.getRecipe())
                .thenReturn(recipe("io.jenkins.tools.pluginmodernizer.SetupJenkinsfile", "chore", "skip-verification"));

        mavenInvoker.invokeRewrite(plugin);

        assertThat(capturedRewriteGoal()).endsWith(":runNoFork");
    }

    @Test
    void usesRunForRecipeWithoutSkipVerification() throws MavenInvocationException {
        when(config.getRecipe()).thenReturn(recipe("io.jenkins.tools.pluginmodernizer.UpgradeBomVersion", "chore"));

        mavenInvoker.invokeRewrite(plugin);

        assertThat(capturedRewriteGoal()).endsWith(":run");
    }

    @Test
    void metadataCollectionKeepsForking() throws MavenInvocationException {
        mavenInvoker.collectMetadata(plugin);

        assertThat(Settings.FETCH_METADATA_RECIPE.isSkipVerification()).isFalse();
        assertThat(capturedRewriteGoal()).endsWith(":run");
    }
}
