package io.jenkins.tools.pluginmodernizer.core.recipes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.ChangeMethodName;
import org.openrewrite.java.ChangePackage;
import org.openrewrite.java.ChangeType;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JLeftPadded;
import org.openrewrite.java.tree.Space;
import org.openrewrite.marker.Markers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MigrateAcegiSecurityToSpringSecurity extends Recipe {
    /**
     * Logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(MigrateAcegiSecurityToSpringSecurity.class);

    @Override
    public String getDisplayName() {
        return "Migrate Acegi Security to Spring Security";
    }

    @Override
    public String getDescription() {
        return "Migrate acegi security to spring security.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<>() {
            @Override
            public J.CompilationUnit visitCompilationUnit(J.CompilationUnit cu, ExecutionContext ctx) {
                // ChangePackage will take care for the most of the migration so don't need to add separate migrations
                // For those import statements that ChangePackage will not account correctly, add separate logic
                cu = (J.CompilationUnit)
                        new ChangePackage("org.acegisecurity", "org.springframework.security.core", false)
                                .getVisitor()
                                .visitNonNull(cu, ctx);

                cu = (J.CompilationUnit) new ChangeType(
                                "org.springframework.security.core.GrantedAuthorityImpl",
                                "org.springframework.security.core.authority.SimpleGrantedAuthority",
                                null)
                        .getVisitor()
                        .visitNonNull(cu, ctx);

                // Authentication classes
                cu = (J.CompilationUnit) new ChangeType(
                                "org.acegisecurity.providers.AbstractAuthenticationToken",
                                "org.springframework.security.authentication.AbstractAuthenticationToken",
                                null)
                        .getVisitor()
                        .visitNonNull(cu, ctx);

                List<J.Import> originalImports = cu.getImports();

                cu = (J.CompilationUnit) new ChangeType(
                                "org.springframework.security.core.AuthenticationManager",
                                " org.springframework.security.authentication.AuthenticationManager",
                                null)
                        .getVisitor()
                        .visitNonNull(cu, ctx);
                if (!cu.getImports().equals(originalImports)) {
                    cu = addImportIfNotExists(
                            cu, "AuthenticationManager", "org.springframework.security.authentication");
                }
                originalImports = cu.getImports();
                cu = (J.CompilationUnit) new ChangeType(
                                "org.springframework.security.core.BadCredentialsException",
                                "org.springframework.security.authentication.BadCredentialsException",
                                null)
                        .getVisitor()
                        .visitNonNull(cu, ctx);

                if (!cu.getImports().equals(originalImports)) {
                    cu = addImportIfNotExists(
                            cu, "BadCredentialsException", "org.springframework.security.authentication");
                }

                return super.visitCompilationUnit(cu, ctx);
            }

            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                method = (J.MethodInvocation) new ChangeMethodName(
                                "jenkins.model.Jenkins getAuthentication()", "getAuthentication2", null, null)
                        .getVisitor()
                        .visitNonNull(method, ctx);
                // Migrate fireAuthenticated to fireAuthenticated2
                if (method.getSimpleName().equals("fireAuthenticated")) {
                    method = method.withName(method.getName().withSimpleName("fireAuthenticated2"));
                }
                return super.visitMethodInvocation(method, ctx);
            }

            private J.CompilationUnit addImportIfNotExists(J.CompilationUnit cu, String className, String packageName) {
                boolean importExists = cu.getImports().stream().anyMatch(anImport -> (packageName + "." + className)
                        .equals(anImport.getQualid().toString()));
                if (!importExists) {
                    J.Identifier identifier = new J.Identifier(
                            UUID.randomUUID(),
                            Space.EMPTY,
                            Markers.EMPTY,
                            Collections.emptyList(),
                            className,
                            null,
                            null);

                    J.Identifier packageIdentifier = new J.Identifier(
                            UUID.randomUUID(),
                            Space.SINGLE_SPACE,
                            Markers.EMPTY,
                            Collections.emptyList(),
                            packageName,
                            null,
                            null);

                    J.FieldAccess fieldAccess = new J.FieldAccess(
                            UUID.randomUUID(),
                            Space.EMPTY,
                            Markers.EMPTY,
                            packageIdentifier,
                            JLeftPadded.build(identifier),
                            null);

                    J.Import newImport = new J.Import(
                            UUID.randomUUID(),
                            Space.format("\n"), // Ensure the import appears on a new line
                            Markers.EMPTY,
                            JLeftPadded.build(false), // static: false
                            fieldAccess,
                            null);

                    List<J.Import> modifiedImports = new ArrayList<>(cu.getImports());
                    modifiedImports.add(newImport);

                    return cu.withImports(modifiedImports);
                }

                return cu;
            }
        };
    }
}
