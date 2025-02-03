package io.jenkins.tools.pluginmodernizer.core.model;

/**
 * # Note that all split plugins between and including matrix-auth and jaxb incorrectly use the first
 * # core release without the plugin's functionality when they should use the immediately prior release.
 * # Fixing these retroactively won't help, as the difference only matters to those specific versions.
 */
import java.util.Set;

/**
 * Detached plugins.
 */
public enum DetachedPlugins {
    MAVEN_PLUGIN(
            "maven-plugin",
            "1.296",
            "1.296",
            "org.jenkins-ci.main",
            Set.of("hudson.maven"),
            Set.of(
                    "hudson.maven.MavenModule",
                    "hudson.maven.MavenModuleSet",
                    "hudson.maven.MavenModuleSetBuild",
                    "hudson.maven.MavenBuild.java")),
    SUBVERSION("subversion", "1.310", "1.0", "org.jenkins-ci.plugins", Set.of(), Set.of("hudson.scm.SubversionSCM")),
    CVS("cvs", "1.340", "0.1", "org.jenkins-ci.plugins", Set.of(), Set.of("hudson.scm.CVSSCM")),
    ANT("ant", "1.430", "1.0", "org.jenkins-ci.plugins", Set.of(), Set.of("hudson.tasks.Ant")),
    JAVADOC("javadoc", "1.430", "1.0", "org.jenkins-ci.plugins", Set.of(), Set.of("hudson.tasks.JavadocArchiver")),
    EXTERNAL_MONITOR_JOB(
            "external-monitor-job",
            "1.467",
            "1.0",
            "org.jenkins-ci.plugins",
            Set.of(),
            Set.of("hudson.model.ExternalJob", "hudson.model.ExternalRun")),
    LDAP(
            "ldap",
            "1.467",
            "1.0",
            "org.jenkins-ci.plugins",
            Set.of(),
            Set.of("hudson.security.LDAPSecurityRealm", "hudson.security.GeneralizedTime")),
    PAM_AUTH(
            "pam-auth", "1.467", "1.0", "org.jenkins-ci.plugins", Set.of(), Set.of("hudson.security.PAMSecurityRealm")),
    MAILER("mailer", "1.493", "1.2", "org.jenkins-ci.plugins", Set.of(), Set.of("hudson.tasks.Mailer")),
    MATRIX_AUTH(
            "matrix-auth",
            "1.535",
            "1.0.2",
            "org.jenkins-ci.plugins",
            Set.of(),
            Set.of(
                    "hudson.security.GlobalMatrixAuthorizationStrategy",
                    "hudson.security.ProjectMatrixAuthorizationStrategy",
                    "hudson.security.AuthorizationMatrixProperty")),
    ANTISAMY_MARKUP_FORMATTER(
            "antisamy-markup-formatter",
            "1.553",
            "1.0",
            "org.jenkins-ci.plugins",
            Set.of(),
            Set.of("hudson.markup.RawHtmlMarkupFormatter")),
    MATRIX_PROJECT(
            "matrix-project",
            "1.561",
            "1.0",
            "org.jenkins-ci.plugins",
            Set.of("hudson.matrix"),
            Set.of("hudson.matrix.MatrixProject")),
    JUNIT(
            "junit",
            "1.577",
            "1.0",
            "org.jenkins-ci.plugins",
            Set.of(),
            Set.of("hudson.tasks.junit.JUnitResultArchiver")),
    BOUNCYCASTLE_API(
            "bouncycastle-api",
            "2.16",
            "2.16.0",
            "org.jenkins-ci.plugins",
            Set.of("jenkins.bouncycastle.api"),
            Set.of("jenkins.bouncycastle.api.BouncyCastlePlugin")),
    COMMAND_LAUNCHER(
            "command-launcher",
            "2.86",
            "1.0",
            "org.jenkins-ci.plugins",
            Set.of(),
            Set.of("hudson.slaves.CommandLauncher", "hudson.slaves.CommandConnector")),
    JDK_TOOL("jdk-tool", "2.112", "1.0", "org.jenkins-ci.plugins", Set.of(), Set.of("hudson.tools.JDKInstaller")),
    JAXB("jaxb", "2.163", "2.3.0", "io.jenkins.plugins", Set.of(), Set.of("javax.xml.bind.JAXBContext")),
    TRILEAD_API(
            "trilead-api",
            "2.184",
            "1.0.4",
            "org.jenkins-ci.plugins",
            Set.of(),
            Set.of("com.trilead.ssh2.Connection", "com.trilead.ssh2.Session")),
    SSHD(
            "sshd",
            "2.281",
            "3.236.ved5e1b_cb_50b_2",
            "org.jenkins-ci.modules",
            Set.of(),
            Set.of("org.jenkinsci.main.modules.sshd.SSHD")),
    JAVAX_ACTIVATION_API(
            "javax-activation-api",
            "2.330",
            "1.2.0-2",
            "io.jenkins.plugins",
            Set.of(),
            Set.of("javax.activation.DataHandler")),
    JAVAX_MAIL_API(
            "javax-mail-api",
            "2.330",
            "1.6.2-5",
            "io.jenkins.plugins",
            Set.of(),
            Set.of(
                    "jenkins.plugins.javax.activation.CommandMapInitializer",
                    "jenkins.plugins.javax.activation.FileTypeMapInitializer")),
    INSTANCE_IDENTITY(
            "instance-identity",
            "2.356",
            "3.1",
            "org.jenkins-ci.modules",
            Set.of("org.jenkinsci.main.modules.instance_identity"),
            Set.of("org.jenkinsci.main.modules.instance_identity.InstanceIdentity")),
    ;

    private final String pluginId;
    private final String lastCoreRelease;
    private final String impliedVersion;
    private final String groupId;
    private final Set<String> packageName;
    private final Set<String> classNames;

    DetachedPlugins(
            String pluginId,
            String lastCoreRelease,
            String impliedVersion,
            String groupId,
            Set<String> packageName,
            Set<String> classNames) {
        this.pluginId = pluginId;
        this.lastCoreRelease = lastCoreRelease;
        this.impliedVersion = impliedVersion;
        this.groupId = groupId;
        this.packageName = packageName;
        this.classNames = classNames;
    }

    public String getPluginId() {
        return pluginId;
    }

    public String getLastCoreRelease() {
        return lastCoreRelease;
    }

    public String getImpliedVersion() {
        return impliedVersion;
    }

    public String getGroupId() {
        return groupId;
    }

    public Set<String> getPackageName() {
        return packageName;
    }

    public Set<String> getClassNames() {
        return classNames;
    }
}
