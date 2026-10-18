package io.jenkins.tools.pluginmodernizer.core.model;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.jenkins.tools.pluginmodernizer.core.config.Settings;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPathFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.w3c.dom.Document;

/**
 * Test for {@link PreconditionError}.
 */
@Execution(ExecutionMode.CONCURRENT)
public class PreconditionErrorTest {

    @TempDir
    Path tempDir;

    @Test
    void detectAndRemediateParentPom1xAddsRelativePath() throws Exception {
        Path pom = tempDir.resolve("pom.xml");
        Files.writeString(pom, """
                <?xml version="1.0" encoding="UTF-8"?>
                <project>
                  <modelVersion>4.0.0</modelVersion>
                  <parent>
                    <groupId>org.jenkins-ci.plugins</groupId>
                    <artifactId>plugin</artifactId>
                    <version>1.399</version>
                  </parent>
                  <artifactId>AnchorChain</artifactId>
                </project>
                """);

        Document document =
                DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(pom.toFile());
        var xpath = XPathFactory.newInstance().newXPath();

        assertTrue(PreconditionError.PARENT_POM_1X.isApplicable(document, xpath));

        Plugin plugin = Plugin.build("AnchorChain", tempDir);
        assertTrue(PreconditionError.PARENT_POM_1X.remediate(plugin));

        String updated = Files.readString(pom);
        assertTrue(updated.contains("<version>" + Settings.REMEDIATION_PLUGIN_PARENT_VERSION + "</version>"));
        assertTrue(updated.contains("<relativePath />") || updated.contains("<relativePath/>"));
        assertFalse(updated.contains("<version>1.399</version>"));
    }

    @Test
    void detectHttpPluginRepositories() throws Exception {
        Path pom = tempDir.resolve("pom.xml");
        Files.writeString(pom, """
                <?xml version="1.0" encoding="UTF-8"?>
                <project>
                  <modelVersion>4.0.0</modelVersion>
                  <parent>
                    <groupId>org.jenkins-ci.plugins</groupId>
                    <artifactId>plugin</artifactId>
                    <version>4.88</version>
                    <relativePath />
                  </parent>
                  <artifactId>example</artifactId>
                  <pluginRepositories>
                    <pluginRepository>
                      <id>repo.jenkins-ci.org</id>
                      <url>http://repo.jenkins-ci.org/public/</url>
                    </pluginRepository>
                  </pluginRepositories>
                </project>
                """);

        Document document =
                DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(pom.toFile());
        var xpath = XPathFactory.newInstance().newXPath();

        assertTrue(PreconditionError.MAVEN_REPOSITORIES_HTTP.isApplicable(document, xpath));

        Plugin plugin = Plugin.build("example", tempDir);
        assertTrue(PreconditionError.MAVEN_REPOSITORIES_HTTP.remediate(plugin));
        assertTrue(Files.readString(pom).contains("<url>https://repo.jenkins-ci.org/public/</url>"));
    }
}
