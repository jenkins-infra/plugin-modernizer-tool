package io.jenkins.tools.pluginmodernizer.core.recipes;

import static org.openrewrite.maven.Assertions.pomXml;
import static org.openrewrite.test.SourceSpecs.text;

import io.jenkins.tools.pluginmodernizer.core.extractor.ArchetypeCommonFile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.openrewrite.test.RewriteTest;

class AddCodeOwnerTest implements RewriteTest {

    @Test
    @EnabledOnOs(OS.LINUX)
    void addCodeOwner() {
        rewriteRun(
                spec -> spec.recipe(new AddCodeOwner("empty")),
                pomXml("""
                        <?xml version="1.0" encoding="UTF-8"?>
                        <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
                          <modelVersion>4.0.0</modelVersion>
                          <groupId>io.jenkins.plugins</groupId>
                          <artifactId>empty</artifactId>
                          <version>1.0.0-SNAPSHOT</version>
                          <name>Empty pom</name>
                        </project>
                        """),
                text(null, "* @jenkinsci/empty-plugin-developers", sourceSpecs -> {
                    sourceSpecs.path(ArchetypeCommonFile.CODEOWNERS.getPath());
                }));
    }

    @Test
    @EnabledOnOs(OS.LINUX)
    void addCodeOwnerForExcludedPlugin() {
        rewriteRun(
                spec -> spec.recipe(new AddCodeOwner("bitbucket-oauth")),
                pomXml("""
                        <?xml version="1.0" encoding="UTF-8"?>
                        <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
                          <modelVersion>4.0.0</modelVersion>
                          <groupId>org.jenkins-ci.plugins</groupId>
                          <artifactId>bitbucket-oauth</artifactId>
                          <version>1.0.0-SNAPSHOT</version>
                          <packaging>hpi</packaging>
                        </project>
                        """),
                text(null, "* @jenkinsci/bitbucket-oauth-plugin-developers", sourceSpecs -> {
                    sourceSpecs.path(ArchetypeCommonFile.CODEOWNERS.getPath());
                }));
    }

    @Test
    @EnabledOnOs(OS.LINUX)
    void addCodeOwnerForMultiModulePlugin() {
        rewriteRun(
                spec -> spec.recipe(new AddCodeOwner("openstack-cloud")),
                pomXml("""
                        <?xml version="1.0" encoding="UTF-8"?>
                        <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
                          <modelVersion>4.0.0</modelVersion>
                          <groupId>io.jenkins.plugins</groupId>
                          <artifactId>parent</artifactId>
                          <version>1.0.0-SNAPSHOT</version>
                          <packaging>pom</packaging>
                        </project>
                        """),
                pomXml("""
                        <?xml version="1.0" encoding="UTF-8"?>
                        <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
                          <modelVersion>4.0.0</modelVersion>
                          <groupId>io.jenkins.plugins</groupId>
                          <artifactId>openstack-cloud</artifactId>
                          <version>1.0.0-SNAPSHOT</version>
                          <packaging>hpi</packaging>
                        </project>
                        """, sourceSpecs -> sourceSpecs.path("plugin/pom.xml")),
                text(null, "* @jenkinsci/openstack-cloud-plugin-developers", sourceSpecs -> {
                    sourceSpecs.path(ArchetypeCommonFile.CODEOWNERS.getPath());
                }));
    }

    @Test
    @EnabledOnOs(OS.LINUX)
    void shouldNotAddCodeOwnerIfAdded() {
        rewriteRun(
                spec -> spec.recipe(new AddCodeOwner("empty")),
                pomXml("""
                        <?xml version="1.0" encoding="UTF-8"?>
                        <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
                          <modelVersion>4.0.0</modelVersion>
                          <groupId>io.jenkins.plugins</groupId>
                          <artifactId>empty</artifactId>
                          <version>1.0.0-SNAPSHOT</version>
                          <name>Empty pom</name>
                        </project>
                        """),
                text("* @jenkinsci/empty-plugin-developers", sourceSpecs -> {
                    sourceSpecs.path(ArchetypeCommonFile.CODEOWNERS.getPath());
                }));
    }

    @Test
    @EnabledOnOs(OS.LINUX)
    void shouldAddCodeOwnerIfNeeded() {
        rewriteRun(spec -> spec.recipe(new AddCodeOwner("empty")), pomXml("""
                        <?xml version="1.0" encoding="UTF-8"?>
                        <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
                          <modelVersion>4.0.0</modelVersion>
                          <groupId>io.jenkins.plugins</groupId>
                          <artifactId>empty</artifactId>
                          <version>1.0.0-SNAPSHOT</version>
                          <name>Empty pom</name>
                        </project>
                        """), text("""
                        * @my-custom-team
                        """, """
                        * @jenkinsci/empty-plugin-developers
                        * @my-custom-team
                    """, sourceSpecs -> {
            sourceSpecs.path(ArchetypeCommonFile.CODEOWNERS.getPath());
        }));
    }
}
