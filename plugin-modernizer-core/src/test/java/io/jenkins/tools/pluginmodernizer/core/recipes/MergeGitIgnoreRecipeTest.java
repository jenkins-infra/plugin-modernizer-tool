package io.jenkins.tools.pluginmodernizer.core.recipes;

import static org.openrewrite.test.SourceSpecs.text;

import io.jenkins.tools.pluginmodernizer.core.extractor.ArchetypeCommonFile;
import org.junit.jupiter.api.Test;
import org.openrewrite.test.RewriteTest;

public class MergeGitIgnoreRecipeTest implements RewriteTest {

    @Test
    void shouldMergeGitIgnoreEntries() {
        rewriteRun(
                spec -> spec.recipe(new MergeGitIgnoreRecipe()),
                text(""), // Need one minimum file to trigger the recipe
                text(
                        """
                    # Existing entries
                    *.log
                    build
                    .idea
                    # Custom section
                    custom/*.tmp
                    """,
                        """
                    # Existing entries
                    *.log
                    build
                    .idea
                    # Custom section
                    custom/*.tmp
                    # Added from archetype
                    target

                    # mvn hpi:run
                    work

                    # IntelliJ IDEA project files
                    *.iml
                    *.iws
                    *.ipr

                    # Eclipse project files
                    .settings
                    .classpath
                    .project
                    """,
                        sourceSpecs -> sourceSpecs.path(ArchetypeCommonFile.GITIGNORE.getPath())));
    }

    @Test
    void shouldMergeWhenGitIgnoreIsEmpty() {
        rewriteRun(
                spec -> spec.recipe(new MergeGitIgnoreRecipe()),
                text(""), // Need one minimum file to trigger the recipe
                text(
                        "",
                        """
                    # Added from archetype
                    target

                    # mvn hpi:run
                    work

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
                        sourceSpecs -> sourceSpecs.path(ArchetypeCommonFile.GITIGNORE.getPath())));
    }

    @Test
    void shouldNotDuplicateExistingEntries() {
        rewriteRun(
                spec -> spec.recipe(new MergeGitIgnoreRecipe()),
                text(""), // Need one minimum file to trigger the recipe
                text(
                        """
                    # Existing entries
                    target
                    *.iml
                    .settings
                    .idea
                    """,
                        """
                    # Existing entries
                    target
                    *.iml
                    .settings
                    .idea
                    # Added from archetype
                    # mvn hpi:run
                    work

                    # IntelliJ IDEA project files
                    *.iws
                    *.ipr

                    # Eclipse project files
                    .classpath
                    .project
                    """,
                        sourceSpecs -> sourceSpecs.path(ArchetypeCommonFile.GITIGNORE.getPath())));
    }

    @Test
    void shouldMergeEntriesInCorrectOrder() {
        rewriteRun(
                spec -> spec.recipe(new MergeGitIgnoreRecipe()),
                text(""), // Need one minimum file to trigger the recipe
                text(
                        """
                    # Existing entries
                    *.log
                    build
                    .idea
                    # Custom section
                    custom/*.tmp""",
                        """
                    # Existing entries
                    *.log
                    build
                    .idea
                    # Custom section
                    custom/*.tmp
                    # Added from archetype
                    target

                    # mvn hpi:run
                    work

                    # IntelliJ IDEA project files
                    *.iml
                    *.iws
                    *.ipr

                    # Eclipse project files
                    .settings
                    .classpath
                    .project
                    """,
                        sourceSpecs -> sourceSpecs.path(ArchetypeCommonFile.GITIGNORE.getPath())));
    }

    @Test
    void shouldNotChangeGitIgnoreWhenNoChangesNeeded() {
        rewriteRun(
                spec -> spec.recipe(new MergeGitIgnoreRecipe()),
                text(""), // Need one minimum file to trigger the recipe
                text(
                        """
                    target

                    # mvn hpi:run
                    work

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
                        sourceSpecs -> sourceSpecs.path(ArchetypeCommonFile.GITIGNORE.getPath())));
    }

    @Test
    void shouldNotDuplicateEntriesWithTrailingSlashes() {
        rewriteRun(
                spec -> spec.recipe(new MergeGitIgnoreRecipe()),
                text(""), // Need one minimum file to trigger the recipe
                text(
                        """
                    # Existing entries
                    target/
                    work/
                    .idea/
                    .settings/
                    """,
                        """
                    # Existing entries
                    target/
                    work/
                    .idea/
                    .settings/
                    # Added from archetype
                    # mvn hpi:run

                    # IntelliJ IDEA project files
                    *.iml
                    *.iws
                    *.ipr

                    # Eclipse project files
                    .classpath
                    .project
                    """,
                        sourceSpecs -> sourceSpecs.path(ArchetypeCommonFile.GITIGNORE.getPath())));
    }
}
