package io.jenkins.tools.pluginmodernizer.core.recipes;

import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.java.Assertions.srcMainJava;
import static org.openrewrite.java.Assertions.srcTestJava;
import static org.openrewrite.maven.Assertions.pomXml;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.openrewrite.test.RewriteTest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Test for {@link AddDetachedPluginDependency}.
 */
@Execution(ExecutionMode.CONCURRENT)
public class AddDetachedPluginDependencyTest implements RewriteTest {

    /**
     * LOGGER.
     */
    private static final Logger LOG = LoggerFactory.getLogger(AddDetachedPluginDependencyTest.class);

    @Test
    void detectsDetachedPluginUsageAndAddsDependencyWithoutBom() {
        rewriteRun(
                spec -> spec.recipe(new AddDetachedPluginDependency("2.440.3")), // language=xml
                pomXml(
                        """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
                  <modelVersion>4.0.0</modelVersion>
                  <parent>
                    <groupId>org.jenkins-ci.plugins</groupId>
                    <artifactId>plugin</artifactId>
                    <version>4.87</version>
                    <relativePath />
                  </parent>
                  <groupId>io.jenkins.plugins</groupId>
                  <artifactId>empty</artifactId>
                  <version>1.0.0-SNAPSHOT</version>
                  <packaging>hpi</packaging>
                  <name>Empty Plugin</name>
                  <properties>
                    <jenkins.version>2.440.3</jenkins.version>
                  </properties>
                  <repositories>
                    <repository>
                      <id>repo.jenkins-ci.org</id>
                      <url>https://repo.jenkins-ci.org/public/</url>
                    </repository>
                  </repositories>
                  <pluginRepositories>
                    <pluginRepository>
                      <id>repo.jenkins-ci.org</id>
                      <url>https://repo.jenkins-ci.org/public/</url>
                    </pluginRepository>
                  </pluginRepositories>
                </project>
                """,
                        """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
                  <modelVersion>4.0.0</modelVersion>
                  <parent>
                    <groupId>org.jenkins-ci.plugins</groupId>
                    <artifactId>plugin</artifactId>
                    <version>4.87</version>
                    <relativePath />
                  </parent>
                  <groupId>io.jenkins.plugins</groupId>
                  <artifactId>empty</artifactId>
                  <version>1.0.0-SNAPSHOT</version>
                  <packaging>hpi</packaging>
                  <name>Empty Plugin</name>
                  <properties>
                    <jenkins.version>2.440.3</jenkins.version>
                  </properties>
                  <dependencies>
                    <dependency>
                      <groupId>io.jenkins.plugins</groupId>
                      <artifactId>javax-activation-api</artifactId>
                      <version>RELEASE</version>
                    </dependency>
                    <dependency>
                      <groupId>io.jenkins.plugins</groupId>
                      <artifactId>javax-mail-api</artifactId>
                      <version>RELEASE</version>
                    </dependency>
                    <dependency>
                      <groupId>io.jenkins.plugins</groupId>
                      <artifactId>jaxb</artifactId>
                      <version>RELEASE</version>
                    </dependency>
                    <dependency>
                      <groupId>org.jenkins-ci.main</groupId>
                      <artifactId>maven-plugin</artifactId>
                      <version>RELEASE</version>
                    </dependency>
                    <dependency>
                      <groupId>org.jenkins-ci.modules</groupId>
                      <artifactId>instance-identity</artifactId>
                      <version>RELEASE</version>
                    </dependency>
                    <dependency>
                      <groupId>org.jenkins-ci.modules</groupId>
                      <artifactId>sshd</artifactId>
                      <version>RELEASE</version>
                    </dependency>
                    <dependency>
                      <groupId>org.jenkins-ci.plugins</groupId>
                      <artifactId>ant</artifactId>
                      <version>RELEASE</version>
                    </dependency>
                    <dependency>
                      <groupId>org.jenkins-ci.plugins</groupId>
                      <artifactId>antisamy-markup-formatter</artifactId>
                      <version>RELEASE</version>
                    </dependency>
                    <dependency>
                      <groupId>org.jenkins-ci.plugins</groupId>
                      <artifactId>bouncycastle-api</artifactId>
                      <version>RELEASE</version>
                    </dependency>
                    <dependency>
                      <groupId>org.jenkins-ci.plugins</groupId>
                      <artifactId>command-launcher</artifactId>
                      <version>RELEASE</version>
                    </dependency>
                    <dependency>
                      <groupId>org.jenkins-ci.plugins</groupId>
                      <artifactId>external-monitor-job</artifactId>
                      <version>RELEASE</version>
                    </dependency>
                    <dependency>
                      <groupId>org.jenkins-ci.plugins</groupId>
                      <artifactId>javadoc</artifactId>
                      <version>RELEASE</version>
                    </dependency>
                    <dependency>
                      <groupId>org.jenkins-ci.plugins</groupId>
                      <artifactId>jdk-tool</artifactId>
                      <version>RELEASE</version>
                    </dependency>
                    <dependency>
                      <groupId>org.jenkins-ci.plugins</groupId>
                      <artifactId>junit</artifactId>
                      <version>RELEASE</version>
                    </dependency>
                    <dependency>
                      <groupId>org.jenkins-ci.plugins</groupId>
                      <artifactId>ldap</artifactId>
                      <version>RELEASE</version>
                    </dependency>
                    <dependency>
                      <groupId>org.jenkins-ci.plugins</groupId>
                      <artifactId>mailer</artifactId>
                      <version>RELEASE</version>
                    </dependency>
                    <dependency>
                      <groupId>org.jenkins-ci.plugins</groupId>
                      <artifactId>matrix-auth</artifactId>
                      <version>RELEASE</version>
                    </dependency>
                    <dependency>
                      <groupId>org.jenkins-ci.plugins</groupId>
                      <artifactId>matrix-project</artifactId>
                      <version>RELEASE</version>
                    </dependency>
                    <dependency>
                      <groupId>org.jenkins-ci.plugins</groupId>
                      <artifactId>pam-auth</artifactId>
                      <version>RELEASE</version>
                    </dependency>
                    <dependency>
                      <groupId>org.jenkins-ci.plugins</groupId>
                      <artifactId>subversion</artifactId>
                      <version>RELEASE</version>
                    </dependency>
                    <dependency>
                      <groupId>org.jenkins-ci.plugins</groupId>
                      <artifactId>trilead-api</artifactId>
                      <version>RELEASE</version>
                    </dependency>
                  </dependencies>
                  <repositories>
                    <repository>
                      <id>repo.jenkins-ci.org</id>
                      <url>https://repo.jenkins-ci.org/public/</url>
                    </repository>
                  </repositories>
                  <pluginRepositories>
                    <pluginRepository>
                      <id>repo.jenkins-ci.org</id>
                      <url>https://repo.jenkins-ci.org/public/</url>
                    </pluginRepository>
                  </pluginRepositories>
                </project>
                """),
                srcTestJava(
                        java(
                                // language=java
                                """
                package hudson.maven;
                public class MavenModuleSet {}
                """),
                        java(
                                // language=java
                                """
                package hudson.scm;
                public class SubversionSCM {}
                """),
                        java(
                                // language=java
                                """
                package hudson.tasks;
                public class Ant {}
                """),
                        java(
                                // language=java
                                """
                package hudson.tasks;
                public class JavadocArchiver {}
                """),
                        java(
                                // language=java
                                """
                package hudson.tasks;
                public class Mailer {}
                """),
                        java(
                                // language=java
                                """
                package hudson.tasks.junit;
                public class JUnitResultArchiver {}
                """),
                        java(
                                // language=java
                                """
                package hudson.model;
                public class ExternalJob {}
                """),
                        java(
                                // language=java
                                """
                package hudson.security;
                public class LDAPSecurityRealm {}
                """),
                        java(
                                // language=java
                                """
                package hudson.security;
                public class PAMSecurityRealm {}
                """),
                        java(
                                // language=java
                                """
                package hudson.security;
                public class GlobalMatrixAuthorizationStrategy {}
                """),
                        java(
                                // language=java
                                """
                package hudson.security;
                public class ProjectMatrixAuthorizationStrategy {}
                """),
                        java(
                                // language=java
                                """
                package hudson.security;
                public class AuthorizationMatrixProperty {}
                """),
                        java(
                                // language=java
                                """
                package hudson.slaves;
                public class CommandLauncher {}
                """),
                        java(
                                // language=java
                                """
                package hudson.tools;
                public class JDKInstaller {}
                """),
                        java(
                                // language=java
                                """
                package javax.xml.bind;
                public class JAXBContext {}
                """),
                        java(
                                // language=java
                                """
                package com.trilead.ssh2;
                public class Connection {}
                """),
                        java(
                                // language=java
                                """
                package org.jenkinsci.main.modules.sshd;
                public class SSHD {}
                """),
                        java(
                                // language=java
                                """
                package javax.activation;
                public class DataHandler {}
                """),
                        java(
                                // language=java
                                """
                package jenkins.bouncycastle.api;
                public class BouncyCastlePlugin {}
                """),
                        java(
                                // language=java
                                """
                package jenkins.plugins.javax.activation;
                public class CommandMapInitializer {}
                """),
                        java(
                                // language=java
                                """
                package jenkins.plugins.javax.activation;
                public class FileTypeMapInitializer {}
                """),
                        java(
                                // language=java
                                """
                package org.jenkinsci.main.modules.instance_identity;
                public class InstanceIdentity {}
                """),
                        java(
                                // language=java
                                """
                package hudson.markup;
                public class RawHtmlMarkupFormatter {}
                """),
                        java(
                                // language=java
                                """
                package hudson.matrix;
                public class MatrixProject {}
                """)),
                srcMainJava(
                        // language=java
                        java(
                                """
                import hudson.maven.MavenModuleSet;
                import hudson.scm.SubversionSCM;
                import hudson.tasks.Ant;
                import hudson.tasks.JavadocArchiver;
                import hudson.tasks.Mailer;
                import hudson.tasks.junit.JUnitResultArchiver;
                import hudson.model.ExternalJob;
                import hudson.security.LDAPSecurityRealm;
                import hudson.security.PAMSecurityRealm;
                import hudson.security.GlobalMatrixAuthorizationStrategy;
                import hudson.security.ProjectMatrixAuthorizationStrategy;
                import hudson.security.AuthorizationMatrixProperty;
                import hudson.slaves.CommandLauncher;
                import hudson.tools.JDKInstaller;
                import javax.xml.bind.JAXBContext;
                import com.trilead.ssh2.Connection;
                import org.jenkinsci.main.modules.sshd.SSHD;
                import javax.activation.DataHandler;
                import jenkins.bouncycastle.api.BouncyCastlePlugin;
                import jenkins.plugins.javax.activation.CommandMapInitializer;
                import jenkins.plugins.javax.activation.FileTypeMapInitializer;
                import org.jenkinsci.main.modules.instance_identity.InstanceIdentity;
                import hudson.markup.RawHtmlMarkupFormatter;
                import hudson.matrix.MatrixProject;
                """)));
    }

    @Test
    void detectsDetachedPluginUsageAndAddsDependencyWithBom() {
        rewriteRun(
                spec -> spec.recipe(new AddDetachedPluginDependency("2.346.3")),
                // language=xml
                pomXml(
                        """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
                  <modelVersion>4.0.0</modelVersion>
                  <parent>
                    <groupId>org.jenkins-ci.plugins</groupId>
                    <artifactId>plugin</artifactId>
                    <version>4.51</version>
                    <relativePath />
                  </parent>
                  <groupId>io.jenkins.plugins</groupId>
                  <artifactId>empty</artifactId>
                  <version>1.0.0-SNAPSHOT</version>
                  <packaging>hpi</packaging>
                  <name>Empty Plugin</name>
                  <properties>
                    <jenkins.baseline>2.346</jenkins.baseline>
                    <jenkins.version>${jenkins.baseline}.3</jenkins.version>
                  </properties>
                  <dependencyManagement>
                    <dependencies>
                      <dependency>
                        <groupId>io.jenkins.tools.bom</groupId>
                        <artifactId>bom-${jenkins.baseline}.x</artifactId>
                        <version>1763.v092b_8980a_f5e</version>
                        <type>pom</type>
                        <scope>import</scope>
                      </dependency>
                    </dependencies>
                  </dependencyManagement>
                    <dependencies>
                    </dependencies>
                  <repositories>
                    <repository>
                      <id>repo.jenkins-ci.org</id>
                      <url>https://repo.jenkins-ci.org/public/</url>
                    </repository>
                  </repositories>
                  <pluginRepositories>
                    <pluginRepository>
                      <id>repo.jenkins-ci.org</id>
                      <url>https://repo.jenkins-ci.org/public/</url>
                    </pluginRepository>
                  </pluginRepositories>
                </project>
                """,
                        """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
                  <modelVersion>4.0.0</modelVersion>
                  <parent>
                    <groupId>org.jenkins-ci.plugins</groupId>
                    <artifactId>plugin</artifactId>
                    <version>4.51</version>
                    <relativePath />
                  </parent>
                  <groupId>io.jenkins.plugins</groupId>
                  <artifactId>empty</artifactId>
                  <version>1.0.0-SNAPSHOT</version>
                  <packaging>hpi</packaging>
                  <name>Empty Plugin</name>
                  <properties>
                    <jenkins.baseline>2.346</jenkins.baseline>
                    <jenkins.version>${jenkins.baseline}.3</jenkins.version>
                  </properties>
                  <dependencyManagement>
                    <dependencies>
                      <dependency>
                        <groupId>io.jenkins.tools.bom</groupId>
                        <artifactId>bom-${jenkins.baseline}.x</artifactId>
                        <version>1763.v092b_8980a_f5e</version>
                        <type>pom</type>
                        <scope>import</scope>
                      </dependency>
                    </dependencies>
                  </dependencyManagement>
                    <dependencies>
                    <dependency>
                      <groupId>io.jenkins.plugins</groupId>
                      <artifactId>javax-activation-api</artifactId>
                    </dependency>
                    <dependency>
                      <groupId>io.jenkins.plugins</groupId>
                      <artifactId>javax-mail-api</artifactId>
                    </dependency>
                    <dependency>
                      <groupId>io.jenkins.plugins</groupId>
                      <artifactId>jaxb</artifactId>
                    </dependency>
                    <dependency>
                      <groupId>org.jenkins-ci.main</groupId>
                      <artifactId>maven-plugin</artifactId>
                      <version>RELEASE</version>
                    </dependency>
                    <dependency>
                      <groupId>org.jenkins-ci.modules</groupId>
                      <artifactId>sshd</artifactId>
                    </dependency>
                    <dependency>
                      <groupId>org.jenkins-ci.plugins</groupId>
                      <artifactId>ant</artifactId>
                    </dependency>
                    <dependency>
                      <groupId>org.jenkins-ci.plugins</groupId>
                      <artifactId>antisamy-markup-formatter</artifactId>
                    </dependency>
                    <dependency>
                      <groupId>org.jenkins-ci.plugins</groupId>
                      <artifactId>bouncycastle-api</artifactId>
                    </dependency>
                    <dependency>
                      <groupId>org.jenkins-ci.plugins</groupId>
                      <artifactId>command-launcher</artifactId>
                    </dependency>
                    <dependency>
                      <groupId>org.jenkins-ci.plugins</groupId>
                      <artifactId>external-monitor-job</artifactId>
                      <version>RELEASE</version>
                    </dependency>
                    <dependency>
                      <groupId>org.jenkins-ci.plugins</groupId>
                      <artifactId>javadoc</artifactId>
                    </dependency>
                    <dependency>
                      <groupId>org.jenkins-ci.plugins</groupId>
                      <artifactId>jdk-tool</artifactId>
                    </dependency>
                    <dependency>
                      <groupId>org.jenkins-ci.plugins</groupId>
                      <artifactId>junit</artifactId>
                    </dependency>
                    <dependency>
                      <groupId>org.jenkins-ci.plugins</groupId>
                      <artifactId>ldap</artifactId>
                    </dependency>
                    <dependency>
                      <groupId>org.jenkins-ci.plugins</groupId>
                      <artifactId>mailer</artifactId>
                    </dependency>
                    <dependency>
                      <groupId>org.jenkins-ci.plugins</groupId>
                      <artifactId>matrix-auth</artifactId>
                    </dependency>
                    <dependency>
                      <groupId>org.jenkins-ci.plugins</groupId>
                      <artifactId>matrix-project</artifactId>
                    </dependency>
                    <dependency>
                      <groupId>org.jenkins-ci.plugins</groupId>
                      <artifactId>pam-auth</artifactId>
                    </dependency>
                    <dependency>
                      <groupId>org.jenkins-ci.plugins</groupId>
                      <artifactId>subversion</artifactId>
                    </dependency>
                    <dependency>
                      <groupId>org.jenkins-ci.plugins</groupId>
                      <artifactId>trilead-api</artifactId>
                    </dependency>
                    </dependencies>
                  <repositories>
                    <repository>
                      <id>repo.jenkins-ci.org</id>
                      <url>https://repo.jenkins-ci.org/public/</url>
                    </repository>
                  </repositories>
                  <pluginRepositories>
                    <pluginRepository>
                      <id>repo.jenkins-ci.org</id>
                      <url>https://repo.jenkins-ci.org/public/</url>
                    </pluginRepository>
                  </pluginRepositories>
                </project>
                """),
                srcTestJava(
                        java(
                                // language=java
                                """
                package hudson.maven;
                public class MavenModuleSet {}
                """),
                        java(
                                // language=java
                                """
                package hudson.scm;
                public class SubversionSCM {}
                """),
                        java(
                                // language=java
                                """
                package hudson.tasks;
                public class Ant {}
                """),
                        java(
                                // language=java
                                """
                package hudson.tasks;
                public class JavadocArchiver {}
                """),
                        java(
                                // language=java
                                """
                package hudson.tasks;
                public class Mailer {}
                """),
                        java(
                                // language=java
                                """
                package hudson.tasks.junit;
                public class JUnitResultArchiver {}
                """),
                        java(
                                // language=java
                                """
                package hudson.model;
                public class ExternalJob {}
                """),
                        java(
                                // language=java
                                """
                package hudson.security;
                public class LDAPSecurityRealm {}
                """),
                        java(
                                // language=java
                                """
                package hudson.security;
                public class PAMSecurityRealm {}
                """),
                        java(
                                // language=java
                                """
                package hudson.security;
                public class GlobalMatrixAuthorizationStrategy {}
                """),
                        java(
                                // language=java
                                """
                package hudson.security;
                public class ProjectMatrixAuthorizationStrategy {}
                """),
                        java(
                                // language=java
                                """
                package hudson.security;
                public class AuthorizationMatrixProperty {}
                """),
                        java(
                                // language=java
                                """
                package hudson.slaves;
                public class CommandLauncher {}
                """),
                        java(
                                // language=java
                                """
                package hudson.tools;
                public class JDKInstaller {}
                """),
                        java(
                                // language=java
                                """
                package javax.xml.bind;
                public class JAXBContext {}
                """),
                        java(
                                // language=java
                                """
                package com.trilead.ssh2;
                public class Connection {}
                """),
                        java(
                                // language=java
                                """
                package org.jenkinsci.main.modules.sshd;
                public class SSHD {}
                """),
                        java(
                                // language=java
                                """
                package javax.activation;
                public class DataHandler {}
                """),
                        java(
                                // language=java
                                """
                package jenkins.bouncycastle.api;
                public class BouncyCastlePlugin {}
                """),
                        java(
                                // language=java
                                """
                package jenkins.plugins.javax.activation;
                public class CommandMapInitializer {}
                """),
                        java(
                                // language=java
                                """
                package jenkins.plugins.javax.activation;
                public class FileTypeMapInitializer {}
                """),
                        java(
                                // language=java
                                """
                package org.jenkinsci.main.modules.instance_identity;
                public class InstanceIdentity {}
                """),
                        java(
                                // language=java
                                """
                package hudson.markup;
                public class RawHtmlMarkupFormatter {}
                """),
                        java(
                                // language=java
                                """
                package hudson.matrix;
                public class MatrixProject {}
                """)),
                srcMainJava(
                        // language=java
                        java(
                                """
                import hudson.maven.MavenModuleSet;
                import hudson.scm.SubversionSCM;
                import hudson.tasks.Ant;
                import hudson.tasks.JavadocArchiver;
                import hudson.tasks.Mailer;
                import hudson.tasks.junit.JUnitResultArchiver;
                import hudson.model.ExternalJob;
                import hudson.security.LDAPSecurityRealm;
                import hudson.security.PAMSecurityRealm;
                import hudson.security.GlobalMatrixAuthorizationStrategy;
                import hudson.security.ProjectMatrixAuthorizationStrategy;
                import hudson.security.AuthorizationMatrixProperty;
                import hudson.slaves.CommandLauncher;
                import hudson.tools.JDKInstaller;
                import javax.xml.bind.JAXBContext;
                import com.trilead.ssh2.Connection;
                import org.jenkinsci.main.modules.sshd.SSHD;
                import javax.activation.DataHandler;
                import jenkins.bouncycastle.api.BouncyCastlePlugin;
                import jenkins.plugins.javax.activation.CommandMapInitializer;
                import jenkins.plugins.javax.activation.FileTypeMapInitializer;
                import org.jenkinsci.main.modules.instance_identity.InstanceIdentity;
                import hudson.markup.RawHtmlMarkupFormatter;
                import hudson.matrix.MatrixProject;

                public class TestDetachedPluginsUsage {
                    public void execute() {
                        new MavenModuleSet();
                        new SubversionSCM();
                        new Ant();
                        new JavadocArchiver();
                        new Mailer();
                        new JUnitResultArchiver();
                        new ExternalJob();
                        new LDAPSecurityRealm();
                        new PAMSecurityRealm();
                        new GlobalMatrixAuthorizationStrategy();
                        new ProjectMatrixAuthorizationStrategy();
                        new AuthorizationMatrixProperty();
                        new CommandLauncher();
                        new JDKInstaller();
                        new JAXBContext();
                        new Connection();
                        new SSHD();
                        new DataHandler();
                        new BouncyCastlePlugin();
                        new CommandMapInitializer();
                        new FileTypeMapInitializer();
                        new InstanceIdentity();
                        new RawHtmlMarkupFormatter();
                        new MatrixProject();
                    }
                }
                """)));
    }
}
