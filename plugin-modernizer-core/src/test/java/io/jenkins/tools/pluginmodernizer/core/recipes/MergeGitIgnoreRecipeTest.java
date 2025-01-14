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
                build/
                .idea/
                # Custom section
                custom/*.tmp
                """,
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
                .project
                """,
                        sourceSpecs -> {
                            sourceSpecs.path(ArchetypeCommonFile.GITIGNORE.getPath());
                        }));
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
                work
                *.iml
                *.iws
                *.ipr
                .settings
                .classpath
                .project
                """,
                        sourceSpecs -> {
                            sourceSpecs.path(ArchetypeCommonFile.GITIGNORE.getPath());
                        }));
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
                """,
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
                .project
                """,
                        sourceSpecs -> {
                            sourceSpecs.path(ArchetypeCommonFile.GITIGNORE.getPath());
                        }));
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
               .project
               """,
                        sourceSpecs -> {
                            sourceSpecs.path(ArchetypeCommonFile.GITIGNORE.getPath());
                        }));
    }

    @Test
    void shouldNotChangeGitIgnoreWhenNoChangesNeeded() {
        rewriteRun(
                spec -> spec.recipe(new MergeGitIgnoreRecipe()),
                text(""), // Need one minimum file to trigger the recipe
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
              .project
              """,
                        sourceSpecs -> {
                            sourceSpecs.path(ArchetypeCommonFile.GITIGNORE.getPath());
                        }));
    }
}
