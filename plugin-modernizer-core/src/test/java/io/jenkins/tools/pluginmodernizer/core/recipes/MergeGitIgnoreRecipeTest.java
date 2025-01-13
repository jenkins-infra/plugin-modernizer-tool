package io.jenkins.tools.pluginmodernizer.core.recipes;

import static org.openrewrite.test.SourceSpecs.text;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.openrewrite.test.RewriteTest;

public class MergeGitIgnoreRecipeTest implements RewriteTest {

    @TempDir
    Path tempDir;

    Path archetypeGitignorePath;

    private static final String ARCHETYPE_GITIGNORE_CONTENT =
            """
            # Added from archetype
            target
            work
            *.iml
            *.iws
            *.ipr
            .settings
            .classpath
            .project""";

    @BeforeEach
    void setup() throws IOException {
        archetypeGitignorePath = tempDir.resolve(".gitignore");
        Files.writeString(archetypeGitignorePath, ARCHETYPE_GITIGNORE_CONTENT);
    }

    @Test
    void shouldMergeGitIgnoreEntries() {
        rewriteRun(
                spec -> spec.recipe(new MergeGitIgnoreRecipe(archetypeGitignorePath)),
                text(
                        """
                # Existing entries
                *.log
                build/
                .idea/
                # Custom section
                custom/*.tmp""",
                        """
                # Existing entries
                *.log
                build/
                .idea/
                # Custom section
                custom/*.tmp
                # Added from archetype
                target
                work
                *.iml
                *.iws
                *.ipr
                .settings
                .classpath
                .project""",
                        sourceSpecs -> sourceSpecs.path(".gitignore")));
    }

    @Test
    void shouldMergeWhenGitIgnoreDoesNotExist() {
        rewriteRun(
                spec -> spec.recipe(new MergeGitIgnoreRecipe(archetypeGitignorePath)),
                text(
                        "",
                        """
                # Added from archetype
                target
                work
                *.iml
                *.iws
                *.ipr
                .settings
                .classpath
                .project""",
                        sourceSpecs -> sourceSpecs.path(".gitignore")));
    }

    @Test
    void shouldNotDuplicateExistingEntries() {
        rewriteRun(
                spec -> spec.recipe(new MergeGitIgnoreRecipe(archetypeGitignorePath)),
                text(
                        """
                # Existing entries
                target
                *.iml
                .settings""",
                        """
                # Existing entries
                target
                *.iml
                .settings
                # Added from archetype
                work
                *.iws
                *.ipr
                .classpath
                .project""",
                        sourceSpecs -> sourceSpecs.path(".gitignore")));
    }

    @Test
    void shouldMergeEntriesInCorrectOrder() {
        rewriteRun(
                spec -> spec.recipe(new MergeGitIgnoreRecipe(archetypeGitignorePath)),
                text(
                        """
                # Existing entries
                *.log
                build/
                .idea/
                # Custom section
                custom/*.tmp""",
                        """
                # Existing entries
                *.log
                build/
                .idea/
                # Custom section
                custom/*.tmp
                # Added from archetype
                target
                work
                *.iml
                *.iws
                *.ipr
                .settings
                .classpath
                .project""",
                        sourceSpecs -> sourceSpecs.path(".gitignore")));
    }

    @Test
    void shouldNotChangeGitIgnoreWhenNoChangesNeeded() {
        rewriteRun(
                spec -> spec.recipe(new MergeGitIgnoreRecipe(archetypeGitignorePath)),
                text(
                        """
                # Existing entries
                target
                work
                *.iml
                *.iws
                *.ipr
                .settings
                .classpath
                .project""",
                        sourceSpecs -> sourceSpecs.path(".gitignore")));
    }
}
