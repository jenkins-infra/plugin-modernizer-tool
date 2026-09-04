package io.jenkins.tools.pluginmodernizer.cli;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import picocli.CommandLine;

/**
 * Verify that every project subcommand exposed by {@link Main} (except the picocli-provided
 * {@code generate-completion}) exposes --help/-h and --version/-V.
 */
public class SubcommandHelpOptionsTest {

    /**
     * Contributed by picocli itself, so it has no VersionProvider of ours.
     */
    private static final String GENERATED_COMPLETION = "generate-completion";

    /**
     * Subcommand names taken from Main, so a newly added subcommand is covered without touching this
     * test. Includes the fetch-metadata alias, which resolves to the same spec as build-metadata.
     */
    private static Stream<String> subcommandNames() {
        return new CommandLine(new Main())
                .getSubcommands().keySet().stream().filter(name -> !GENERATED_COMPLETION.equals(name));
    }

    private static CommandLine.Model.CommandSpec specOf(String name) {
        CommandLine subcommand = new CommandLine(new Main()).getSubcommands().get(name);
        assertNotNull(subcommand, "Subcommand '%s' is not registered on Main".formatted(name));
        return subcommand.getCommandSpec();
    }

    @Test
    public void shouldRegisterEverySubcommand() {
        assertTrue(
                subcommandNames()
                        .toList()
                        .containsAll(java.util.List.of(
                                "validate", "run", "dry-run", "cleanup", "recipes", "build-metadata", "version")),
                "A documented subcommand is no longer registered on Main, found: "
                        + subcommandNames().toList());
    }

    @ParameterizedTest
    @MethodSource("subcommandNames")
    public void shouldEnableStandardHelpOptions(String name) {
        CommandLine.Model.CommandSpec spec = specOf(name);
        assertTrue(
                spec.mixinStandardHelpOptions(),
                "Subcommand '%s' must set mixinStandardHelpOptions so --help/-h and --version/-V work".formatted(name));
        assertNotNull(spec.findOption("--help"), "Subcommand '%s' must expose --help".formatted(name));
        assertNotNull(spec.findOption("-h"), "Subcommand '%s' must expose -h".formatted(name));
        assertNotNull(spec.findOption("--version"), "Subcommand '%s' must expose --version".formatted(name));
        assertNotNull(spec.findOption("-V"), "Subcommand '%s' must expose -V".formatted(name));
    }

    @ParameterizedTest
    @MethodSource("subcommandNames")
    public void shouldUseSharedVersionProvider(String name) {
        assertInstanceOf(
                VersionProvider.class,
                specOf(name).versionProvider(),
                "Subcommand '%s' must reuse VersionProvider so --version matches the top-level command"
                        .formatted(name));
    }

    @Test
    public void shouldRegisterBuildMetadataAlias() {
        // Resolve both names from the same CommandLine, otherwise the specs are distinct instances
        CommandLine main = new CommandLine(new Main());
        CommandLine buildMetadata = main.getSubcommands().get("build-metadata");
        CommandLine fetchMetadata = main.getSubcommands().get("fetch-metadata");
        assertNotNull(buildMetadata, "Subcommand 'build-metadata' is not registered on Main");
        assertNotNull(fetchMetadata, "Subcommand 'fetch-metadata' is not registered on Main");
        assertSame(
                buildMetadata.getCommandSpec(),
                fetchMetadata.getCommandSpec(),
                "fetch-metadata must remain an alias of build-metadata");
    }
}
