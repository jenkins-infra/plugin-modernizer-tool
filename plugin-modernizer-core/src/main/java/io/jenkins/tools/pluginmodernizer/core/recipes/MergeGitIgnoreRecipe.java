package io.jenkins.tools.pluginmodernizer.core.recipes;

import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.text.PlainText;
import org.openrewrite.text.PlainTextVisitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MergeGitIgnoreRecipe extends Recipe {
    private static final Logger LOG = LoggerFactory.getLogger(MergeGitIgnoreRecipe.class);

    private static final String ARCHETYPE_GITIGNORE_CONTENT =
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
            """;

    @Override
    public String getDisplayName() {
        return "Merge .gitignore Entries";
    }

    @Override
    public String getDescription() {
        return "Merges predefined archetype .gitignore entries with the existing .gitignore file.";
    }

    @Override
    public PlainTextVisitor<ExecutionContext> getVisitor() {
        return new GitIgnoreMerger();
    }

    private static class GitIgnoreMerger extends PlainTextVisitor<ExecutionContext> {
        @Override
        public PlainText visitText(PlainText text, ExecutionContext ctx) {
            Path sourcePath = text.getSourcePath();

            // Early return if source path is null
            if (sourcePath == null) {
                return text;
            }

            // Safely get filename with null checks
            Path fileNamePath = sourcePath.getFileName();
            if (fileNamePath == null) {
                return text;
            }

            String fileName = fileNamePath.toString();
            if (!".gitignore".equals(fileName)) {
                return text; // Return early if not a .gitignore file
            }

            String existingContent = text.getText();
            String mergedContent = mergeGitIgnoreFiles(existingContent);

            // Only update if there are changes
            if (!mergedContent.equals(existingContent)) {
                LOG.info("Merging .gitignore for file: {}", sourcePath);
                return text.withText(mergedContent);
            }

            return text;
        }

        private String removeTrailingSlash(String line) {
            String trimmed = line.trim();
            return trimmed.endsWith("/") ? trimmed.substring(0, trimmed.length() - 1) : trimmed;
        }

        private String mergeGitIgnoreFiles(String existingContent) {
            // Get existing non-empty lines with normalized paths (removing trailing slashes)
            List<String> existingLines = existingContent.lines()
                    .map(line -> {
                        String trimmed = line.trim();
                        return trimmed.startsWith("#") || trimmed.isEmpty() ? trimmed : removeTrailingSlash(trimmed);
                    })
                    .collect(Collectors.toList());

            StringBuilder merged = new StringBuilder();

            // Add existing content
            if (!existingContent.isEmpty()) {
                merged.append(existingContent);
                if (!existingContent.endsWith("\n")) {
                    merged.append("\n");
                }
            }

            // Process archetype entries
            String[] archetypeEntries = ARCHETYPE_GITIGNORE_CONTENT.split("\n");
            boolean hasNewEntries = false;
            StringBuilder newContent = new StringBuilder();

            for (String line : archetypeEntries) {
                String trimmed = line.trim();
                
                // Skip empty lines at the start
                if (!hasNewEntries && trimmed.isEmpty()) {
                    continue;
                }

                // Check if we need to start adding entries
                if (!hasNewEntries) {
                    if (!trimmed.isEmpty() && !trimmed.startsWith("#")) {
                        String normalized = removeTrailingSlash(trimmed);
                        if (!existingLines.contains(normalized)) {
                            hasNewEntries = true;
                            newContent.append("# Added from archetype\n");
                        }
                    }
                    if (!hasNewEntries) continue;
                }

                // Add the line if it's a comment, empty, or not already present
                if (trimmed.startsWith("#") || trimmed.isEmpty() || 
                    !existingLines.contains(removeTrailingSlash(trimmed))) {
                    newContent.append(line).append("\n");
                }
            }

            // Only append new content if we have new entries
            if (hasNewEntries) {
                merged.append(newContent);
            }

            String result = merged.toString();
            if (result.endsWith("\n")) {
                result = result.substring(0, result.length() - 1);
            }

            return result;
        }
    }
}