package io.jenkins.tools.pluginmodernizer.core.recipes;

import io.jenkins.tools.pluginmodernizer.core.model.DetachedPlugins;
import java.util.HashSet;
import java.util.Set;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.ScanningRecipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.maven.AddDependency;
import org.openrewrite.maven.MavenIsoVisitor;
import org.openrewrite.xml.tree.Xml;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AddDetachedPluginDependency extends ScanningRecipe<Set<String>> {

    /**
     * Logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(AddDetachedPluginDependency.class);

    /**
     * The jenkins version.
     */
    @Option(displayName = "Version", description = "Jenkins version.", example = "2.440.3")
    String jenkinsVersion;

    /**
     * Constructor.
     * @param jenkinsVersion The Jenkins version.
     */
    public AddDetachedPluginDependency(String jenkinsVersion) {
        this.jenkinsVersion = jenkinsVersion;
    }

    @Override
    public String getDisplayName() {
        return "Add missing detached plugins as dependencies";
    }

    @Override
    public String getDescription() {
        return "Detects usage of detached plugins and automatically adds them to pom.xml.";
    }

    @Override
    public Set<String> getInitialValue(ExecutionContext ctx) {
        return new HashSet<>();
    }

    /**
     * Detect usage of detached plugins.
     */
    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(Set<String> acc) {
        return new JavaIsoVisitor<>() {
            @Override
            public J.Import visitImport(J.Import importStmt, ExecutionContext ctx) {
                String importedClass = importStmt.getTypeName();
                String importedPackage = importedClass.substring(0, importedClass.lastIndexOf('.'));

                LOG.info("Detected import: {}", importedClass);
                for (DetachedPlugins plugin : DetachedPlugins.values()) {
                    if (plugin.getPackageName().contains(importedPackage)
                            || plugin.getClassNames().contains(importedClass)) {
                        // Only add if jenkins version past lastCoreRelease
                        if (new ComparableVersion(jenkinsVersion)
                                        .compareTo(new ComparableVersion(plugin.getLastCoreRelease()))
                                > 0) {
                            LOG.info("Detected usage of detached plugin: {}", plugin.getPluginId());
                            acc.add(plugin.getPluginId());
                        }
                    }
                }
                return super.visitImport(importStmt, ctx);
            }
        };
    }

    /**
     * Add dependencies to pom.xml if they were detected.
     */
    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(Set<String> acc) {
        return new MavenIsoVisitor<>() {
            @Override
            public Xml.Document visitDocument(Xml.Document document, ExecutionContext ctx) {
                for (String pluginId : acc) {
                    DetachedPlugins plugin =
                            DetachedPlugins.valueOf(pluginId.toUpperCase().replace("-", "_"));
                    document = (Xml.Document) new AddDependency(
                                    plugin.getGroupId(),
                                    plugin.getPluginId(),
                                    "RELEASE",
                                    null,
                                    null,
                                    null,
                                    null,
                                    null,
                                    null,
                                    null,
                                    null,
                                    null)
                            .getVisitor()
                            .visitNonNull(document, ctx);
                }
                LOG.info("Document after adding dependency: {}", document.printAll());
                return super.visitDocument(document, ctx);
            }
        };
    }
}
