package io.jenkins.tools.pluginmodernizer.core.recipes;

import static java.util.Collections.emptyList;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;

import java.nio.file.Paths;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jspecify.annotations.Nullable;
import org.openrewrite.Cursor;
import org.openrewrite.ExecutionContext;
import org.openrewrite.NlsRewrite;
import org.openrewrite.Option;
import org.openrewrite.Preconditions;
import org.openrewrite.ScanningRecipe;
import org.openrewrite.SourceFile;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.text.PlainText;
import org.openrewrite.text.PlainTextParser;
import org.openrewrite.text.PlainTextVisitor;

/**
 * Adds a CODEOWNERS file using the update center plugin artifact ID.
 * <p>
 * Unlike {@link org.openrewrite.jenkins.github.AddTeamToCodeowners}, this recipe does not rely on scanning
 * pom.xml files (which may resolve to a parent module in multi-module projects) and does not skip plugins
 * listed in rewrite-jenkins {@code teams.properties}.
 */
public class AddCodeOwner extends ScanningRecipe<AddCodeOwner.Scanned> {

    private static final String FILE_PATH = ".github/CODEOWNERS";

    @Option(
            displayName = "Plugin artifact ID",
            description = "Update center plugin artifact ID used to derive the developer team name.",
            example = "openstack-cloud")
    String artifactId;

    public AddCodeOwner() {}

    public AddCodeOwner(String artifactId) {
        this.artifactId = artifactId;
    }

    @Override
    public @NlsRewrite.DisplayName String getDisplayName() {
        return "Add plugin developer team to CODEOWNERS";
    }

    @Override
    public @NlsRewrite.Description String getDescription() {
        return "Adds the `{artifactId}-plugin-developers` team to `.github/CODEOWNERS` if absent.";
    }

    @Override
    public Scanned getInitialValue(ExecutionContext ctx) {
        return new Scanned(artifactId != null ? artifactId : "");
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(Scanned acc) {
        return new TreeVisitor<>() {
            @Override
            public Tree visit(@Nullable Tree tree, ExecutionContext ctx, Cursor parent) {
                SourceFile sourceFile = (SourceFile) requireNonNull(tree);
                if ("CODEOWNERS".equals(sourceFile.getSourcePath().getFileName().toString())) {
                    acc.foundFile = true;
                }
                return sourceFile;
            }
        };
    }

    @Override
    public Collection<SourceFile> generate(Scanned acc, ExecutionContext ctx) {
        if (acc.foundFile || !acc.hasValidTeamName()) {
            return emptyList();
        }
        PlainTextParser parser = new PlainTextParser();
        String line = "* " + acc.teamName() + "\n";
        return parser.parse(line)
                .map(brandNewFile -> (PlainText) brandNewFile.withSourcePath(Paths.get(FILE_PATH)))
                .collect(toList());
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(Scanned acc) {
        return Preconditions.check(acc.hasValidTeamName(), new PlainTextVisitor<>() {
            @Override
            public PlainText visitText(PlainText plainText, ExecutionContext ctx) {
                if (!Paths.get(FILE_PATH).equals(plainText.getSourcePath())) {
                    return plainText;
                }
                String text = plainText.getText();
                if (acc.presentIn(text)) {
                    return plainText;
                }
                boolean endsWithNewLine = text.endsWith("\n");
                List<String> lines = new LinkedList<>();
                List<String> after = new LinkedList<>();
                try (Scanner scanner = new Scanner(text)) {
                    int atPos = 0;
                    boolean lastComment = true;
                    while (scanner.hasNextLine()) {
                        String line = scanner.nextLine();
                        if (atPos == 0 && line.contains("@")) {
                            atPos = line.indexOf("@");
                        }
                        if (lastComment && line.startsWith("#")) {
                            lines.add(line);
                        } else {
                            lastComment = false;
                            after.add(line);
                        }
                    }
                    int spaces = Math.max(1, atPos - 1);
                    lines.add("*" + StringUtils.repeat(" ", spaces) + acc.teamName());
                    lines.addAll(after);
                    String updated = String.join("\n", lines);
                    if (endsWithNewLine) {
                        updated += "\n";
                    }
                    return plainText.withText(updated);
                }
            }
        });
    }

    static String teamNameForArtifactId(String artifactId) {
        if (artifactId == null || artifactId.isBlank()) {
            return "";
        }
        String base = artifactId;
        if (base.endsWith("-parent") || base.endsWith("-plugin")) {
            base = base.substring(0, base.lastIndexOf('-'));
        }
        return ("@jenkinsci/" + base + "-plugin-developers").toLowerCase(Locale.ROOT);
    }

    public static class Scanned {
        private final String artifactId;
        boolean foundFile;

        public Scanned(String artifactId) {
            this.artifactId = artifactId;
        }

        boolean presentIn(String text) {
            Pattern p = Pattern.compile("^\\*\\s+" + teamName() + "\\s*$");
            try (Scanner s = new Scanner(text)) {
                while (s.hasNextLine()) {
                    String line = s.nextLine();
                    Matcher matcher = p.matcher(line);
                    if (matcher.matches()) {
                        return true;
                    }
                }
                return false;
            }
        }

        String teamName() {
            return teamNameForArtifactId(artifactId);
        }

        boolean hasValidTeamName() {
            return !teamName().isBlank();
        }
    }
}
