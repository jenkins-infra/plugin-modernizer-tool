package io.jenkins.tools.pluginmodernizer.core.github;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Utilities for working with git submodules in plugin repositories.
 */
final class GitSubmoduleUtils {

    private GitSubmoduleUtils() {}

    /**
     * Read submodule paths declared in {@code .gitmodules}.
     *
     * @param repositoryRoot The root of the git repository
     * @return Submodule paths, or an empty set if none are declared
     */
    static Set<String> readSubmodulePaths(Path repositoryRoot) throws IOException {
        Path gitmodules = repositoryRoot.resolve(".gitmodules");
        if (!Files.exists(gitmodules)) {
            return Set.of();
        }
        Set<String> paths = new HashSet<>();
        for (String line : Files.readAllLines(gitmodules)) {
            String trimmed = line.trim();
            if (trimmed.startsWith("path")) {
                int separator = trimmed.indexOf('=');
                if (separator > 0 && separator < trimmed.length() - 1) {
                    paths.add(trimmed.substring(separator + 1).trim());
                }
            }
        }
        return paths;
    }

    /**
     * Exclude declared submodule paths from a set of repository paths.
     */
    static Set<String> excludeSubmodulePaths(Set<String> paths, Set<String> submodulePaths) {
        if (submodulePaths.isEmpty()) {
            return paths;
        }
        return paths.stream().filter(path -> !submodulePaths.contains(path)).collect(Collectors.toSet());
    }
}
