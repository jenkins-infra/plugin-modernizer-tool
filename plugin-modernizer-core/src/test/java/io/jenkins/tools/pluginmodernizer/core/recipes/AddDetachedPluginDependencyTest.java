package io.jenkins.tools.pluginmodernizer.core.recipes;

import static org.openrewrite.java.Assertions.*;
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
    void detectsDetachedPluginUsageAndAddsDependency() {
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
                                """
                    package hudson.maven;
                    public class MavenModuleSet {}
                    """),
                        java(
                                """
                    package hudson.scm;
                    public class SubversionSCM {}
                    """),
                        java(
                                """
                    package hudson.tasks;
                    public class Ant {}
                    """),
                        java(
                                """
                    package hudson.tasks;
                    public class JavadocArchiver {}
                    """),
                        java(
                                """
                    package hudson.tasks;
                    public class Mailer {}
                    """),
                        java(
                                """
                    package hudson.tasks.junit;
                    public class JUnitResultArchiver {}
                    """),
                        java(
                                """
                    package hudson.model;
                    public class ExternalJob {}
                    """),
                        java(
                                """
                    package hudson.security;
                    public class LDAPSecurityRealm {}
                    """),
                        java(
                                """
                    package hudson.security;
                    public class PAMSecurityRealm {}
                    """),
                        java(
                                """
                    package hudson.security;
                    public class GlobalMatrixAuthorizationStrategy {}
                    """),
                        java(
                                """
                    package hudson.security;
                    public class ProjectMatrixAuthorizationStrategy {}
                    """),
                        java(
                                """
                    package hudson.security;
                    public class AuthorizationMatrixProperty {}
                    """),
                        java(
                                """
                    package hudson.slaves;
                    public class CommandLauncher {}
                    """),
                        java(
                                """
                    package hudson.tools;
                    public class JDKInstaller {}
                    """),
                        java(
                                """
                    package javax.xml.bind;
                    public class JAXBContext {}
                    """),
                        java(
                                """
                    package com.trilead.ssh2;
                    public class Connection {}
                    """),
                        java(
                                """
                    package org.jenkinsci.main.modules.sshd;
                    public class SSHD {}
                    """),
                        java(
                                """
                    package javax.activation;
                    public class DataHandler {}
                    """),
                        java(
                                """
                    package jenkins.bouncycastle.api;
                    public class BouncyCastlePlugin {}
                    """),
                        java(
                                """
                    package jenkins.plugins.javax.activation;
                    public class CommandMapInitializer {}
                    """),
                        java(
                                """
                    package jenkins.plugins.javax.activation;
                    public class FileTypeMapInitializer {}
                    """),
                        java(
                                """
                    package org.jenkinsci.main.modules.instance_identity;
                    public class InstanceIdentity {}
                    """),
                        java(
                                """
                    package hudson.markup;
                    public class RawHtmlMarkupFormatter {}
                    """),
                        java(
                                """
                    package hudson.matrix;
                    public class MatrixProject {}
                    """)),
                srcMainJava(
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
}
