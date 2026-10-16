package io.jenkins.tools.pluginmodernizer.core.github;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class GitSubmoduleUtilsTest {

    @TempDir
    Path repositoryRoot;

    @Test
    void readSubmodulePathsReturnsEmptyWhenGitmodulesMissing() throws Exception {
        assertTrue(GitSubmoduleUtils.readSubmodulePaths(repositoryRoot).isEmpty());
    }

    @Test
    void readSubmodulePathsParsesDeclaredPaths() throws Exception {
        Files.writeString(repositoryRoot.resolve(".gitmodules"), """
                [submodule "docdock"]
                    path = hugo/themes/docdock
                    url = https://github.com/example/docdock.git
                """);

        assertEquals(Set.of("hugo/themes/docdock"), GitSubmoduleUtils.readSubmodulePaths(repositoryRoot));
    }

    @Test
    void excludeSubmodulePathsFiltersMissingSubmoduleEntries() {
        Set<String> missing = Set.of("pom.xml", "hugo/themes/docdock", "README.md");
        Set<String> submodules = Set.of("hugo/themes/docdock");

        assertEquals(Set.of("pom.xml", "README.md"), GitSubmoduleUtils.excludeSubmodulePaths(missing, submodules));
    }
}
