package io.jenkins.tools.pluginmodernizer.core.recipes.code;

import org.openrewrite.ExecutionContext;
import org.openrewrite.NlsRewrite;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.search.IsLikelyTest;
import org.openrewrite.java.tree.J;

/**
 * A recipe that update the bom version to latest available.
 */
public class ReplaceRemovedSSHLauncherConstructor extends Recipe {

    @Override
    public @NlsRewrite.DisplayName String getDisplayName() {
        return "Replace a remove SSHLauncher constructor";
    }

    @Override
    public @NlsRewrite.Description String getDescription() {
        return "Replace a remove SSHLauncher constructor.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        // Only run on test files
        return Preconditions.check(new IsLikelyTest(), new UseDataBoundConstructor());
    }

    /**
     * Visitor that replace the removed SSHLauncher removed constructor by the new one.
     */
    public static class UseDataBoundConstructor extends JavaIsoVisitor<ExecutionContext> {

        // We will replace by the @DataBoundConstructor
        JavaTemplate newConstructorTemplate = JavaTemplate.builder(
                        "new SSHLauncher(#{any(java.lang.String)}, #{any(int)}, null)")
                .imports("hudson.plugins.sshslaves.SSHLauncher")
                .javaParser(JavaParser.fromJavaVersion().classpath("ssh-slaves"))
                .build();

        @Override
        public J.NewClass visitNewClass(J.NewClass newClass, ExecutionContext ctx) {
            newClass = super.visitNewClass(newClass, ctx);
            if (newClass.getConstructorType() == null) {
                return newClass;
            }
            if (!newClass.getConstructorType()
                    .getDeclaringType()
                    .getFullyQualifiedName()
                    .equals("hudson.plugins.sshslaves.SSHLauncher")) {
                return newClass;
            }
            // Replace removed 6 arguments constructor with 3 arguments constructor
            // See https://github.com/jenkinsci/ssh-agents-plugin/commit/f540572d7819bec840605227636de319a192bc84
            if (newClass.getArguments().size() == 6) {
                return newConstructorTemplate.apply(
                        updateCursor(newClass),
                        newClass.getCoordinates().replace(),
                        newClass.getArguments().get(0),
                        newClass.getArguments().get(1));
            }
            return newClass;
        }
    }
}
