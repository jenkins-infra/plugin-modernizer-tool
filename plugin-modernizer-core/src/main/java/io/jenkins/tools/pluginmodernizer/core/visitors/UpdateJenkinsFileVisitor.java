package io.jenkins.tools.pluginmodernizer.core.visitors;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.jenkins.tools.pluginmodernizer.core.model.PlatformConfig;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Tree;
import org.openrewrite.groovy.GroovyIsoVisitor;
import org.openrewrite.groovy.tree.G;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JRightPadded;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.Space;
import org.openrewrite.java.tree.TextComment;
import org.openrewrite.marker.Markers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A visitor to update the Jenkinsfile with recommended configuration
 */
@SuppressFBWarnings(value = "VA_FORMAT_STRING_USES_NEWLINE", justification = "Newline is used for formatting")
public class UpdateJenkinsFileVisitor extends GroovyIsoVisitor<ExecutionContext> {

    /**
     * The method comment
     */
    public static final String METHOD_COMMENT =
            """

             See the documentation for more options:
             https://github.com/jenkins-infra/pipeline-library/
            """;

    public static final String CONTAINER_AGENT_COMMENT =
            "Set to `false` if you need to use Docker for containerized tests";
    public static final String FORK_COUNT_COMMENT =
            "run this number of tests in parallel for faster feedback.  If the number terminates with a 'C', the value will be multiplied by the number of available CPU cores";

    /**
     * LOGGER.
     */
    private static final Logger LOG = LoggerFactory.getLogger(UpdateJenkinsFileVisitor.class);

    /**
     * Use container agent flag
     */
    private final Boolean useContainerAgent;

    /**
     * Fork count flag
     */
    private final String forkCount;

    /**
     * List of platform config to insert
     */
    private final List<PlatformConfig> platformConfigs;

    /**
     * Default constructor that create a buildPlugin() without any argument
     */
    public UpdateJenkinsFileVisitor() {
        this.useContainerAgent = true;
        this.forkCount = "1C";
        this.platformConfigs = PlatformConfig.getDefaults();
    }

    /**
     * Constructor that create a buildPlugin() with arguments
     * @param useContainerAgent the useContainerAgent flag
     */
    public UpdateJenkinsFileVisitor(Boolean useContainerAgent, String forkCount, List<PlatformConfig> platformConfigs) {
        this.useContainerAgent = Objects.requireNonNullElse(useContainerAgent, true);
        this.forkCount = Objects.requireNonNullElse(forkCount, "1C");
        this.platformConfigs = platformConfigs;
    }

    @Override
    public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
        method = super.visitMethodInvocation(method, ctx);
        LOG.debug("Visiting method invocation {}", method);
        if (!"buildPlugin".equals(method.getSimpleName())) {
            return method;
        }
        // Add or replace a comment before the method
        TextComment existingComment = method.getComments().isEmpty()
                ? null
                : (TextComment) method.getComments().get(0);
        if (method.getComments().isEmpty() || !existingComment.getText().equals(METHOD_COMMENT)) {
            method = method.withComments(List.of(new TextComment(true, METHOD_COMMENT, "\n", Markers.EMPTY)));
        }

        // Remove legacy arguments
        method = removeLegacyArguments(method);

        List<Expression> arguments = new LinkedList<>(method.getArguments());

        // Remove argument if contains only one element that is J.Empty
        // Since we will never create anymore a buildPlugin() without any argument
        arguments = arguments.stream().filter(arg -> !(arg instanceof J.Empty)).collect(Collectors.toList());

        // Fork count
        G.MapEntry forkCountEntry = buildForkCountEntry();
        if (!hasArgument(method, "forkCount")) {
            // Add argument at the end
            arguments.add(forkCountEntry);
            method = method.withArguments(arguments);
        }

        // Container agent
        G.MapEntry useContainerAgentEntry = buildContainerAgentEntry();
        if (!hasArgument(method, "useContainerAgent")) {
            arguments.add(useContainerAgentEntry);
            method = method.withArguments(arguments);
        }

        if (!hasArgument(method, "configurations")) {
            G.MapEntry configurationsEntry = buildConfigurations();
            arguments.add(configurationsEntry);
            method = method.withArguments(arguments);
        }

        return method;
    }

    /**
     * Check if the method invocation has an argument with the given name
     * @param method the method invocation
     * @param argumentName the argument name
     * @return true if the argument is present
     */
    private boolean hasArgument(J.MethodInvocation method, String argumentName) {
        return method.getArguments().stream()
                .anyMatch(arg -> arg instanceof G.MapEntry
                        && ((G.MapEntry) arg).getKey() instanceof J.Literal
                        && ((J.Literal) ((G.MapEntry) arg).getKey()).getValue().equals(argumentName));
    }

    /**
     * Remove the legacy arguments from the method invocation
     * They are replaced by configurations that are more flexible
     * @param method the method invocation
     */
    private J.MethodInvocation removeLegacyArguments(J.MethodInvocation method) {

        // Remove jdkVersions argument if present
        List<Expression> arguments = method.getArguments().stream()
                .filter(arg -> !(arg instanceof G.MapEntry
                        && ((G.MapEntry) arg).getKey() instanceof J.Literal
                        && ((J.Literal) ((G.MapEntry) arg).getKey()).getValue().equals("jdkVersions")))
                .collect(Collectors.toList());
        method = method.withArguments(arguments);

        // Remove platforms argument if present
        arguments = method.getArguments().stream()
                .filter(arg -> !(arg instanceof G.MapEntry
                        && ((G.MapEntry) arg).getKey() instanceof J.Literal
                        && ((J.Literal) ((G.MapEntry) arg).getKey()).getValue().equals("platforms")))
                .collect(Collectors.toList());
        method = method.withArguments(arguments);

        // Remove jenkinsVersions argument if present
        arguments = method.getArguments().stream()
                .filter(arg -> !(arg instanceof G.MapEntry
                        && ((G.MapEntry) arg).getKey() instanceof J.Literal
                        && ((J.Literal) ((G.MapEntry) arg).getKey()).getValue().equals("jenkinsVersions")))
                .collect(Collectors.toList());
        method = method.withArguments(arguments);

        return method;
    }

    /**
     * Build the forkCount argument as a G.MapEntry
     * @return the G.MapEntry
     */
    private G.MapEntry buildForkCountEntry() {
        J.Literal keyLiteral =
                new J.Literal(Tree.randomId(), Space.EMPTY, Markers.EMPTY, "forkCount", "forkCount", null, null);

        J.Literal valueLiteral = new J.Literal(
                Tree.randomId(),
                Space.format(" "),
                Markers.EMPTY,
                forkCount,
                "'" + forkCount + "'",
                null,
                JavaType.Primitive.String);

        // Prefix with newline and indentation
        return new G.MapEntry(
                Tree.randomId(),
                Space.format("\n  "),
                Markers.EMPTY,
                JRightPadded.build(keyLiteral),
                valueLiteral,
                null);
    }

    /**
     * Build the useContainerAgent argument as a G.MapEntry
     * @return the G.MapEntry
     */
    private G.MapEntry buildContainerAgentEntry() {
        J.Literal keyLiteral = new J.Literal(
                Tree.randomId(), Space.EMPTY, Markers.EMPTY, "useContainerAgent", "useContainerAgent", null, null);

        J.Literal valueLiteral = new J.Literal(
                Tree.randomId(),
                Space.format(" "),
                Markers.EMPTY,
                useContainerAgent,
                useContainerAgent.toString(),
                null,
                JavaType.Primitive.Boolean);

        // Prefix with newline and indentation
        return new G.MapEntry(
                Tree.randomId(),
                Space.build(" ", List.of(new TextComment(false, " " + FORK_COUNT_COMMENT, "\n  ", Markers.EMPTY))),
                Markers.EMPTY,
                JRightPadded.build(keyLiteral),
                valueLiteral,
                null);
    }

    /**
     * Build an empty configurations argument as a G.MapEntry
     * @return the G.MapEntry
     */
    private G.MapEntry buildConfigurations() {
        J.Literal keyLiteral = new J.Literal(
                Tree.randomId(), Space.EMPTY, Markers.EMPTY, "configurations", "configurations", null, null);

        // New configuration is an empty map
        J.Literal valueLiteral;
        if (platformConfigs.isEmpty()) {
            valueLiteral = new J.Literal(
                    Tree.randomId(),
                    Space.format(" "),
                    Markers.EMPTY,
                    new Object[0],
                    "[]\n",
                    null,
                    JavaType.Primitive.String);
        } else {

            String valueSource = platformConfigs.stream()
                    .map(platformConfig -> {
                        String platform = platformConfig.name().toString().toLowerCase();
                        int major = platformConfig.jdk().getMajor();
                        String jenkinsVersion = platformConfig.jenkins();
                        if (jenkinsVersion == null) {
                            return String.format("\n    [platform: '%s', jdk: %s]", platform, major);
                        } else {
                            return String.format(
                                    "\n    [platform: '%s', jdk: %s, jenkins: '%s']", platform, major, jenkinsVersion);
                        }
                    })
                    .collect(Collectors.joining(","));

            valueLiteral = new J.Literal(
                    Tree.randomId(),
                    Space.format(" "),
                    Markers.EMPTY,
                    new Object[0],
                    "[%s,\n]".formatted(valueSource), // Keep last comma and return to next line
                    null,
                    JavaType.Primitive.String);
        }

        // Prefix with newline and indentation
        return new G.MapEntry(
                Tree.randomId(),
                Space.build(" ", List.of(new TextComment(false, " " + CONTAINER_AGENT_COMMENT, "\n  ", Markers.EMPTY))),
                Markers.EMPTY,
                JRightPadded.build(keyLiteral),
                valueLiteral,
                null);
    }
}
