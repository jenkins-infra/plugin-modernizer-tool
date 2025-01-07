package io.jenkins.tools.pluginmodernizer.core.extractor;

import io.jenkins.tools.pluginmodernizer.core.model.JDK;
import io.jenkins.tools.pluginmodernizer.core.model.Platform;
import io.jenkins.tools.pluginmodernizer.core.model.PlatformConfig;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.openrewrite.groovy.GroovyIsoVisitor;
import org.openrewrite.groovy.tree.G;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Visitor for a Jenkinsfile to accumulate @see PluginMetadata.
 */
public class JenkinsfileVisitor extends GroovyIsoVisitor<PluginMetadata> {

    /**
     * LOGGER.
     */
    public static final Logger LOG = LoggerFactory.getLogger(JenkinsfileVisitor.class);

    /**
     * Keep track of variables if there are defined on top level and not directly on the method invocation.
     */
    private final Map<String, Object> variableMap = new HashMap<>();

    @Override
    public J.VariableDeclarations visitVariableDeclarations(J.VariableDeclarations v, PluginMetadata pluginMetadata) {
        LOG.debug("Visiting variable declarations {}", v);
        J.VariableDeclarations variableDeclarations = super.visitVariableDeclarations(v, pluginMetadata);

        for (J.VariableDeclarations.NamedVariable variable : variableDeclarations.getVariables()) {
            variableMap.put(variable.getSimpleName(), variable.getInitializer());
        }

        return variableDeclarations;
    }

    @Override
    public J visitMapEntry(G.MapEntry mapEntry, PluginMetadata pluginMetadata) {
        super.visitMapEntry(mapEntry, pluginMetadata);
        J.MethodInvocation method = getCursor().firstEnclosing(J.MethodInvocation.class);
        if (method == null) {
            return mapEntry;
        }
        if (method.getSimpleName().equals("buildPlugin")) {
            if ("useContainerAgent".equals(mapEntry.getKey().toString())) {
                if (mapEntry.getValue() instanceof J.Literal literal) {
                    pluginMetadata.setUseContainerAgent(Boolean.valueOf(literal.getValueSource()));
                } else if (mapEntry.getValue() instanceof J.Identifier) {
                    Expression resolvedArg = resolveIdentifier(mapEntry.getValue());
                    if (resolvedArg instanceof J.Literal literal) {
                        pluginMetadata.setUseContainerAgent(Boolean.valueOf(literal.getValueSource()));
                    }
                }
            }
            if ("forkCount".equals(mapEntry.getKey().toString())) {
                if (mapEntry.getValue() instanceof J.Literal literal) {
                    pluginMetadata.setForkCount(literal.getValue().toString());
                } else if (mapEntry.getValue() instanceof J.Identifier) {
                    Expression resolvedArg = resolveIdentifier(mapEntry.getValue());
                    if (resolvedArg instanceof J.Literal literal) {
                        pluginMetadata.setForkCount(literal.getValue().toString());
                    }
                }
            }
        }
        return mapEntry;
    }

    @Override
    public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, PluginMetadata pluginMetadata) {
        LOG.debug("Visiting method invocation {}", method);
        method = super.visitMethodInvocation(method, pluginMetadata);
        if ("buildPlugin".equals(method.getSimpleName())) {
            List<Expression> args = method.getArguments();

            // Empty args means Java 8 in Windows and Linux
            if (args.size() == 1 && args.get(0) instanceof J.Empty) {
                pluginMetadata.addPlatform(new PlatformConfig(Platform.LINUX, JDK.getImplicit(), null, true));
                pluginMetadata.addPlatform(new PlatformConfig(Platform.WINDOWS, JDK.getImplicit(), null, true));
                return method;
            }

            List<PlatformConfig> platforms = new LinkedList<>();
            for (Expression expression : args) {
                platforms.addAll(extractPlatforms(expression, pluginMetadata));
            }
            // Filter platforms (Remove implicit if there is at least one explicit)
            if (platforms.stream().anyMatch(pc -> !pc.implicit())) {
                platforms.removeIf(PlatformConfig::implicit);
            }
            // If all platform are unknown ensure each platform on the list is present with LINUX and WINDOWS
            if (platforms.stream().allMatch(pc -> pc.name() == Platform.UNKNOWN)) {
                List<PlatformConfig> newPlatforms = new LinkedList<>();
                for (PlatformConfig pc : platforms) {
                    newPlatforms.add(new PlatformConfig(Platform.LINUX, pc.jdk(), null, false));
                    newPlatforms.add(new PlatformConfig(Platform.WINDOWS, pc.jdk(), null, false));
                }
                platforms = newPlatforms;
            }
            // If there is a platform without JDK ensure all JDK are using the same platform
            if (platforms.stream().anyMatch(pc -> pc.jdk() == null)) {
                List<PlatformConfig> newPlatforms = new LinkedList<>();
                for (PlatformConfig pc : platforms) {
                    if (!pc.name().equals(Platform.UNKNOWN)) {
                        if (platforms.stream().allMatch(p -> p.jdk() == null)) {
                            newPlatforms.add(new PlatformConfig(pc.name(), JDK.getImplicit(), null, false));
                            continue;
                        }
                        for (PlatformConfig pc2 : platforms) {
                            if (pc2.jdk() != null) {
                                newPlatforms.add(new PlatformConfig(pc.name(), pc2.jdk(), null, false));
                            }
                        }
                    }
                }
                platforms = newPlatforms;
            }

            LOG.info("Platform: {}", platforms);
            pluginMetadata.setPlatforms(platforms);
        }

        return method;
    }

    private List<PlatformConfig> extractPlatforms(Expression arg, PluginMetadata pluginMetadata) {
        if (arg instanceof G.MapEntry) {

            // Get 'configurations' stream
            Stream<G.MapEntry> configurations = Stream.of(arg)
                    .map(G.MapEntry.class::cast)
                    .filter(entry -> "configurations".equals(((J.Literal) entry.getKey()).getValue()));

            // List of platform config from 'configurations' parameter
            List<PlatformConfig> platformFromConfigs = configurations
                    .map(entry -> resolveIdentifier(entry.getValue()))
                    .filter(value -> value instanceof G.ListLiteral)
                    .flatMap(value -> ((G.ListLiteral) value).getElements().stream())
                    .filter(expression -> expression instanceof G.MapLiteral)
                    .map(expression -> (G.MapLiteral) expression)
                    .map(JenkinsfileVisitor::toPlatformEntry)
                    .toList();

            // Cannot be combinated
            if (!platformFromConfigs.isEmpty()) {
                return platformFromConfigs;
            }

            // List of JDK versions from 'jdkVersions' parameter
            List<Integer> jdkVersions = Stream.of(arg)
                    .map(G.MapEntry.class::cast)
                    .filter(entry -> "jdkVersions".equals(((J.Literal) entry.getKey()).getValue()))
                    .map(entry -> resolveIdentifier(entry.getValue()))
                    .filter(value -> value instanceof G.ListLiteral)
                    .map(value -> (G.ListLiteral) value)
                    .flatMap(value -> value.getElements().stream())
                    .map(expression ->
                            Integer.parseInt(((J.Literal) expression).getValue().toString()))
                    .toList();

            // List of platforms from 'platforms' parameter
            List<String> platforms = Stream.of(arg)
                    .map(G.MapEntry.class::cast)
                    .filter(entry -> "platforms".equals(((J.Literal) entry.getKey()).getValue()))
                    .map(entry -> resolveIdentifier(entry.getValue()))
                    .filter(value -> value instanceof G.ListLiteral)
                    .map(value -> (G.ListLiteral) value)
                    .flatMap(value -> value.getElements().stream())
                    .map(expression -> expression instanceof J.Literal ? expression.toString() : null)
                    .toList();

            // Combine all JDK versions with all platforms
            List<PlatformConfig> platformFromJdkVersions = new LinkedList<>();
            for (Integer jdk : jdkVersions) {
                platformFromJdkVersions.add(new PlatformConfig(Platform.UNKNOWN, JDK.get(jdk), null, true));
            }
            for (String platform : platforms) {
                platformFromJdkVersions.add(new PlatformConfig(Platform.fromPlatform(platform), null, null, true));
            }

            return platformFromJdkVersions;

        } else if (arg instanceof J.Identifier) {
            Expression resolvedArg = resolveIdentifier(arg);

            Stream<G.MapEntry> configurations = Stream.of(resolvedArg)
                    .filter(resolved -> resolved instanceof G.MapLiteral)
                    .flatMap(resolved -> ((G.MapLiteral) resolved).getElements().stream())
                    .filter(entry -> "configurations".equals(((J.Literal) entry.getKey()).getValue()));

            List<PlatformConfig> platformFromConfigs = configurations
                    .map(entry -> resolveIdentifier(entry.getValue()))
                    .filter(value -> value instanceof G.ListLiteral)
                    .flatMap(value -> ((G.ListLiteral) value).getElements().stream())
                    .filter(expression -> expression instanceof G.MapLiteral)
                    .map(expression -> (G.MapLiteral) expression)
                    .map(JenkinsfileVisitor::toPlatformEntry)
                    .toList();

            // Get useContainerAgent
            Boolean useContainerAgent = Stream.of(resolvedArg)
                    .filter(resolved -> resolved instanceof G.MapLiteral)
                    .flatMap(resolved -> ((G.MapLiteral) resolved).getElements().stream())
                    .filter(entry -> "useContainerAgent".equals(((J.Literal) entry.getKey()).getValue()))
                    .map(entry -> resolveIdentifier(entry.getValue()))
                    .filter(value -> value instanceof J.Literal)
                    .map(value -> Boolean.valueOf(((J.Literal) value).getValue().toString()))
                    .findFirst()
                    .orElse(null);
            if (useContainerAgent != null) {
                pluginMetadata.setUseContainerAgent(useContainerAgent);
            }
            String forkCount = Stream.of(resolvedArg)
                    .filter(resolved -> resolved instanceof G.MapLiteral)
                    .flatMap(resolved -> ((G.MapLiteral) resolved).getElements().stream())
                    .filter(entry -> "forkCount".equals(((J.Literal) entry.getKey()).getValue()))
                    .map(entry -> resolveIdentifier(entry.getValue()))
                    .filter(value -> value instanceof J.Literal)
                    .map(value -> ((J.Literal) value).getValue().toString())
                    .findFirst()
                    .orElse(null);
            if (forkCount != null) {
                pluginMetadata.setForkCount(forkCount);
            }

            // Cannot be combinated
            if (!platformFromConfigs.isEmpty()) {
                return platformFromConfigs;
            }

            // List of JDK versions from 'jdkVersions' parameter
            List<Integer> jdkVersions = Stream.of(resolvedArg)
                    .filter(resolved -> resolved instanceof G.MapLiteral)
                    .flatMap(resolved -> ((G.MapLiteral) resolved).getElements().stream())
                    .filter(entry -> "jdkVersions".equals(((J.Literal) entry.getKey()).getValue()))
                    .map(entry -> resolveIdentifier(entry.getValue()))
                    .filter(value -> value instanceof G.ListLiteral)
                    .map(value -> (G.ListLiteral) value)
                    .flatMap(value -> value.getElements().stream())
                    .map(expression ->
                            Integer.parseInt(((J.Literal) expression).getValue().toString()))
                    .toList();

            // List of platforms from 'platforms' parameter
            List<String> platforms = Stream.of(resolvedArg)
                    .filter(resolved -> resolved instanceof G.MapLiteral)
                    .flatMap(resolved -> ((G.MapLiteral) resolved).getElements().stream())
                    .filter(entry -> "platforms".equals(((J.Literal) entry.getKey()).getValue()))
                    .map(entry -> resolveIdentifier(entry.getValue()))
                    .filter(value -> value instanceof G.ListLiteral)
                    .map(value -> (G.ListLiteral) value)
                    .flatMap(value -> value.getElements().stream())
                    .map(expression -> expression instanceof J.Literal ? expression.toString() : null)
                    .toList();

            // Combine all JDK versions with all platforms
            List<PlatformConfig> platformFromJdkVersions = new LinkedList<>();
            for (Integer jdk : jdkVersions) {
                platformFromJdkVersions.add(new PlatformConfig(Platform.UNKNOWN, JDK.get(jdk), null, true));
            }
            for (String platform : platforms) {
                platformFromJdkVersions.add(new PlatformConfig(Platform.fromPlatform(platform), null, null, true));
            }

            return platformFromJdkVersions;
        }
        return Collections.emptyList();
    }

    /**
     * Return if the map entry is a platform entry.
     * @param mapLiteral The map entry
     * @return The platform config
     */
    private static PlatformConfig toPlatformEntry(G.MapLiteral mapLiteral) {
        Stream<G.MapEntry> entries = mapLiteral.getElements().stream();
        List<G.MapEntry> list = entries.toList();
        Integer jdk = null;
        String platform = null;
        for (G.MapEntry entry : list) {
            if (entry.getKey() instanceof J.Literal key) {
                if ("jdk".equals(key.getValue())) {
                    jdk = Integer.parseInt(entry.getValue().toString());
                }
                if ("platform".equals(key.getValue())) {
                    platform = entry.getValue().toString();
                }
            }
        }

        return new PlatformConfig(Platform.fromPlatform(platform), JDK.get(jdk), null, false);
    }

    /**
     * Resolve an identifier to its value. Return it's value if it's a litteral already
     * @param expression The expression
     * @return The resolved expression
     */
    private Expression resolveIdentifier(Expression expression) {
        if (expression instanceof G.ListLiteral) {
            return expression;
        }
        if (expression instanceof J.Literal) {
            return expression;
        }
        String variableName = ((J.Identifier) expression).getSimpleName();
        if (variableMap.containsKey(variableName)) {
            return (Expression) variableMap.get(variableName);
        }
        return expression;
    }
}
