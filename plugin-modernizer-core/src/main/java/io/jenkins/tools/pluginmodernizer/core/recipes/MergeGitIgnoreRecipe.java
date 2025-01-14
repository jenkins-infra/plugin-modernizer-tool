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
    private final String gitIgnoreContent;

    public MergeGitIgnoreRecipe(String gitIgnoreContent) {
        this.gitIgnoreContent = gitIgnoreContent;
        LOG.info("updated MergeGitIgnoreRecipe with given .gitignore content.");
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
        return new GitIgnoreMerger(gitIgnoreContent);
    }

    private static class GitIgnoreMerger extends PlainTextVisitor<ExecutionContext> {
        private final String archetypeContent;

        GitIgnoreMerger(String archetypeContent) {
            this.archetypeContent = archetypeContent;
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

            String existingContent = text.getText();
            String mergedContent = mergeGitIgnoreFiles(existingContent, archetypeContent);

            if (!mergedContent.equals(existingContent)) {
                return text.withText(mergedContent);
            }

            return text;
        }

        private String mergeGitIgnoreFiles(String existing, String fromArchetype) {
            List<String> existingLines = existing.lines().map(String::trim)
                .collect(Collectors.toList());

            List<String> archetypeLines = fromArchetype.lines()
                .map(String::trim)
                .filter(line -> !line.isEmpty() && !line.startsWith("#"))
                .collect(Collectors.toList());

            StringBuilder merged = new StringBuilder();

            // Add existing content
            if (!existing.isEmpty()) {
                merged.append(existing);
                if (!existing.endsWith("\n")) {
                    merged.append("\n");
                }
            }

            // Add archetype entries
            boolean hasNewEntries = false;
            for (String line : archetypeLines) {
                if (!existingLines.contains(line)) {
                    if (!hasNewEntries) {
                        merged.append("# Added from archetype\n");
                        hasNewEntries = true;
                    }
                    merged.append(line).append("\n");
                }
            }

            String result = merged.toString();
            if (!result.isEmpty() && result.endsWith("\n")) {
                result = result.substring(0, result.length() - 1);
            }

            return result;
        }
    }
}
