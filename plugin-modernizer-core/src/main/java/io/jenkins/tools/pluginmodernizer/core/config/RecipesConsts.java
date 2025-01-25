package io.jenkins.tools.pluginmodernizer.core.config;

/**
 * Constants for the recipes.
 */
public final class RecipesConsts {

    /**
     * Utility class.
     */
    private RecipesConsts() {
        // Utility class
    }

    public static final String PLUGIN_POM_GROUP_ID = "org.jenkins-ci.plugins";
    public static final String PLUGINS_BOM_GROUP_ID = "io.jenkins.tools.bom";
    public static final String ANALYSIS_POM_ARTIFACT_ID = "analysis-pom";
    public static final String VERSION_METADATA_PATTERN = "\\.v[a-f0-9_]+";
    public static final String OLD_BOM_VERSION_PATTERN =
            ""; // Old release line like 2.150.x, no need to filter by pattern
    public static final String INCREMENTAL_REPO_ID = "incrementals";
}
