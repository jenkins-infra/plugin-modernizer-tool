package io.jenkins.tools.pluginmodernizer.core.recipes;

import io.jenkins.tools.pluginmodernizer.core.model.JDK;
import io.jenkins.tools.pluginmodernizer.core.model.Platform;
import io.jenkins.tools.pluginmodernizer.core.model.PlatformConfig;
import io.jenkins.tools.pluginmodernizer.core.visitors.UpdateJenkinsFileVisitor;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.groovy.GroovyVisitor;
import org.openrewrite.groovy.tree.G;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UpdateJenkinsfileForJavaVersion extends Recipe {

    private static final Logger LOG = LoggerFactory.getLogger(UpdateJenkinsfileForJavaVersion.class);

    /**
     * The java version.
     */
    @Option(displayName = "Version", description = "Java version.", example = "25")
    Integer javaVersion;

    /**
     * Constructor.
     * @param javaVersion The java version.
     */
    public UpdateJenkinsfileForJavaVersion(Integer javaVersion) {
        this.javaVersion = javaVersion;
    }

    @Override
    public String getDisplayName() {
        return "Update Jenkinsfile for specefied Java Version";
    }

    @Override
    public String getDescription() {
        return "Adds Java version to the buildPlugin configurations in Jenkinsfile for runtime testing on ci.jenkins.io.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new GroovyVisitor<>() {

            @Override
            public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                if (!method.getSimpleName().equals("buildPlugin")) {
                    return method;
                }

                JDK jdkVersion = JDK.get(javaVersion);
                if (jdkVersion == null) {
                    LOG.warn("Unsupported Java version: {}, probably not an LTS. No update will be made.", javaVersion);
                    return method;
                }

                // extract all existing arguments into a simple data model.
                JenkinsfileModel model = new JenkinsfileModel(method);

                // see if java version is already present
                boolean hasJavaVersion = model.platformConfigs.stream()
                        .anyMatch(p -> "linux".equalsIgnoreCase(p.name().toString())
                                && p.jdk().getMajor() == javaVersion);

                if (hasJavaVersion) {
                    LOG.info("Java {} configuration already exists. No update needed.", javaVersion);
                    return method;
                }

                // add the java version configuration to the model.
                model.platformConfigs.add(PlatformConfig.build(Platform.LINUX, jdkVersion));

                // We pass it the complete, updated configuration, and it will overwrite the old method call.
                // in future at some point remove jdk 17 from the configurations
                doAfterVisit(
                        new UpdateJenkinsFileVisitor(model.useContainerAgent, model.forkCount, model.platformConfigs));

                return method;
            }
        };
    }

    /**
     * A simple data model to hold the state of the Jenkinsfile's buildPlugin method.
     */
    private static class JenkinsfileModel {
        List<PlatformConfig> platformConfigs = new ArrayList<>();
        Boolean useContainerAgent = null;
        String forkCount = null;

        JenkinsfileModel(J.MethodInvocation method) {
            // Temporary lists to hold legacy format values
            List<String> platforms = new ArrayList<>();
            List<Integer> jdks = new ArrayList<>();

            for (Expression arg : method.getArguments()) {
                if (arg instanceof G.MapEntry) {
                    G.MapEntry entry = (G.MapEntry) arg;
                    String key = "";
                    if (entry.getKey() instanceof J.Identifier) {
                        key = ((J.Identifier) entry.getKey()).getSimpleName();
                    } else if (entry.getKey() instanceof J.Literal) {
                        Object keyValue = ((J.Literal) entry.getKey()).getValue();
                        if (keyValue != null) {
                            key = keyValue.toString();
                        }
                    }
                    switch (key) {
                        case "configurations":
                            if (entry.getValue() instanceof G.ListLiteral) {
                                this.platformConfigs = extractPlatformConfigs((G.ListLiteral) entry.getValue());
                            }
                            break;
                        case "platforms": // Legacy format
                            if (entry.getValue() instanceof G.ListLiteral) {
                                platforms = extractStringList((G.ListLiteral) entry.getValue());
                            }
                            break;
                        case "jdkVersions": // Legacy format
                            if (entry.getValue() instanceof G.ListLiteral) {
                                jdks = extractIntegerList((G.ListLiteral) entry.getValue());
                            }
                            break;
                        case "useContainerAgent":
                            if (entry.getValue() instanceof J.Literal) {
                                Object value = ((J.Literal) entry.getValue()).getValue();
                                if (value instanceof Boolean) {
                                    this.useContainerAgent = (Boolean) value;
                                }
                            }
                            break;
                        case "forkCount":
                            if (entry.getValue() instanceof J.Literal) {
                                assert ((J.Literal) entry.getValue()).getValue() != null;
                                this.forkCount = ((J.Literal) entry.getValue())
                                        .getValue()
                                        .toString();
                            }
                            break;
                        default:
                            LOG.warn("Unknown argument in buildPlugin: {}", key);
                            break;
                    }
                }
            }
            // If the modern 'configurations' block was not found, but legacy lists were,
            // build the configurations from the Cartesian product.
            if (this.platformConfigs.isEmpty() && !platforms.isEmpty() && !jdks.isEmpty()) {
                for (String platform : platforms) {
                    for (Integer jdk : jdks) {
                        this.platformConfigs.add(PlatformConfig.build(Platform.fromPlatform(platform), JDK.get(jdk)));
                    }
                }
            }
        }

        /**
         * Extracts platform configurations
         * @param list The G.ListLiteral containing platform configurations.
         * @return A list of PlatformConfig objects.
         */
        private List<PlatformConfig> extractPlatformConfigs(G.ListLiteral list) {
            List<PlatformConfig> configs = new ArrayList<>();
            for (Expression configExpr : list.getElements()) {
                if (configExpr instanceof G.MapLiteral) {
                    G.MapLiteral configMap = (G.MapLiteral) configExpr;
                    String platform = null;
                    Integer jdk = null;
                    for (G.MapEntry entry : configMap.getElements()) {
                        String key = "";
                        if (entry.getKey() instanceof J.Identifier) {
                            key = ((J.Identifier) entry.getKey()).getSimpleName();
                        } else if (entry.getKey() instanceof J.Literal) {
                            Object keyValue = ((J.Literal) entry.getKey()).getValue();
                            if (keyValue != null) {
                                key = keyValue.toString();
                            }
                        }
                        if ("platform".equals(key) && entry.getValue() instanceof J.Literal) {
                            assert ((J.Literal) entry.getValue()).getValue() != null;
                            platform = ((J.Literal) entry.getValue()).getValue().toString();
                        } else if ("jdk".equals(key) && entry.getValue() instanceof J.Literal) {
                            Object jdkValue = ((J.Literal) entry.getValue()).getValue();
                            if (jdkValue instanceof Number) {
                                jdk = ((Number) jdkValue).intValue();
                            }
                        }
                    }
                    if (platform != null && jdk != null) {
                        configs.add(PlatformConfig.build(Platform.fromPlatform(platform), JDK.get(jdk)));
                    }
                }
            }
            return configs;
        }

        /**
         * Extracts a list of strings from a G.ListLiteral.
         * @param list The G.ListLiteral to extract from.
         * @return A list of strings.
         */
        private List<String> extractStringList(G.ListLiteral list) {
            return list.getElements().stream()
                    .filter(J.Literal.class::isInstance)
                    .map(J.Literal.class::cast)
                    .map(l -> l.getValue().toString())
                    .collect(Collectors.toList());
        }

        /**
         * Extracts a list of integers from a G.ListLiteral.
         * @param list The G.ListLiteral to extract from.
         * @return A list of integers.
         */
        private List<Integer> extractIntegerList(G.ListLiteral list) {
            return list.getElements().stream()
                    .filter(J.Literal.class::isInstance)
                    .map(J.Literal.class::cast)
                    .map(l -> {
                        try {
                            return Integer.parseInt(l.getValue().toString());
                        } catch (NumberFormatException e) {
                            return null;
                        }
                    })
                    .filter(java.util.Objects::nonNull)
                    .collect(Collectors.toList());
        }
    }
}
