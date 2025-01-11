package io.jenkins.tools.pluginmodernizer.core.recipes;

import static org.openrewrite.java.Assertions.mavenProject;
import static org.openrewrite.java.Assertions.srcMainResources;
import static org.openrewrite.maven.Assertions.pomXml;
import static org.openrewrite.test.SourceSpecs.text;

import io.jenkins.tools.pluginmodernizer.core.extractor.ArchetypeCommonFile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.openrewrite.test.RewriteTest;

/**
 * Test for {@link EnsureIndexJelly}.
 */
@Execution(ExecutionMode.CONCURRENT)
public class EnsureIndexJellyTest implements RewriteTest {

    @Test
    void shouldNotReplaceJellyFile() {
        rewriteRun(spec -> spec.recipe(new EnsureIndexJelly()), text("jelly", sourceSpecs -> {
            sourceSpecs.path(ArchetypeCommonFile.INDEX_JELLY.getPath());
        }));
    }

    @Test
    void createFromDescription() {
        rewriteRun(
                spec -> spec.recipe(new EnsureIndexJelly()),
                // language=xml
                pomXml(
                        """
                          <project>
                              <parent>
                                  <groupId>org.jenkins-ci.plugins</groupId>
                                  <artifactId>plugin</artifactId>
                                  <version>4.88</version>
                              </parent>
                              <artifactId>empty</artifactId>
                              <description>The empty plugin</description>
                              <version>1.0.0-SNAPSHOT</version>
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
                        """
                          <?jelly escape-by-default='true'?>
                          <div>
                             The empty plugin
                          </div>
                          """,
                        s -> s.path(ArchetypeCommonFile.INDEX_JELLY.getPath())));
    }

    @Test
    void createNoCreateIfNotAPlugin() {
        rewriteRun(
                spec -> spec.recipe(new EnsureIndexJelly()),
                // language=xml
                pomXml(
                        """
                          <project>
                              <artifactId>not-plugin</artifactId>
                              <description>Not a plugin</description>
                              <groupId>org.example</groupId>
                              <version>1.0.0-SNAPSHOT</version>
                          </project>
                          """));
    }

    @Test
    void createFromArtifactIdEmptyDescription() {
        rewriteRun(
                spec -> spec.recipe(new EnsureIndexJelly()),
                // language=xml
                pomXml(
                        """
                          <project>
                              <parent>
                                  <groupId>org.jenkins-ci.plugins</groupId>
                                  <artifactId>plugin</artifactId>
                                  <version>4.88</version>
                              </parent>
                              <artifactId>empty</artifactId>
                              <description />
                              <version>1.0.0-SNAPSHOT</version>
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
                        """
                          <?jelly escape-by-default='true'?>
                          <div>
                             empty
                          </div>
                          """,
                        s -> s.path(ArchetypeCommonFile.INDEX_JELLY.getPath())));
    }

    @Test
    void createFromArtifactIdNoDescription() {
        rewriteRun(
                spec -> spec.recipe(new EnsureIndexJelly()),
                // language=xml
                pomXml(
                        """
                          <project>
                              <parent>
                                  <groupId>org.jenkins-ci.plugins</groupId>
                                  <artifactId>plugin</artifactId>
                                  <version>4.88</version>
                              </parent>
                              <artifactId>empty</artifactId>
                              <version>1.0.0-SNAPSHOT</version>
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
                        """
                          <?jelly escape-by-default='true'?>
                          <div>
                             empty
                          </div>
                          """,
                        s -> s.path("src/main/resources/index.jelly")));
    }

    @Test
    void shouldCreateMultipleNestedIndexJellies() {
        rewriteRun(
                spec -> spec.recipe(new EnsureIndexJelly()),
                mavenProject(
                        "parent",
                        // language=xml
                        pomXml(
                                """
              <project>
                  <groupId>org.example</groupId>
                  <artifactId>my-root</artifactId>
                  <version>0.1</version>
                  <packaging>pom</packaging>
                  <modules>
                      <module>plugin</module>
                      <module>other-plugin</module>
                      <module>not-a-plugin</module>
                  </modules>
              </project>
              """),
                        mavenProject(
                                "plugin",
                                pomXml(
                                        """
                <project>
                    <parent>
                        <groupId>org.jenkins-ci.plugins</groupId>
                        <artifactId>plugin</artifactId>
                        <version>4.86</version>
                    </parent>
                    <artifactId>plugin</artifactId>
                    <version>1.0.0-SNAPSHOT</version>
                    <repositories>
                        <repository>
                            <id>repo.jenkins-ci.org</id>
                            <url>https://repo.jenkins-ci.org/public/</url>
                        </repository>
                    </repositories>
                </project>
                """),
                                srcMainResources(text(
                                        null,
                                        """
                                                  <?jelly escape-by-default='true'?>
                                                  <div>
                                                     plugin
                                                  </div>
                                                  """,
                                        s -> s.path(ArchetypeCommonFile.INDEX_JELLY
                                                .getPath()
                                                .getFileName()
                                                .toString())))),
                        mavenProject(
                                "other-plugin",
                                pomXml(
                                        """
                <project>
                    <parent>
                        <groupId>org.jenkins-ci.plugins</groupId>
                        <artifactId>plugin</artifactId>
                        <version>4.88</version>
                    </parent>
                    <artifactId>other-plugin</artifactId>
                    <version>1.0.0-SNAPSHOT</version>
                    <repositories>
                        <repository>
                            <id>repo.jenkins-ci.org</id>
                            <url>https://repo.jenkins-ci.org/public/</url>
                        </repository>
                    </repositories>
                </project>
                """),
                                srcMainResources(text(
                                        null,
                                        """
                                                  <?jelly escape-by-default='true'?>
                                                  <div>
                                                     other-plugin
                                                  </div>
                                                  """,
                                        s -> s.path("index.jelly")))),
                        mavenProject(
                                "non-plugin",
                                pomXml(
                                        """
                <project>
                    <groupId>org.example</groupId>
                    <artifactId>not-a-plugin</artifactId>
                    <version>1.0.0-SNAPSHOT</version>
                </project>
                """))));
    }
}
