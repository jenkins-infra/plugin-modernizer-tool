package io.jenkins.tools.pluginmodernizer.core.recipes;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.ChangeMethodName;
import org.openrewrite.java.ChangePackage;
import org.openrewrite.java.ChangeType;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JContainer;
import org.openrewrite.java.tree.JLeftPadded;
import org.openrewrite.java.tree.JRightPadded;
import org.openrewrite.java.tree.JavaType;
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
                        new ChangePackage("org.acegisecurity", "org.springframework.security.core", true)
                                .getVisitor()
                                .visitNonNull(cu, ctx);

                List<J.Import> originalImports = cu.getImports();

                cu = (J.CompilationUnit) new ChangeType(
                                "org.springframework.security.core.GrantedAuthorityImpl",
                                "org.springframework.security.core.authority.SimpleGrantedAuthority",
                                null)
                        .getVisitor()
                        .visitNonNull(cu, ctx);

                if (!cu.getImports().equals(originalImports)) {
                    cu = addImportIfNotExists(
                            cu, "SimpleGrantedAuthority", "org.springframework.security.core.authority");
                }
                originalImports = cu.getImports();

                // Authentication classes
                cu = (J.CompilationUnit) new ChangeType(
                                "org.springframework.security.core.providers.AbstractAuthenticationToken",
                                "org.springframework.security.authentication.AbstractAuthenticationToken",
                                null)
                        .getVisitor()
                        .visitNonNull(cu, ctx);
                if (!cu.getImports().equals(originalImports)) {
                    cu = addImportIfNotExists(
                            cu, "AbstractAuthenticationToken", "org.springframework.security.authentication");

                    cu = addImportIfNotExists(cu, "List", "java.util");
                    cu = addImportIfNotExists(cu, "Collection", "java.util");
                }
                originalImports = cu.getImports();

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

                // add java.util.Collections where UserDetails is used
                for (J.Import anImport : cu.getImports()) {
                    String importName = anImport.getQualid().toString();
                    if (importName.equals("import org.acegisecurity.userdetails.UserDetails")
                            || importName.equals("org.springframework.security.core.userdetails.UserDetails")) {
                        cu = addImportIfNotExists(cu, "Collection", "java.util");
                    }
                }

                return super.visitCompilationUnit(cu, ctx);
            }

            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                // migration of getAuthentication to getAuthentication2
                method = (J.MethodInvocation) new ChangeMethodName(
                                "jenkins.model.Jenkins getAuthentication()", "getAuthentication2", null, null)
                        .getVisitor()
                        .visitNonNull(method, ctx);

                // Migrate fireAuthenticated to fireAuthenticated2
                if (method.getSimpleName().equals("fireAuthenticated")) {
                    method = method.withName(method.getName().withSimpleName("fireAuthenticated2"));
                }

                // migrate loadUserByUsername to loadUserByUsername2
                MethodMatcher methodMatcher =
                        new MethodMatcher("hudson.security.SecurityRealm loadUserByUsername(java.lang.String)", true);
                if (methodMatcher.matches(method)) {
                    JavaType.Method type = method.getMethodType();

                    if (type != null) {
                        type = type.withName("loadUserByUsername2");
                    }

                    method = method.withName(method.getName()
                                    .withSimpleName("loadUserByUsername2")
                                    .withType(type))
                            .withMethodType(type);
                }
                return super.visitMethodInvocation(method, ctx);
            }

            @Override
            public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext ctx) {
                // migration of changing the getAuthorities() return type from GrantedAuthority[] to
                // Collection<GrantedAuthority>
                // Identify methods named `getAuthorities()`
                J.ClassDeclaration enclosingClass = getCursor().firstEnclosing(J.ClassDeclaration.class);
                if ("getAuthorities".equals(method.getSimpleName())) {
                    JavaType returnType = method.getReturnTypeExpression().getType();

                    // Check if the return type is an array of `GrantedAuthority`
                    if (returnType instanceof JavaType.Array
                            && ((JavaType.Array) returnType)
                                    .getElemType()
                                    .toString()
                                    .contains("GrantedAuthority")) {

                        List<JRightPadded<Expression>> typeParameters = Collections.singletonList(new JRightPadded<>(
                                new J.Identifier(
                                        UUID.randomUUID(),
                                        Space.EMPTY,
                                        Markers.EMPTY,
                                        "GrantedAuthority",
                                        JavaType.buildType("org.springframework.security.core.GrantedAuthority"),
                                        null),
                                Space.EMPTY, // No trailing space
                                Markers.EMPTY));

                        JContainer<Expression> typeParametersContainer = JContainer.build(typeParameters);
                        // Change return type to `Collection<GrantedAuthority>`
                        method = method.withReturnTypeExpression(new J.ParameterizedType(
                                UUID.randomUUID(),
                                Space.EMPTY,
                                Markers.EMPTY,
                                new J.Identifier(
                                        UUID.randomUUID(),
                                        Space.SINGLE_SPACE,
                                        Markers.EMPTY,
                                        "Collection",
                                        JavaType.buildType("java.util.Collection"),
                                        null),
                                typeParametersContainer,
                                JavaType.buildType("java.util.Collection")));

                        //                         Modify method body to return the list directly
                        if (method.getBody() != null
                                && !method.getBody().getStatements().isEmpty()) {
                            String grantedAuthoritiesFieldName = null;
                            assert enclosingClass != null;
                            for (J.VariableDeclarations field : enclosingClass.getBody().getStatements().stream()
                                    .filter(J.VariableDeclarations.class::isInstance)
                                    .map(J.VariableDeclarations.class::cast)
                                    .toList()) {
                                if (field.getTypeExpression() != null
                                        && field.getTypeExpression().getType() instanceof JavaType.Parameterized
                                        && ((JavaType.Parameterized) field.getTypeExpression()
                                                        .getType())
                                                .getType()
                                                .toString()
                                                .equals("java.util.List")
                                        && ((JavaType.Parameterized) field.getTypeExpression()
                                                        .getType())
                                                .getTypeParameters()
                                                .get(0)
                                                .toString()
                                                .equals("org.springframework.security.core.GrantedAuthority")) {
                                    grantedAuthoritiesFieldName =
                                            field.getVariables().get(0).getSimpleName();
                                    break;
                                }
                            }

                            if (grantedAuthoritiesFieldName != null) {
                                method = method.withBody(method.getBody()
                                        .withStatements(Collections.singletonList(new J.Return(
                                                UUID.randomUUID(),
                                                Space.format("\n" + " ".repeat(8)),
                                                Markers.EMPTY,
                                                new J.Identifier(
                                                        UUID.randomUUID(),
                                                        Space.SINGLE_SPACE,
                                                        Markers.EMPTY,
                                                        grantedAuthoritiesFieldName,
                                                        JavaType.buildType("java.util.List"),
                                                        null)))));
                            } else {
                                method = method.withBody(method.getBody()
                                        .withStatements(method.getBody().getStatements().stream()
                                                .map(statement -> {
                                                    // Check if the statement is a return statement with `new
                                                    // GrantedAuthority[0]`
                                                    if (statement instanceof J.Return) {
                                                        J.Return returnStatement = (J.Return) statement;
                                                        if (returnStatement.getExpression() instanceof J.Ternary) {
                                                            J.Ternary ternaryExpression =
                                                                    (J.Ternary) returnStatement.getExpression();
                                                            if (ternaryExpression.getFalsePart()
                                                                    instanceof J.NewArray) {
                                                                J.NewArray newArrayExpression =
                                                                        (J.NewArray) ternaryExpression.getFalsePart();
                                                                if (newArrayExpression
                                                                                .getType()
                                                                                .toString()
                                                                                .contains("GrantedAuthority[]")
                                                                        && newArrayExpression
                                                                                .getDimensions()
                                                                                .get(0)
                                                                                .toString()
                                                                                .contains("[0]")) {
                                                                    LOG.info(
                                                                            "Found `new GrantedAuthority[0]` inside a ternary expression in getAuthorities method");
                                                                    // Replace `new GrantedAuthority[0]` with
                                                                    // `List.of()` in the ternary expression
                                                                    ternaryExpression = ternaryExpression.withFalsePart(
                                                                            new J.Identifier(
                                                                                    UUID.randomUUID(),
                                                                                    Space.SINGLE_SPACE,
                                                                                    Markers.EMPTY,
                                                                                    "List.of()",
                                                                                    JavaType.buildType(
                                                                                            "java.util.List"),
                                                                                    null));
                                                                }
                                                            }
                                                            return returnStatement.withExpression(
                                                                    ternaryExpression); // Update the return statement
                                                            // with the modified ternary
                                                        }
                                                    }
                                                    return statement; // Return the unchanged statement if it's not a
                                                    // `new GrantedAuthority[0]`
                                                })
                                                .collect(Collectors.toList())));
                            }
                        }
                    }
                }

                // Migrate loadUserByUsername to loadUserByUsername2 method declaration if the class extends Security
                // Realm
                MethodMatcher methodMatcher =
                        new MethodMatcher("hudson.security.SecurityRealm loadUserByUsername(java.lang.String)", true);
                if (enclosingClass != null
                        && enclosingClass.getExtends() != null
                        && enclosingClass.getExtends().getType() != null
                        && enclosingClass.getExtends().getType().toString().equals("hudson.security.SecurityRealm")) {

                    // Check if the overriding method is named loadUserByUsername
                    if (methodMatcher.matches(method, enclosingClass)) {
                        JavaType.Method type = method.getMethodType();

                        if (method.getMethodType().getOverride() != null) {
                            LOG.info(
                                    "Don't migrate this one to loadUserByUsername2 {}",
                                    method.getMethodType().getOverride().toString());
                            return super.visitMethodDeclaration(method, ctx);
                        }
                        if (type != null) {
                            type = type.withName("loadUserByUsername2");
                        }
                        method = method.withName(method.getName()
                                        .withSimpleName("loadUserByUsername2")
                                        .withType(type))
                                .withMethodType(type);
                    }
                }

                // add super(null) call to the constructor of the class that extends AbstractAuthenticationToken
                if (enclosingClass != null
                        && enclosingClass.getExtends() != null
                        && enclosingClass.getExtends().getType() != null
                        && enclosingClass
                                .getExtends()
                                .getType()
                                .toString()
                                .equals("org.springframework.security.authentication.AbstractAuthenticationToken")) {
                    if (method.isConstructor()) {
                        if (method.getBody() != null) {
                            J.Block body = method.getBody();

                            // Avoid duplicate insertions
                            if (body.getStatements().isEmpty()
                                    || !(body.getStatements().get(0) instanceof J.MethodInvocation
                                            && body.getStatements()
                                                    .get(0)
                                                    .printTrimmed()
                                                    .contains("super("))) {
                                method = addSuperCallTemplate.apply(
                                        updateCursor(method),
                                        body.getCoordinates().firstStatement());
                            }
                        }
                    }
                }

                return super.visitMethodDeclaration(method, ctx);
            }

            private final JavaTemplate addSuperCallTemplate = JavaTemplate.builder("super(null);")
                    .contextSensitive()
                    .javaParser(JavaParser.fromJavaVersion()
                            .addClasspathEntry(
                                    Path.of("target/openrewrite-jars").resolve("jenkins-core-2.497.jar")))
                    .build();

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
