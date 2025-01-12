package io.jenkins.tools.pluginmodernizer.core.utils;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.jenkins.tools.pluginmodernizer.core.model.ModernizerException;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

/**
 * Utility class for parsing a pom.xml
 */
public class StaticPomParser {

    private static final Logger LOG = LoggerFactory.getLogger(StaticPomParser.class);
    private final Document document;

    /**
     * Constructor for StaticPomParser.
     *
     * @param pomFilePath the path to the POM file
     */
    @SuppressFBWarnings("PATH_TRAVERSAL_IN")
    public StaticPomParser(String pomFilePath) {
        try {
            // Validate the file path
            Path path = Paths.get(pomFilePath).normalize().toAbsolutePath();
            if (!Files.exists(path) || !Files.isRegularFile(path)) {
                throw new ModernizerException("Invalid file path: " + path);
            }

            File pomFile = path.toFile();
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            dbFactory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            dbFactory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            dbFactory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            dbFactory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            dbFactory.setXIncludeAware(false);
            dbFactory.setExpandEntityReferences(false);
            // Ignore whitespace
            dbFactory.setIgnoringElementContentWhitespace(true);
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            document = dBuilder.parse(pomFile);
            document.getDocumentElement().normalize();
        } catch (Exception e) {
            throw new ModernizerException("Error parsing POM file: " + e.getMessage(), e);
        }
    }

    /**
     * Return the packaging type of the POM file.
     * @return the packaging type or null if not found
     */
    public String getPackaging() {
        XPath xPath = XPathFactory.newInstance().newXPath();
        try {
            return xPath.compile("/project/packaging").evaluate(document);
        } catch (Exception e) {
            LOG.warn("Error getting packaging: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Return the Jenkins version of the POM file.
     * @return the Jenkins version or null if not found
     */
    public String getJenkinsVersion() {
        XPath xPath = XPathFactory.newInstance().newXPath();
        try {
            return xPath.compile("/project/properties/jenkins.version").evaluate(document);
        } catch (Exception e) {
            LOG.warn("Error getting jenkins.version: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Return the Jenkins baseline of the POM file.
     * @return the Jenkins baseline or null if not found
     */
    public String getBaseline() {
        XPath xPath = XPathFactory.newInstance().newXPath();
        try {
            return xPath.compile("/project/properties/jenkins.baseline").evaluate(document);
        } catch (Exception e) {
            LOG.warn("Error getting baseline: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Return gitHubRepo property of the POM file or null if not found.
     * @return the gitHubRepo property or null if not found
     */
    public String getGithubRepoProperty() {
        XPath xPath = XPathFactory.newInstance().newXPath();
        try {
            return xPath.compile("/project/properties/gitHubRepo").evaluate(document);
        } catch (Exception e) {
            LOG.warn("Error getting github.repo: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Return scm connection property of the POM file or null if not found.
     * @return the scm connection property or null if not found
     */
    public String getScmConnectionProperty() {
        XPath xPath = XPathFactory.newInstance().newXPath();
        try {
            return xPath.compile("/project/properties/scm/connection").evaluate(document);
        } catch (Exception e) {
            LOG.warn("Error getting scm connection: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Return the groupId of the POM file.
     * @return the groupId or null if not found
     */
    public String getArtifactId() {
        XPath xPath = XPathFactory.newInstance().newXPath();
        try {
            return xPath.compile("/project/artifactId").evaluate(document);
        } catch (Exception e) {
            LOG.warn("Error getting artifactId: {}", e.getMessage());
            return null;
        }
    }
}
