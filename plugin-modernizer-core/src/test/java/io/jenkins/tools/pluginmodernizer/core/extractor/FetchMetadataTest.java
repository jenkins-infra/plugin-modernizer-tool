package io.jenkins.tools.pluginmodernizer.core.extractor;

import static org.junit.jupiter.api.Assertions.*;
import static org.openrewrite.groovy.Assertions.groovy;
import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.maven.Assertions.pomXml;
import static org.openrewrite.test.SourceSpecs.text;
import static org.openrewrite.yaml.Assertions.yaml;

import io.jenkins.tools.pluginmodernizer.core.model.JDK;
import io.jenkins.tools.pluginmodernizer.core.model.Platform;
import io.jenkins.tools.pluginmodernizer.core.recipes.FetchMetadata;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openrewrite.test.RewriteTest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tests for {@link FetchMetadata}.
 */
public class FetchMetadataTest implements RewriteTest {

    private static final Logger LOG = LoggerFactory.getLogger(FetchMetadataTest.class);

    private static final PluginMetadata EXPECTED_METADATA;

    static {
        EXPECTED_METADATA = new PluginMetadata();
        EXPECTED_METADATA.setPluginName("GitLab Plugin");
        EXPECTED_METADATA.setParentVersion("4.80");
        EXPECTED_METADATA.setJenkinsVersion("2.426.3");
        EXPECTED_METADATA.setBomArtifactId("bom-2.414.x");
        EXPECTED_METADATA.setBomVersion("2950.va_633b_f42f759");
        EXPECTED_METADATA.setUseContainerAgent(null);
        EXPECTED_METADATA.setForkCount(null);
        Map<String, String> properties = new LinkedHashMap<>();
        properties.put("revision", "1.8.1");
        properties.put("java.level", "8");
        properties.put("changelist", "-SNAPSHOT");
        properties.put("jenkins.version", "2.426.3");
        properties.put("spotbugs.effort", "Max");
        properties.put("spotbugs.threshold", "Low");
        properties.put("gitHubRepo", "jenkinsci/${project.artifactId}");
        properties.put("hpi.compatibleSinceVersion", "1.4.0");
        properties.put("mockserver.version", "5.15.0");
        properties.put("spotless.check.skip", "false");
        EXPECTED_METADATA.setProperties(properties);
        List<ArchetypeCommonFile> commonFiles = new LinkedList<>();
        commonFiles.add(ArchetypeCommonFile.JENKINSFILE);
        commonFiles.add(ArchetypeCommonFile.POM);
        EXPECTED_METADATA.setCommonFiles(commonFiles);
        Set<MetadataFlag> flags = new LinkedHashSet<>();
        flags.add(MetadataFlag.DEVELOPER_SET);
        flags.add(MetadataFlag.LICENSE_SET);
        flags.add(MetadataFlag.SCM_HTTPS);
        flags.add(MetadataFlag.MAVEN_REPOSITORIES_HTTPS);
        EXPECTED_METADATA.setFlags(flags);
    }

    @Language("xml")
    private static final String POM_XML =
            """
                        <?xml version="1.0" encoding="UTF-8"?>
                        <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/maven-v4_0_0.xsd">
                          <modelVersion>4.0.0</modelVersion>
                          <parent>
                            <groupId>org.jenkins-ci.plugins</groupId>
                            <artifactId>plugin</artifactId>
                            <version>4.80</version>
                            <relativePath />
                          </parent>

                          <artifactId>gitlab-plugin</artifactId>
                          <version>${revision}${changelist}</version>
                          <packaging>hpi</packaging>
                          <name>GitLab Plugin</name>
                          <url>https://github.com/jenkinsci/${project.artifactId}</url>
                          <developers>
                            <developer>
                              <id>john.doe</id>
                              <name>John Doe</name>
                              <email>john.doe@example.com</email>
                            </developer>
                          </developers>
                          <licenses>
                            <license>
                              <name>GPL v2.0 License</name>
                              <url>http://www.gnu.org/licenses/old-licenses/gpl-2.0-standalone.html</url>
                            </license>
                          </licenses>
                          <scm>
                            <connection>scm:git:https://github.com/${gitHubRepo}.git</connection>
                            <developerConnection>scm:git:git@github.com:${gitHubRepo}.git</developerConnection>
                            <tag>${scmTag}</tag>
                            <url>https://github.com/${gitHubRepo}</url>
                          </scm>
                          <distributionManagement>
                            <repository>
                              <id>maven.jenkins-ci.org</id>
                              <name>jenkinsci-releases</name>
                              <url>https://repo.jenkins-ci.org/releases</url>
                            </repository>
                            <snapshotRepository>
                              <id>maven.jenkins-ci.org</id>
                              <name>jenkinsci-snapshots</name>
                              <url>https://repo.jenkins-ci.org/snapshots</url>
                            </snapshotRepository>
                          </distributionManagement>

                          <properties>
                            <revision>1.8.1</revision>
                            <java.level>8</java.level>
                            <changelist>-SNAPSHOT</changelist>
                            <jenkins.version>2.426.3</jenkins.version>
                            <spotbugs.effort>Max</spotbugs.effort>
                            <spotbugs.threshold>Low</spotbugs.threshold>
                            <gitHubRepo>jenkinsci/${project.artifactId}</gitHubRepo>
                            <hpi.compatibleSinceVersion>1.4.0</hpi.compatibleSinceVersion>
                            <mockserver.version>5.15.0</mockserver.version>
                            <spotless.check.skip>false</spotless.check.skip>
                          </properties>

                          <dependencyManagement>
                            <dependencies>
                              <dependency>
                                <!-- Pick up common dependencies for the selected LTS line: https://github.com/jenkinsci/bom#usage -->
                                <groupId>io.jenkins.tools.bom</groupId>
                                <artifactId>bom-2.414.x</artifactId>
                                <version>2950.va_633b_f42f759</version>
                                <type>pom</type>
                                <scope>import</scope>
                              </dependency>
                            </dependencies>
                          </dependencyManagement>

                          <dependencies>
                            <dependency>
                              <groupId>io.jenkins.plugins</groupId>
                              <artifactId>caffeine-api</artifactId>
                            </dependency>
                            <dependency>
                              <groupId>org.jboss.resteasy</groupId>
                              <artifactId>resteasy-client</artifactId>
                              <version>3.15.6.Final</version>
                              <exclusions>
                                <!-- Provided by Jenkins core -->
                                <exclusion>
                                  <groupId>com.github.stephenc.jcip</groupId>
                                  <artifactId>jcip-annotations</artifactId>
                                </exclusion>
                              </exclusions>
                            </dependency>
                            <dependency>
                              <groupId>org.jenkins-ci.plugins</groupId>
                              <artifactId>credentials</artifactId>
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
                        """;

    @BeforeEach
    void cleanupTarget() {
        new PluginMetadata().delete();
    }

    @Test
    void testWithPomOnly() throws Exception {
        rewriteRun(recipeSpec -> recipeSpec.recipe(new FetchMetadata()), pomXml(POM_XML));

        PluginMetadata pluginMetadata = new PluginMetadata().refresh();
        assertNotNull(pluginMetadata, "Plugin metadata was not written by the recipe");
        assertTrue(pluginMetadata.hasFile(ArchetypeCommonFile.POM));
        Set<JDK> jdkVersion = pluginMetadata.getJdks();
        assertEquals(0, jdkVersion.size());
        assertEquals(EXPECTED_METADATA.getParentVersion(), pluginMetadata.getParentVersion());
        assertEquals(EXPECTED_METADATA.getPluginName(), pluginMetadata.getPluginName());
        assertEquals(EXPECTED_METADATA.getJenkinsVersion(), pluginMetadata.getJenkinsVersion());
        assertEquals(EXPECTED_METADATA.getBomVersion(), pluginMetadata.getBomVersion());
        assertEquals(EXPECTED_METADATA.getProperties(), pluginMetadata.getProperties());
        assertEquals(EXPECTED_METADATA.getFlags(), pluginMetadata.getFlags());

        // Only pom here
        assertEquals(List.of(ArchetypeCommonFile.POM), pluginMetadata.getCommonFiles());
    }

    @Test
    void testWithDifferentParent() throws Exception {
        rewriteRun(
                recipeSpec -> recipeSpec.recipe(new FetchMetadata()),
                // language=xml
                pomXml(
                        """
                 <?xml version="1.0" encoding="UTF-8"?>
                 <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
                   <modelVersion>4.0.0</modelVersion>
                   <parent>
                     <groupId>org.jvnet.hudson.plugins</groupId>
                     <artifactId>analysis-pom</artifactId>
                     <version>10.0.0</version>
                     <relativePath />
                   </parent>
                   <groupId>io.jenkins.plugins</groupId>
                   <artifactId>check</artifactId>
                   <version>1.0.0-SNAPSHOT</version>
                   <packaging>hpi</packaging>
                   <name>Check Plugin</name>
                   <repositories>
                     <repository>
                       <id>repo.jenkins-ci.org</id>
                       <url>https://repo.jenkins-ci.org/public/</url>
                     </repository>
                   </repositories>
                 </project>
                 """));

        PluginMetadata pluginMetadata = new PluginMetadata().refresh();
        assertNotNull(pluginMetadata, "Plugin metadata was not written by the recipe");
        assertTrue(pluginMetadata.hasFile(ArchetypeCommonFile.POM));

        // Check metadata
        assertEquals("5.2", pluginMetadata.getParentVersion());
        assertEquals("Check Plugin", pluginMetadata.getPluginName());
        assertEquals("2.479.1", pluginMetadata.getJenkinsVersion());
        assertEquals("3654.v237e4a_f2d8da_", pluginMetadata.getBomVersion());

        // Only pom here
        assertEquals(List.of(ArchetypeCommonFile.POM), pluginMetadata.getCommonFiles());
    }

    @Test
    void testWithOneCommonFile() throws Exception {
        rewriteRun(
                recipeSpec -> recipeSpec.recipe(new FetchMetadata()),
                // language=yaml
                yaml("""
                ---
                name: Empty
                """, sourceSpecs -> {
                    sourceSpecs.path(".github/dependabot.yml");
                }));

        PluginMetadata pluginMetadata = new PluginMetadata().refresh();
        assertNotNull(pluginMetadata, "Plugin metadata was not written by the recipe");
        // Only dependabot.yml here
        assertEquals(List.of(ArchetypeCommonFile.DEPENDABOT), pluginMetadata.getCommonFiles());
    }

    @Test
    void testWithPomAndJavaFile() throws Exception {
        rewriteRun(
                recipeSpec -> recipeSpec.recipe(new FetchMetadata()),
                pomXml(POM_XML),
                java(
                        """
                            package com.uppercase.camelcase;
                            class FooBar {}
                        """));

        PluginMetadata pluginMetadata = new PluginMetadata().refresh();
        assertNotNull(pluginMetadata, "Plugin metadata was not written by the recipe");
        assertTrue(pluginMetadata.hasFile(ArchetypeCommonFile.POM));
        Set<JDK> jdkVersion = pluginMetadata.getJdks();
        assertEquals(0, jdkVersion.size());
        assertEquals(EXPECTED_METADATA.getParentVersion(), pluginMetadata.getParentVersion());
        assertEquals(EXPECTED_METADATA.getPluginName(), pluginMetadata.getPluginName());
        assertEquals(EXPECTED_METADATA.getJenkinsVersion(), pluginMetadata.getJenkinsVersion());
        assertEquals(EXPECTED_METADATA.getBomVersion(), pluginMetadata.getBomVersion());
        assertEquals(EXPECTED_METADATA.getProperties(), pluginMetadata.getProperties());
        assertEquals(EXPECTED_METADATA.getFlags(), pluginMetadata.getFlags());

        // Only pom here
        assertEquals(List.of(ArchetypeCommonFile.POM), pluginMetadata.getCommonFiles());
    }

    @Test
    void testWithManyCommonFiles() throws Exception {
        rewriteRun(
                recipeSpec -> recipeSpec.recipe(new FetchMetadata()),
                pomXml(POM_XML),
                // language=groovy
                groovy(
                        """
                         buildPlugin(
                         useContainerAgent: true,
                         configurations: [
                                [platform: 'linux', jdk: 21],
                                [platform: 'windows', jdk: 17],
                         ])
                         """,
                        spec -> spec.path("Jenkinsfile")),
                // language=java
                java(
                        """
                            package com.uppercase.camelcase;
                            class FooBar {}
                        """),
                // language=yaml
                yaml("""
                ---
                name: Empty
                """, sourceSpecs -> {
                    sourceSpecs.path(".github/dependabot.yml");
                }),
                // language=text
                text("The readme", sourceSpecs -> {
                    sourceSpecs.path("README.md");
                }),
                text("The contributing", sourceSpecs -> {
                    sourceSpecs.path("CONTRIBUTING.md");
                }),
                text("The maven config", sourceSpecs -> {
                    sourceSpecs.path(".mvn/maven.config");
                }),
                text("The license", sourceSpecs -> {
                    sourceSpecs.path("LICENSE.md");
                }),
                text("index jelly", sourceSpecs -> {
                    sourceSpecs.path("src/main/resources/index.jelly");
                }));

        PluginMetadata pluginMetadata = new PluginMetadata().refresh();
        assertNotNull(pluginMetadata, "Plugin metadata was not written by the recipe");

        // Check common files
        assertTrue(pluginMetadata.hasFile(ArchetypeCommonFile.POM), "POM file is missing");
        assertTrue(pluginMetadata.hasFile(ArchetypeCommonFile.JENKINSFILE), "Jenkinsfile is missing");
        assertTrue(pluginMetadata.hasFile(ArchetypeCommonFile.DEPENDABOT), "Dependabot is missing");
        assertTrue(pluginMetadata.hasFile(ArchetypeCommonFile.README), "README is missing");
        assertTrue(pluginMetadata.hasFile(ArchetypeCommonFile.CONTRIBUTING), "CONTRIBUTING is missing");
        assertTrue(pluginMetadata.hasFile(ArchetypeCommonFile.MAVEN_CONFIG), "Maven config is missing");
        assertTrue(pluginMetadata.hasFile(ArchetypeCommonFile.LICENSE), "License is missing");
        assertTrue(pluginMetadata.hasFile(ArchetypeCommonFile.INDEX_JELLY), "Index jelly is missing");

        // Check rest
        Set<JDK> jdkVersion = pluginMetadata.getJdks();
        assertEquals(2, jdkVersion.size());
        assertTrue(pluginMetadata.isUseContainerAgent());
        assertEquals(EXPECTED_METADATA.getParentVersion(), pluginMetadata.getParentVersion());
        assertEquals(EXPECTED_METADATA.getPluginName(), pluginMetadata.getPluginName());
        assertEquals(EXPECTED_METADATA.getJenkinsVersion(), pluginMetadata.getJenkinsVersion());
        assertEquals(EXPECTED_METADATA.getBomVersion(), pluginMetadata.getBomVersion());
        assertEquals(EXPECTED_METADATA.getProperties(), pluginMetadata.getProperties());
        assertEquals(EXPECTED_METADATA.getFlags(), pluginMetadata.getFlags());
    }

    @Test
    void testWithJenkinsfileOnly() throws Exception {
        rewriteRun(
                recipeSpec -> recipeSpec.recipe(new FetchMetadata()),
                // language=groovy
                groovy(
                        """
                         buildPlugin(
                         useContainerAgent: false,
                         forkCount: '1C',
                         configurations: [
                                [platform: 'linux', jdk: 21],
                                [platform: 'windows', jdk: 17],
                         ])
                         """,
                        spec -> spec.path("Jenkinsfile")));

        PluginMetadata pluginMetadata = new PluginMetadata().refresh();
        assertNotNull(pluginMetadata, "Plugin metadata was not written by the recipe");
        // Only Jenkinsfile here
        assertEquals(List.of(ArchetypeCommonFile.JENKINSFILE), pluginMetadata.getCommonFiles());

        // Assert JDK
        Set<JDK> jdkVersion = pluginMetadata.getJdks();
        assertEquals(2, jdkVersion.size());
        assertFalse(pluginMetadata.isUseContainerAgent());
        assertEquals("1C", pluginMetadata.getForkCount());

        // Assert platform
        Set<Platform> platforms = pluginMetadata.getPlatforms();
        assertEquals(2, platforms.size());
        assertTrue(platforms.contains(Platform.WINDOWS));
        assertTrue(platforms.contains(Platform.LINUX));
    }

    @Test
    void testPluginWithJenkinsfileWithJdkInfoConfiguration() {
        rewriteRun(
                recipeSpec -> recipeSpec.recipe(new FetchMetadata()),
                // language=groovy
                groovy(
                        """
                         buildPlugin(
                         useContainerAgent: true,
                         configurations: [
                                [platform: 'linux', jdk: 21],
                                [platform: 'windows', jdk: 17],
                         ])
                         """,
                        spec -> spec.path("Jenkinsfile")),
                pomXml(POM_XML));

        PluginMetadata pluginMetadata = new PluginMetadata().refresh();
        // Files are present
        assertTrue(pluginMetadata.hasFile(ArchetypeCommonFile.JENKINSFILE));
        assertTrue(pluginMetadata.hasFile(ArchetypeCommonFile.POM));

        // Assert JDK
        Set<JDK> jdkVersion = pluginMetadata.getJdks();
        assertEquals(2, jdkVersion.size());
        assertTrue(jdkVersion.contains(JDK.JAVA_21));
        assertTrue(jdkVersion.contains(JDK.JAVA_17));
        assertTrue(pluginMetadata.isUseContainerAgent());

        // Assert platform
        Set<Platform> platforms = pluginMetadata.getPlatforms();
        assertEquals(2, platforms.size());
        assertTrue(platforms.contains(Platform.WINDOWS));
        assertTrue(platforms.contains(Platform.LINUX));
    }

    @Test
    void testPluginWithJenkinsfileWithJdkInfoVersion() {
        rewriteRun(
                recipeSpec -> recipeSpec.recipe(new FetchMetadata()),
                // language=groovy
                groovy(
                        """
                         buildPlugin(
                             jdkVersions: [21, 17]
                         )
                         """,
                        spec -> spec.path("Jenkinsfile")));

        PluginMetadata pluginMetadata = new PluginMetadata().refresh();
        // Files are present
        assertTrue(pluginMetadata.hasFile(ArchetypeCommonFile.JENKINSFILE));

        // Assert JDK
        Set<JDK> jdkVersion = pluginMetadata.getJdks();
        assertEquals(2, jdkVersion.size());
        assertTrue(jdkVersion.contains(JDK.JAVA_21));
        assertTrue(jdkVersion.contains(JDK.JAVA_17));

        // Assert platform
        Set<Platform> platforms = pluginMetadata.getPlatforms();
        assertEquals(2, platforms.size());
        assertTrue(platforms.contains(Platform.WINDOWS));
        assertTrue(platforms.contains(Platform.LINUX));
    }

    @Test
    void testPluginWithJenkinsfileWithPlatformsOnly() {
        rewriteRun(
                recipeSpec -> recipeSpec.recipe(new FetchMetadata()),
                // language=groovy
                groovy(
                        """
                         buildPlugin(
                             platforms: ["linux", "windows"]
                         )
                         """,
                        spec -> spec.path("Jenkinsfile")));

        PluginMetadata pluginMetadata = new PluginMetadata().refresh();
        // Files are present
        assertTrue(pluginMetadata.hasFile(ArchetypeCommonFile.JENKINSFILE));

        // Assert JDK
        Set<JDK> jdkVersion = pluginMetadata.getJdks();
        assertEquals(1, jdkVersion.size());
        assertTrue(jdkVersion.contains(JDK.JAVA_8));

        // Assert platform
        Set<Platform> platforms = pluginMetadata.getPlatforms();
        assertEquals(2, platforms.size());
        assertTrue(platforms.contains(Platform.WINDOWS));
        assertTrue(platforms.contains(Platform.LINUX));
    }

    @Test
    void testPluginWithJenkinsfileWithJdkInfoVersionAndPlatform() {
        rewriteRun(
                recipeSpec -> recipeSpec.recipe(new FetchMetadata()),
                // language=groovy
                groovy(
                        """
                         buildPlugin(
                             platforms: ['linux'],
                             jdkVersions: [21, 17]
                         )
                         """,
                        spec -> spec.path("Jenkinsfile")));

        PluginMetadata pluginMetadata = new PluginMetadata().refresh();
        // Files are present
        assertTrue(pluginMetadata.hasFile(ArchetypeCommonFile.JENKINSFILE));

        // Assert JDK
        Set<JDK> jdkVersion = pluginMetadata.getJdks();
        assertEquals(2, jdkVersion.size());
        assertTrue(jdkVersion.contains(JDK.JAVA_21));
        assertTrue(jdkVersion.contains(JDK.JAVA_17));

        // Assert platform
        Set<Platform> platforms = pluginMetadata.getPlatforms();
        assertEquals(1, platforms.size());
        assertTrue(platforms.contains(Platform.LINUX));
    }

    @Test
    void testPluginWithJenkinsfileWithJdkInfoVersionVar() {
        rewriteRun(
                recipeSpec -> recipeSpec.recipe(new FetchMetadata()),
                // language=groovy
                groovy(
                        """
                         def versions = [21, 17]
                         buildPlugin(
                             jdkVersions: versions
                         )
                         """,
                        spec -> spec.path("Jenkinsfile")));

        PluginMetadata pluginMetadata = new PluginMetadata().refresh();
        // Files are present
        assertTrue(pluginMetadata.hasFile(ArchetypeCommonFile.JENKINSFILE));

        Set<JDK> jdkVersion = pluginMetadata.getJdks();
        assertEquals(2, jdkVersion.size());
        assertTrue(jdkVersion.contains(JDK.JAVA_21));
        assertTrue(jdkVersion.contains(JDK.JAVA_17));
    }

    @Test
    // Keep in sync with https://github.com/jenkins-infra/pipeline-library with default JDK
    void testPluginWithJenkinsfileDefault() {

        // Keep in sync with https://github.com/jenkins-infra/pipeline-library with default JDK and 2 platforms
        EXPECTED_METADATA.addPlatform(Platform.LINUX, JDK.getImplicit(), null);
        EXPECTED_METADATA.addPlatform(Platform.WINDOWS, JDK.getImplicit(), null);

        rewriteRun(
                recipeSpec -> recipeSpec.recipe(new FetchMetadata()),
                // language=groovy
                groovy(
                        """
                         buildPlugin()
                         """,
                        spec -> spec.path("Jenkinsfile")));

        PluginMetadata pluginMetadata = new PluginMetadata().refresh();
        // Files are present
        assertTrue(pluginMetadata.hasFile(ArchetypeCommonFile.JENKINSFILE));

        // Assert JDK
        Set<JDK> jdkVersion = pluginMetadata.getJdks();
        assertEquals(1, jdkVersion.size());
        assertNull(pluginMetadata.isUseContainerAgent());
        assertTrue(jdkVersion.contains(JDK.getImplicit()));

        // Assert platform
        Set<Platform> platforms = pluginMetadata.getPlatforms();
        assertEquals(2, platforms.size());
        assertTrue(platforms.contains(Platform.WINDOWS));
        assertTrue(platforms.contains(Platform.LINUX));
    }

    @Test
    void testJenkinsfileWithConfigurationsAsParameter() {
        Set<JDK> jdks = new LinkedHashSet<>();
        jdks.add(JDK.JAVA_11);
        jdks.add(JDK.JAVA_17);
        EXPECTED_METADATA.addPlatform(Platform.LINUX, JDK.JAVA_11, null);
        EXPECTED_METADATA.addPlatform(Platform.WINDOWS, JDK.JAVA_17, null);
        rewriteRun(
                recipeSpec -> recipeSpec.recipe(new FetchMetadata()),
                // language=groovy
                groovy(
                        """
                            def configurations = [
                              [ platform: "linux", jdk: "11" ],
                              [ platform: "windows", jdk: "17" ]
                            ]

                            def params = [
                                failFast: false,
                                configurations: configurations,
                                checkstyle: [qualityGates: [[threshold: 1, type: 'NEW', unstable: true]]],
                                pmd: [qualityGates: [[threshold: 1, type: 'NEW', unstable: true]]],
                                jacoco: [sourceCodeRetention: 'MODIFIED']
                                ]

                            buildPlugin(params)
                            """,
                        spec -> spec.path("Jenkinsfile")),
                pomXml(POM_XML));
        PluginMetadata pluginMetadata = new PluginMetadata().refresh();
        assertTrue(pluginMetadata.hasFile(ArchetypeCommonFile.JENKINSFILE));

        // Assert JDK
        Set<JDK> jdkVersion = pluginMetadata.getJdks();
        assertEquals(2, jdkVersion.size());

        // Assert platform
        Set<Platform> platforms = pluginMetadata.getPlatforms();
        assertEquals(2, platforms.size());
        assertTrue(platforms.contains(Platform.WINDOWS));
        assertTrue(platforms.contains(Platform.LINUX));
    }

    @Test
    void testJenkinsfileWithPlatfomsAndJdkVersionsAsParameter() {
        Set<JDK> jdks = new LinkedHashSet<>();
        jdks.add(JDK.JAVA_17);
        jdks.add(JDK.JAVA_21);
        EXPECTED_METADATA.addPlatform(Platform.LINUX, JDK.JAVA_17, null);
        EXPECTED_METADATA.addPlatform(Platform.WINDOWS, JDK.JAVA_21, null);
        rewriteRun(
                recipeSpec -> recipeSpec.recipe(new FetchMetadata()),
                // language=groovy
                groovy(
                        """
                            def useContainerAgent = true
                            def platforms = ["linux", "windows"]
                            def params = [
                                platforms: platforms,
                                jdkVersions: [17, 21],
                                forkCount: '1C',
                                useContainerAgent: useContainerAgent,
                            ]
                            buildPlugin(params)
                            """,
                        spec -> spec.path("Jenkinsfile")),
                pomXml(POM_XML));
        PluginMetadata pluginMetadata = new PluginMetadata().refresh();
        assertTrue(pluginMetadata.hasFile(ArchetypeCommonFile.JENKINSFILE));

        // Assert other param
        assertNotNull(pluginMetadata.isUseContainerAgent());
        assertTrue(pluginMetadata.isUseContainerAgent());
        assertEquals("1C", pluginMetadata.getForkCount());

        // Assert JDK
        Set<JDK> jdkVersion = pluginMetadata.getJdks();
        assertEquals(2, jdkVersion.size());

        // Assert platform
        Set<Platform> platforms = pluginMetadata.getPlatforms();
        assertEquals(2, platforms.size());
        assertTrue(platforms.contains(Platform.WINDOWS));
        assertTrue(platforms.contains(Platform.LINUX));
    }

    @Test
    void testJenkinsfileWithConditional() {
        Set<JDK> jdks = new LinkedHashSet<>();
        jdks.add(JDK.JAVA_17);
        jdks.add(JDK.JAVA_21);
        EXPECTED_METADATA.addPlatform(Platform.LINUX, JDK.JAVA_17, null);
        EXPECTED_METADATA.addPlatform(Platform.WINDOWS, JDK.JAVA_21, null);
        rewriteRun(
                recipeSpec -> recipeSpec.recipe(new FetchMetadata()),
                // language=groovy
                groovy(
                        """
                    if (JENKINS_URL == 'https://ci.jenkins.io/') {
                      buildPlugin(
                        configurations: [
                          [ platform: "linux", jdk: "21" ],
                          [ platform: "linux", jdk: "17" ]
                        ],
                        useContainerAgent: true,
                        timeout: 90
                      )
                      return
                    }
                    node() {
                      // Not implemented
                    }
                    """,
                        spec -> spec.path("Jenkinsfile")),
                pomXml(POM_XML));
        PluginMetadata pluginMetadata = new PluginMetadata().refresh();
        assertTrue(pluginMetadata.hasFile(ArchetypeCommonFile.JENKINSFILE));

        // Assert other param
        assertNotNull(pluginMetadata.isUseContainerAgent());
        assertTrue(pluginMetadata.isUseContainerAgent());

        // Assert JDK
        Set<JDK> jdkVersion = pluginMetadata.getJdks();
        assertEquals(2, jdkVersion.size());

        // Assert platform
        Set<Platform> platforms = pluginMetadata.getPlatforms();
        assertEquals(1, platforms.size());
        assertTrue(platforms.contains(Platform.LINUX));
    }

    @Test
    void testJenkinsfileWithInlineConfigurations() {
        Set<JDK> jdks = new LinkedHashSet<>();
        jdks.add(JDK.JAVA_11);
        jdks.add(JDK.JAVA_17);
        EXPECTED_METADATA.addPlatform(Platform.LINUX, JDK.JAVA_11, null);
        EXPECTED_METADATA.addPlatform(Platform.WINDOWS, JDK.JAVA_17, null);
        rewriteRun(
                recipeSpec -> recipeSpec.recipe(new FetchMetadata()),
                // language=groovy
                groovy(
                        """
                            def forkCount = '1C'
                            def useContainerAgent = true
                            def configurations = [
                              [ platform: "linux", jdk: "11" ],
                              [ platform: "windows", jdk: "17" ]
                            ]

                            buildPlugin(
                                forkCount: forkCount,
                                useContainerAgent: useContainerAgent,
                                failFast: false,
                                configurations: configurations,
                                checkstyle: [qualityGates: [[threshold: 1, type: 'NEW', unstable: true]]],
                                pmd: [qualityGates: [[threshold: 1, type: 'NEW', unstable: true]]],
                                jacoco: [sourceCodeRetention: 'MODIFIED'])
                            """,
                        spec -> spec.path("Jenkinsfile")),
                pomXml(POM_XML));
        PluginMetadata pluginMetadata = new PluginMetadata().refresh();
        assertTrue(pluginMetadata.hasFile(ArchetypeCommonFile.JENKINSFILE));

        // Assert JDK
        Set<JDK> jdkVersion = pluginMetadata.getJdks();
        assertEquals(2, jdkVersion.size());

        // Assert platform
        Set<Platform> platforms = pluginMetadata.getPlatforms();
        assertEquals(2, platforms.size());
        assertTrue(platforms.contains(Platform.WINDOWS));
        assertTrue(platforms.contains(Platform.LINUX));

        assertNotNull(pluginMetadata.isUseContainerAgent());
        assertTrue(pluginMetadata.isUseContainerAgent());
        assertEquals("1C", pluginMetadata.getForkCount());
    }
}
