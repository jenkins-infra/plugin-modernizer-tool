package io.jenkins.tools.pluginmodernizer.core.extractor;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Set;

/**
 * An archetype repository file with location
 * Used to create metadata and store information about file presence or changes
 */
public enum ArchetypeCommonFile {

    /**
     * The Jenkinsfile in the root repository
     */
    JENKINSFILE("Jenkinsfile"),

    /**
     * The pom.xml file in the root repository
     */
    POM("pom.xml"),

    /**
     * The workflow CD file
     */
    WORKFLOW_CD(".github/workflows/cd.yml", ".github/workflows/cd.yaml"),

    /**
     * The workflow Jenkins security scan
     */
    WORKFLOW_SECURITY(".github/workflows/jenkins-security-scan.yml", ".github/workflows/jenkins-security-scan.yaml"),

    /**
     * Release drafter file
     */
    RELEASE_DRAFTER(".github/release-drafter.yml", ".github/release-drafter.yaml"),

    /**
     * Release workflows
     */
    RELEASE_DRAFTER_WORKFLOW(".github/workflows/release-drafter.yml", ".github/workflows/release-drafter.yaml"),

    /**
     * Pull request template file
     */
    PULL_REQUEST_TEMPLATE(".github/PULL_REQUEST_TEMPLATE.md"),

    /**
     * Codeowners file
     */
    CODEOWNERS(".github/CODEOWNERS"),

    /**
     * Index jelly file
     */
    INDEX_JELLY("src/main/resources/index.jelly"),

    /**
     * .gitignore file
     */
    GITIGNORE(".gitignore"),

    /**
     * License file
     */
    LICENSE("LICENSE.md", "LICENSE.adoc", "LICENSE.txt", "LICENSE"),

    /**
     * Contributing file
     */
    CONTRIBUTING("CONTRIBUTING.md"),

    /**
     * Dependabot configuration file
     */
    DEPENDABOT(".github/dependabot.yml", ".github/dependabot.yaml"),

    /**
     * Renovate configuration file.
     * Not in archetype but to skip plugins using a different bot for updates
     */
    RENOVATE("renovate.json"),

    /**
     * Maven extensions file
     */
    MAVEN_EXTENSIONS(".mvn/extensions.xml"),

    /**
     * Maven configuration file
     */
    MAVEN_CONFIG(".mvn/maven.config"),

    /**
     * README
     */
    README("README.md", "README.adoc"),
    ;

    /**
     * Relative path
     */
    private final String[] paths;

    /**
     * Private constructor for one single path
     * @param value the path
     */
    ArchetypeCommonFile(String value) {
        this.paths = new String[] {value};
    }

    /**
     * Private constructor for multiple path
     * @param paths the paths
     */
    ArchetypeCommonFile(String... paths) {
        this.paths = paths;
    }

    /**
     * Return the enum from a file path or null if not found
     * @param path path of the file
     * @return the enum or null
     */
    public static ArchetypeCommonFile fromPath(Path path) {
        for (ArchetypeCommonFile f : ArchetypeCommonFile.values()) {
            if (f.getPaths().contains(path)) {
                return f;
            }
        }
        return null;
    }

    /**
     * Get the path default path
     * @return the path
     */
    public Path getPath() {
        return Path.of(paths[0]);
    }

    /**
     * Get the paths
     * @return the paths
     */
    public Set<Path> getPaths() {
        return Set.copyOf(Arrays.stream(paths).map(Path::of).toList());
    }

    /**
     * Return if the common file is the same as the file
     * @param path the path
     * @return true if the file is the same
     */
    public boolean same(Path path) {
        return getPaths().contains(path);
    }
}
