package io.jenkins.tools.pluginmodernizer.core.recipes;

import java.io.IOException;
import java.nio.file.Files;
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
    private final Path archetypeGitIgnorePath;

    public MergeGitIgnoreRecipe(Path archetypeGitIgnorePath) {
        this.archetypeGitIgnorePath = archetypeGitIgnorePath;
    }

    @Override
    public String getDisplayName() {
        return "Merge .gitignore Entries";
    }

    @Override
    public String getDescription() {
        return "Merges .gitignore entries from archetype with existing .gitignore file.";
    }

    @Override
    public PlainTextVisitor<ExecutionContext> getVisitor() {
        return new GitIgnoreMerger(archetypeGitIgnorePath);
    }

    private static class GitIgnoreMerger extends PlainTextVisitor<ExecutionContext> {
        private final Path archetypeGitIgnorePath;

        GitIgnoreMerger(Path archetypeGitIgnorePath) {
            this.archetypeGitIgnorePath = archetypeGitIgnorePath;
        }

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

            try {
                if (Files.exists(archetypeGitIgnorePath)) {
                    String existingContent = text.getText();
                    String archetypeContent = Files.readString(archetypeGitIgnorePath);
                    String mergedContent = mergeGitIgnoreFiles(existingContent, archetypeContent);

                    // Instead of writing directly to file, return modified LST
                    return text.withText(mergedContent);
                }
            } catch (IOException e) {
                LOG.error("Error reading archetype .gitignore file", e);
                throw new RuntimeException("Failed to read archetype .gitignore file", e);
            }

            return text;
        }

        private String mergeGitIgnoreFiles(String existing, String fromArchetype) {
            List<String> existingLines = existing.lines().collect(Collectors.toList());
            List<String> archetypeLines = fromArchetype.lines().collect(Collectors.toList());

            StringBuilder merged = new StringBuilder(existing);
            if (!existing.endsWith("\n")) {
                merged.append("\n");
            }
            merged.append("\n# Added from archetype\n");

            for (String line : archetypeLines) {
                String trimmed = line.trim();
                if (!trimmed.isEmpty() && !trimmed.startsWith("#") && !existingLines.contains(trimmed)) {
                    merged.append(line).append("\n");
                }
            }

            return merged.toString();
        }
    }
}
