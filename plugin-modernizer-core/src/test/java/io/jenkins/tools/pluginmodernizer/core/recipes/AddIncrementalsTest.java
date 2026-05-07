package io.jenkins.tools.pluginmodernizer.core.recipes;

import static org.openrewrite.maven.Assertions.pomXml;
import static org.openrewrite.test.SourceSpecs.text;
import static org.openrewrite.xml.Assertions.xml;

import io.jenkins.tools.pluginmodernizer.core.config.Settings;
import io.jenkins.tools.pluginmodernizer.core.extractor.ArchetypeCommonFile;
import io.jenkins.tools.pluginmodernizer.core.utils.Utils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.openrewrite.test.RewriteTest;

/**
 * Test for {@link AddIncrementals}.
 */
@Execution(ExecutionMode.CONCURRENT)
class AddIncrementalsTest implements RewriteTest {

    @Test
    void shouldAddIncrementalsToBasicPlugin() {
        rewriteRun(
                spec -> spec.executionContext(Utils.getMavenExecutionContext()).recipe(new AddIncrementals()),
                // language=xml
                pomXml("""
                    <?xml version="1.0" encoding="UTF-8"?>
                    <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                            xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
                        <modelVersion>4.0.0</modelVersion>
                        <parent>
                            <groupId>org.jenkins-ci.plugins</groupId>
                            <artifactId>plugin</artifactId>
                            <version>4.87</version>
                            <relativePath />
                        </parent>
                        <artifactId>test-plugin</artifactId>
                        <version>1.0.0-SNAPSHOT</version>
                        <packaging>hpi</packaging>
                        <name>Test Plugin</name>
                        <url>https://github.com/jenkinsci/test-plugin</url>
                        <scm>
                            <connection>scm:git:https://github.com/jenkinsci/test-plugin.git</connection>
                            <developerConnection>scm:git:git@github.com:jenkinsci/test-plugin.git</developerConnection>
                            <tag>HEAD</tag>
                            <url>https://github.com/jenkinsci/test-plugin</url>
                        </scm>
                        <properties>
                            <jenkins.version>2.452.4</jenkins.version>
                        </properties>
                        <repositories>
                            <repository>
                                <id>repo.jenkins-ci.org</id>
                                <url>https://repo.jenkins-ci.org/public/</url>
                            </repository>
                        </repositories>
                    </project>
                    """, """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                            xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
                        <modelVersion>4.0.0</modelVersion>
                        <parent>
                            <groupId>org.jenkins-ci.plugins</groupId>
                            <artifactId>plugin</artifactId>
                            <version>4.87</version>
                            <relativePath />
                        </parent>
                        <artifactId>test-plugin</artifactId>
                        <version>${revision}${changelist}</version>
                        <packaging>hpi</packaging>
                        <name>Test Plugin</name>
                        <url>https://github.com/${gitHubRepo}</url>
                        <scm>
                            <connection>scm:git:https://github.com/${gitHubRepo}.git</connection>
                            <developerConnection>scm:git:git@github.com:${gitHubRepo}.git</developerConnection>
                            <tag>${scmTag}</tag>
                            <url>https://github.com/${gitHubRepo}</url>
                        </scm>
                        <properties>
                            <jenkins.version>2.452.4</jenkins.version>
                            <revision>1.0.0</revision>
                            <changelist>-SNAPSHOT</changelist>
                            <gitHubRepo>jenkinsci/test-plugin</gitHubRepo>
                            <scmTag>HEAD</scmTag>
                        </properties>
                        <repositories>
                            <repository>
                                <id>repo.jenkins-ci.org</id>
                                <url>https://repo.jenkins-ci.org/public/</url>
                            </repository>
                        </repositories>
                    </project>
                    """),
                text(
                        null,
                        "-Pconsume-incrementals\n-Pmight-produce-incrementals\n",
                        spec -> spec.path(ArchetypeCommonFile.MAVEN_CONFIG.getPath())),
                xml(
                        null,
                        """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <extensions xmlns="http://maven.apache.org/EXTENSIONS/1.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/EXTENSIONS/1.0.0 http://maven.apache.org/xsd/core-extensions-1.0.0.xsd">
                      <extension>
                        <groupId>io.jenkins.tools.incrementals</groupId>
                        <artifactId>git-changelist-maven-extension</artifactId>
                        <version>%s</version>
                      </extension>
                    </extensions>
                    """.formatted(Settings.getIncrementalExtensionVersion()),
                        spec -> spec.path(ArchetypeCommonFile.MAVEN_EXTENSIONS.getPath())));
    }

    @Test
    void shouldNotModifyIfAlreadyUsingIncrementals() {
        rewriteRun(
                spec -> spec.executionContext(Utils.getMavenExecutionContext()).recipe(new AddIncrementals()),
                // language=xml
                pomXml("""
                    <?xml version="1.0" encoding="UTF-8"?>
                    <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                            xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
                        <modelVersion>4.0.0</modelVersion>
                        <parent>
                            <groupId>org.jenkins-ci.plugins</groupId>
                            <artifactId>plugin</artifactId>
                            <version>4.87</version>
                            <relativePath />
                        </parent>
                        <artifactId>test-plugin</artifactId>
                        <version>${revision}${changelist}</version>
                        <packaging>hpi</packaging>
                        <name>Test Plugin</name>
                        <url>https://github.com/${gitHubRepo}</url>
                        <scm>
                            <connection>scm:git:https://github.com/${gitHubRepo}.git</connection>
                            <developerConnection>scm:git:git@github.com:${gitHubRepo}.git</developerConnection>
                            <tag>${scmTag}</tag>
                            <url>https://github.com/${gitHubRepo}</url>
                        </scm>
                        <properties>
                            <revision>1.0.0</revision>
                            <changelist>-SNAPSHOT</changelist>
                            <gitHubRepo>jenkinsci/test-plugin</gitHubRepo>
                            <scmTag>HEAD</scmTag>
                        </properties>
                        <repositories>
                            <repository>
                                <id>repo.jenkins-ci.org</id>
                                <url>https://repo.jenkins-ci.org/public/</url>
                            </repository>
                        </repositories>
                    </project>
                    """),
                text("existing content", spec -> spec.path(ArchetypeCommonFile.MAVEN_CONFIG.getPath())),
                text("existing content", spec -> spec.path(ArchetypeCommonFile.MAVEN_EXTENSIONS.getPath())));
    }

    @Test
    void shouldNotCreateFilesIfAlreadyExist() {
        rewriteRun(
                spec -> spec.executionContext(Utils.getMavenExecutionContext()).recipe(new AddIncrementals()),
                // language=xml
                pomXml("""
                    <?xml version="1.0" encoding="UTF-8"?>
                    <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                            xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
                        <modelVersion>4.0.0</modelVersion>
                        <parent>
                            <groupId>org.jenkins-ci.plugins</groupId>
                            <artifactId>plugin</artifactId>
                            <version>4.87</version>
                            <relativePath />
                        </parent>
                        <groupId>io.jenkins.plugins</groupId>
                        <artifactId>test-plugin</artifactId>
                        <version>1.0-SNAPSHOT</version>
                        <packaging>hpi</packaging>
                        <scm>
                            <connection>scm:git:https://github.com/jenkinsci/test-plugin.git</connection>
                            <developerConnection>scm:git:git@github.com:jenkinsci/test-plugin.git</developerConnection>
                            <tag>HEAD</tag>
                        </scm>
                        <properties>
                            <jenkins.version>2.452.4</jenkins.version>
                        </properties>
                        <repositories>
                            <repository>
                                <id>repo.jenkins-ci.org</id>
                                <url>https://repo.jenkins-ci.org/public/</url>
                            </repository>
                        </repositories>
                    </project>
                    """, """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                            xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
                        <modelVersion>4.0.0</modelVersion>
                        <parent>
                            <groupId>org.jenkins-ci.plugins</groupId>
                            <artifactId>plugin</artifactId>
                            <version>4.87</version>
                            <relativePath />
                        </parent>
                        <groupId>io.jenkins.plugins</groupId>
                        <artifactId>test-plugin</artifactId>
                        <version>${revision}${changelist}</version>
                        <packaging>hpi</packaging>
                        <scm>
                            <connection>scm:git:https://github.com/${gitHubRepo}.git</connection>
                            <developerConnection>scm:git:git@github.com:${gitHubRepo}.git</developerConnection>
                            <tag>${scmTag}</tag>
                        </scm>
                        <properties>
                            <jenkins.version>2.452.4</jenkins.version>
                            <revision>1.0</revision>
                            <changelist>-SNAPSHOT</changelist>
                            <gitHubRepo>jenkinsci/test-plugin</gitHubRepo>
                            <scmTag>HEAD</scmTag>
                        </properties>
                        <repositories>
                            <repository>
                                <id>repo.jenkins-ci.org</id>
                                <url>https://repo.jenkins-ci.org/public/</url>
                            </repository>
                        </repositories>
                    </project>
                    """),
                text("-Pexisting", spec -> spec.path(ArchetypeCommonFile.MAVEN_CONFIG.getPath())),
                xml("<extensions></extensions>", spec -> spec.path(ArchetypeCommonFile.MAVEN_EXTENSIONS.getPath())));
    }
}
