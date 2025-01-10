package io.jenkins.tools.pluginmodernizer.core.recipes;

import static org.openrewrite.test.SourceSpecs.text;

import io.jenkins.tools.pluginmodernizer.core.extractor.ArchetypeCommonFile;
import org.junit.jupiter.api.Test;
import org.openrewrite.test.RewriteTest;

public class MergeGitIgnoreRecipeTest implements RewriteTest {

    private static final String ARCHETYPE_GITIGNORE_CONTENT =
            """
            target
            work

            # mvn hpi:run
            # IntelliJ IDEA project files
            *.iml
            *.iws
            *.ipr
            .idea

            # Eclipse project files
            .settings
            .classpath
            .project
            """;

    @Test
    void shouldMergeGitIgnoreEntries() {
        rewriteRun(
                spec -> spec.recipe(new MergeGitIgnoreRecipe(ArchetypeCommonFile.GITIGNORE.getPath())),
                text(
                        """
                # Existing user-defined entries
                *.log
                build/
                .idea/

                # Custom section
                custom/*.tmp
                """,
                        sourceSpecs -> sourceSpecs.path(".gitignore")),
                text(
                        ARCHETYPE_GITIGNORE_CONTENT,
                        sourceSpecs -> sourceSpecs.path(ArchetypeCommonFile.GITIGNORE.getPath())),
                text(
                        """
                # Existing user-defined entries
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
                .project
                """,
                        sourceSpecs -> sourceSpecs.path(".gitignore")));
    }

    @Test
    void shouldNotChangeGitIgnoreWhenNoChangesNeeded() {
        // Test case where the existing .gitignore already contains all entries from archetype.
        rewriteRun(
                spec -> spec.recipe(new MergeGitIgnoreRecipe(ArchetypeCommonFile.GITIGNORE.getPath())),
                text(
                        """
                        # Existing user-defined entries
                        *.log
                        build/
                        .idea/
                        target
                        work
                        *.iml
                        *.iws
                        *.ipr
                        .settings
                        .classpath
                        .project

                        # Custom section
                        custom/*.tmp
                        """,
                        sourceSpecs -> sourceSpecs.path(".gitignore")),
                text(
                        ARCHETYPE_GITIGNORE_CONTENT,
                        sourceSpecs -> sourceSpecs.path(ArchetypeCommonFile.GITIGNORE.getPath())),
                text(
                        """
                        # Existing user-defined entries
                        *.log
                        build/
                        .idea/
                        target
                        work
                        *.iml
                        *.iws
                        *.ipr
                        .settings
                        .classpath
                        .project

                        # Custom section
                        custom/*.tmp
                        """,
                        sourceSpecs -> sourceSpecs.path(".gitignore")));
    }

    @Test
    void shouldMergeWhenGitIgnoreDoesNotExist() {
        // Test case where the existing .gitignore does not exist.
        rewriteRun(
                spec -> spec.recipe(new MergeGitIgnoreRecipe(ArchetypeCommonFile.GITIGNORE.getPath())),
                text(
                        ARCHETYPE_GITIGNORE_CONTENT,
                        sourceSpecs -> sourceSpecs.path(ArchetypeCommonFile.GITIGNORE.getPath())),
                text(
                        """
                        # Added from archetype
                        target
                        work

                        # mvn hpi:run
                        # IntelliJ IDEA project files
                        *.iml
                        *.iws
                        *.ipr
                        .idea

                        # Eclipse project files
                        .settings
                        .classpath
                        .project

                       """,
                        sourceSpecs -> sourceSpecs.path(".gitignore")));
    }

    @Test
    void shouldMergeEntriesInCorrectOrder() {
        // Test case where it check if the entries are in the correct order
        rewriteRun(
                spec -> spec.recipe(new MergeGitIgnoreRecipe(ArchetypeCommonFile.GITIGNORE.getPath())),
                text(
                        """
                    # Custom section
                    custom/*.tmp

                    # Existing user-defined entries
                    build/
                    .idea/
                    *.log
                    """,
                        sourceSpecs -> sourceSpecs.path(".gitignore")),
                text(
                        ARCHETYPE_GITIGNORE_CONTENT,
                        sourceSpecs -> sourceSpecs.path(ArchetypeCommonFile.GITIGNORE.getPath())),
                text(
                        """
                    # Custom section
                    custom/*.tmp

                    # Existing user-defined entries
                    build/
                    .idea/
                    *.log

                    # Added from archetype
                    target
                    work
                    *.iml
                    *.iws
                    *.ipr
                    .settings
                    .classpath
                    .project
                    """,
                        sourceSpecs -> sourceSpecs.path(".gitignore")));
    }
}
