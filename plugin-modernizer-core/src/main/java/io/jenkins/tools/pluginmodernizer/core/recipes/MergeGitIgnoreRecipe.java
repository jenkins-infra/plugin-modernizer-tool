package io.jenkins.tools.pluginmodernizer.core.recipes;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
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

        private String mergeGitIgnoreFiles(String existingContent) {
            // Get existing non-empty lines and normalize paths (removing trailing slashes)
            Set<String> existingLines = new HashSet<>();

            for (String line : existingContent.lines().map(String::trim).collect(Collectors.toList())) {
                if (!line.isEmpty()) {
                    existingLines.add(line.endsWith("/") ? line.substring(0, line.length() - 1) : line);
                }
            }

            StringBuilder merged = new StringBuilder();

            // Add existing content
            if (!existingContent.isEmpty()) {
                merged.append(existingContent);
                if (!existingContent.endsWith("\n")) {
                    merged.append("\n");
                }
            }

            // Maintain proper formatting for archetype entries
            String[] archetypeEntries = ARCHETYPE_GITIGNORE_CONTENT.split("\n");
            boolean hasNewEntries = false;
            StringBuilder newContent = new StringBuilder();

            // Process each line from archetype content
            for (String line : archetypeEntries) {
                String trimmedLine = line.trim();

                // Skip empty lines at the start
                if (!hasNewEntries && trimmedLine.isEmpty()) {
                    continue;
                }

                // Check if we need to start adding entries
                if (!hasNewEntries) {
                    if (!trimmedLine.isEmpty() && !existingLines.contains(trimmedLine)) {
                        hasNewEntries = true;
                        newContent.append("# Added from archetype\n");
                    } else {
                        continue; // Skip already existing lines or comments
                    }
                }

                // Add all the lines that are comments, empty, or not already present in existing lines
                if (trimmedLine.startsWith("#") || trimmedLine.isEmpty() || !existingLines.contains(trimmedLine)) {
                    newContent.append(line).append("\n");
                }
            }

            // Only append new content if we have new entries
            if (hasNewEntries) {
                merged.append(newContent);
            }

            String result = merged.toString();
            if (!result.isEmpty() && result.endsWith("\n")) {
                result = result.substring(0, result.length() - 1);
            }

            return result;
        }
    }
}
