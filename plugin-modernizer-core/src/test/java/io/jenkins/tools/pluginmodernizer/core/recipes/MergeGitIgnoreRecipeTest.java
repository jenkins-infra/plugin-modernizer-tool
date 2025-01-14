package io.jenkins.tools.pluginmodernizer.core.recipes;

import static org.openrewrite.test.SourceSpecs.text;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RewriteTest;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MergeGitIgnoreRecipeTest implements RewriteTest {
    private static final Logger LOG = LoggerFactory.getLogger(MergeGitIgnoreRecipeTest.class);

    private static final Path ARCHETYPE_GITIGNORE_PATH = 
        Paths.get("archetypes/common/.gitignore");

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

    private String getGitIgnoreContent() {
        try {
            if (Files.exists(ARCHETYPE_GITIGNORE_PATH)) {
                return Files.readString(ARCHETYPE_GITIGNORE_PATH);
            }
        } catch (IOException e) {
            LOG.warn("Could not read .gitignore from archetype", e);
        }
        return ARCHETYPE_GITIGNORE_CONTENT;
    }

    @Test
    void shouldMergeGitIgnoreEntries() {
        rewriteRun(
                spec -> spec.recipe(new MergeGitIgnoreRecipe(getGitIgnoreContent())),
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
                        sourceSpecs -> sourceSpecs.path(".gitignore")
                )
        );
    }

    @Test
    void shouldMergeWhenGitIgnoreDoesNotExist() {
        rewriteRun(
                spec -> spec.recipe(new MergeGitIgnoreRecipe(getGitIgnoreContent())),
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
                        sourceSpecs -> sourceSpecs.path(".gitignore")
                )
        );
    }

    @Test
    void shouldNotDuplicateExistingEntries() {
        rewriteRun(
                spec -> spec.recipe(new MergeGitIgnoreRecipe(getGitIgnoreContent())),
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
                        sourceSpecs -> sourceSpecs.path(".gitignore")
                )
        );
    }

    @Test
    void shouldMergeEntriesInCorrectOrder() {
        rewriteRun(
                spec -> spec.recipe(new MergeGitIgnoreRecipe(getGitIgnoreContent())),
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
                    sourceSpecs -> sourceSpecs.path(".gitignore")
                )
        );
    }

    @Test
    void shouldNotChangeGitIgnoreWhenNoChangesNeeded() {
        rewriteRun(
                spec -> spec.recipe(new MergeGitIgnoreRecipe(getGitIgnoreContent())),
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
                    sourceSpecs -> sourceSpecs.path(".gitignore")
                )
        );
    }
}
