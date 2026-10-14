package io.jenkins.tools.pluginmodernizer.core.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import io.jenkins.tools.pluginmodernizer.core.config.Config;
import io.jenkins.tools.pluginmodernizer.core.model.Plugin;
import io.jenkins.tools.pluginmodernizer.core.model.Recipe;
import io.jenkins.tools.pluginmodernizer.core.utils.TemplateUtils;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import org.apache.maven.shared.invoker.InvocationRequest;
import org.apache.maven.shared.invoker.InvocationResult;
import org.apache.maven.shared.invoker.Invoker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openrewrite.CsvDataTableStore;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.table.SourcesFileResults;

@ExtendWith(MockitoExtension.class)
class AppliedRecipeDescriptionsTest {
    private static final String ROOT = "io.jenkins.tools.pluginmodernizer.UpgradeNextMajorParentVersion";
    private static final String DEVELOPERS = "io.jenkins.tools.pluginmodernizer.RemoveDevelopersTag";
    private static final String SECURITY =
            "io.jenkins.tools.pluginmodernizer.core.recipes.MigrateAcegiSecurityToSpringSecurity";

    @Mock
    private Config config;

    @Mock
    private Invoker invoker;

    @Mock
    private InvocationResult result;

    @InjectMocks
    private MavenInvoker mavenInvoker;

    @TempDir
    private Path tempDir;

    private Plugin plugin;
    private Recipe recipe;
    private Path datatables;

    @BeforeEach
    void setUp() throws IOException {
        Path sources = Files.createDirectories(tempDir.resolve("sources"));
        Files.writeString(sources.resolve("pom.xml"), "<project/>");
        Files.createDirectories(tempDir.resolve("maven/bin"));
        Files.writeString(tempDir.resolve("maven/bin/mvn.cmd"), "");
        plugin = Plugin.build("example", sources);
        datatables = sources.resolve("target/rewrite/datatables");
        recipe = new Recipe();
        recipe.setName(ROOT);
        recipe.setDisplayName("Upgrade parent version");
        recipe.setDescription("Upgrade the plugin parent and Jenkins baseline.");
        recipe.setTags(Set.of());
        when(config.getConfiguredMavenHome()).thenReturn(tempDir.resolve("maven"));
        when(config.getMavenLocalRepo()).thenReturn(tempDir.resolve("repo"));
        when(config.getVersion()).thenReturn("999999-SNAPSHOT");
        when(config.getRecipe()).thenReturn(recipe);
    }

    @Test
    void keepsCurrentResultsAfterCleanupAndMetadataCollection() throws Exception {
        writeResults("old", SECURITY);
        when(invoker.execute(any())).thenAnswer(invocation -> {
            InvocationRequest request = invocation.getArgument(0);
            if (request.getArgs().contains("-Drewrite.activeRecipes=" + ROOT)) {
                assertThat(request.getArgs()).contains("-Drewrite.exportDatatables=true");
                writeResults("current", ROOT, DEVELOPERS, DEVELOPERS);
            } else {
                assertThat(request.getArgs()).doesNotContain("-Drewrite.exportDatatables=true");
                try (var paths = Files.walk(plugin.getLocalRepository().resolve("target"))) {
                    for (Path path :
                            paths.sorted(java.util.Comparator.reverseOrder()).toList()) {
                        Files.delete(path);
                    }
                }
                writeResults("metadata", SECURITY);
            }
            return result;
        });

        mavenInvoker.invokeRewrite(plugin);
        mavenInvoker.collectMetadata(plugin);

        String body = TemplateUtils.renderPullRequestBody(plugin, recipe);
        assertThat(body).contains("#### Removing `developers` Tag", "#### Summary");
        assertThat(body).doesNotContain("#### Jenkins baseline", "#### JEP-227");
    }

    @Test
    void missingResultsDoNotReuseAPreviousRun() throws Exception {
        writeResults("old", DEVELOPERS);
        plugin.setAppliedRecipes(Set.of(DEVELOPERS));
        when(invoker.execute(any())).thenReturn(result);

        mavenInvoker.invokeRewrite(plugin);

        assertThat(TemplateUtils.renderPullRequestBody(plugin, recipe))
                .contains("#### Jenkins baseline", "#### Removing `developers` Tag", "#### JEP-227");
    }

    @Test
    void unreadableResultsKeepFullDescriptionWithoutFailingModernization() throws Exception {
        when(invoker.execute(any())).thenAnswer(invocation -> {
            Path directory = writeResults("current", DEVELOPERS);
            Path csv = directory.resolve("org.openrewrite.table.SourcesFileResults.csv");
            String contents = Files.readString(csv);
            Files.writeString(csv, contents.replace(",60,1", ",not-a-number,1"));
            return result;
        });

        mavenInvoker.invokeRewrite(plugin);

        assertThat(plugin.hasErrors()).isFalse();
        assertThat(TemplateUtils.renderPullRequestBody(plugin, recipe))
                .contains("#### Jenkins baseline", "#### Removing `developers` Tag", "#### JEP-227");
    }

    @Test
    void doesNotUseResultsFromFailedInvocation() throws Exception {
        when(result.getExitCode()).thenReturn(1);
        when(invoker.execute(any())).thenAnswer(invocation -> {
            writeResults("current", DEVELOPERS);
            return result;
        });

        mavenInvoker.invokeRewrite(plugin);

        assertThat(plugin.hasErrors()).isTrue();
        assertThat(plugin.shouldDescribeRecipe(SECURITY)).isTrue();
    }

    private Path writeResults(String directoryName, String... recipes) {
        Path directory = datatables.resolve(directoryName);
        try (var store = new CsvDataTableStore(directory)) {
            var table = new SourcesFileResults(org.openrewrite.Recipe.noop());
            var context = new InMemoryExecutionContext();
            for (String name : recipes) {
                store.insertRow(
                        table,
                        context,
                        new SourcesFileResults.Row(
                                "path,with,commas/pom.xml", "path,with,commas/pom.xml", ROOT, name, 60L, 1));
            }
        }
        return directory;
    }
}
