package io.jenkins.tools.pluginmodernizer.core.model;

import io.jenkins.tools.pluginmodernizer.core.config.Settings;
import java.io.IOException;
import java.nio.file.Files;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

/**
 * Enum to represent the precondition errors preventing any modernization process
 * Generally, these are the errors that need to be fixed before applying any modernization (very old plugin)
 * We can provide in future version a way to fix these errors automatically (without OpenRewrite) by adding a fix function
 * on this enum
 */
public enum PreconditionError {

    /**
     * No pom file found
     */
    NO_POM(
            (document, xpath) -> document == null,
            plugin -> false, // No remediation function available if pom is missing
            "No pom file found"),

    /**
     * If the plugin is using an older java level bellow 8
     */
    OLDER_JAVA_LEVEL(
            (document, xpath) -> {
                if (document == null) {
                    return false;
                }
                try {
                    String javaLevel = (String) xpath.evaluate(
                            "//*[local-name()='project']/*[local-name()='properties']/*[local-name()='java.level']",
                            document,
                            XPathConstants.STRING);
                    if (javaLevel == null) {
                        return false;
                    }
                    // Change to 8
                    if (javaLevel.equals("5") || javaLevel.equals("6") || javaLevel.equals("7")) {
                        return true;
                    }
                    return false;
                } catch (Exception e) {
                    return false;
                }
            },
            plugin -> {
                try {
                    String content =
                            Files.readString(plugin.getLocalRepository().resolve("pom.xml"));
                    String newContent =
                            content.replaceAll("<java.level>(.*)</java.level>", "<java.level>8</java.level>");
                    if (!content.equals(newContent)) {
                        Files.writeString(plugin.getLocalRepository().resolve("pom.xml"), newContent);
                        return true;
                    }
                    return false;
                } catch (IOException e) {
                    plugin.addError("Error fixing java level: " + e.getMessage());
                    return false;
                }
            },
            "Found java level below 8 in pom file preventing modernization"),

    /**
     * Parent with 1.x doesn't work because of unfixed versionRange
     */
    PARENT_POM_1X(
            (document, xpath) -> {
                if (document == null) {
                    return false;
                }
                try {
                    Double parentVersion = (Double) xpath.evaluate(
                            "count(//*[local-name()='project']/*[local-name()='parent']/*[local-name()='version' and starts-with(., '1.')])",
                            document,
                            XPathConstants.NUMBER);
                    return parentVersion != null && !parentVersion.equals(0.0);
                } catch (Exception e) {
                    return false;
                }
            },
            plugin -> {
                try {
                    String content =
                            Files.readString(plugin.getLocalRepository().resolve("pom.xml"));

                    // Define regex to match the version in the <parent> tag
                    String regex = "(<parent>.*?<version>)(.*?)(</version>.*?</parent>)";
                    Pattern pattern = Pattern.compile(regex, Pattern.DOTALL);
                    Matcher matcher = pattern.matcher(content);

                    String newContent = matcher.replaceAll("$1" + Settings.REMEDIATION_PLUGIN_PARENT_VERSION + "$3");

                    if (!content.equals(newContent)) {
                        Files.writeString(plugin.getLocalRepository().resolve("pom.xml"), newContent);
                        return true;
                    }
                    return false;
                } catch (IOException e) {
                    plugin.addError("Error fixing parent version: " + e.getMessage());
                    return false;
                }
            },
            "Found parent version starting with 1. in pom file preventing modernization"),

    /**
     * If the plugin has HTTP repositories preventing modernization
     */
    MAVEN_REPOSITORIES_HTTP(
            (document, xpath) -> {
                if (document == null) {
                    return false;
                }
                try {
                    NodeList repositoryUrls = (NodeList) xpath.evaluate(
                            "//*[local-name()='project']/*[local-name()='repositories']/*[local-name()='repository']/*[local-name()='url']",
                            document,
                            XPathConstants.NODESET);

                    int nonHttpsCount = 0;
                    for (int i = 0; i < repositoryUrls.getLength(); i++) {
                        String url = repositoryUrls.item(i).getTextContent().trim();
                        if (!url.startsWith("https") && !url.startsWith("${") && !url.startsWith("file://")) {
                            nonHttpsCount++;
                        }
                    }
                    return nonHttpsCount > 0;
                } catch (Exception e) {
                    return false;
                }
            },
            plugin -> {
                try {
                    String content =
                            Files.readString(plugin.getLocalRepository().resolve("pom.xml"));
                    String newContent = content.replaceAll("<url>http://(.*)</url>", "<url>https://$1</url>");
                    if (!content.equals(newContent)) {
                        Files.writeString(plugin.getLocalRepository().resolve("pom.xml"), newContent);
                        return true;
                    }
                    return false;
                } catch (Exception e) {
                    plugin.addError("Error fixing HTTP repositories: " + e.getMessage());
                    return false;
                }
            },
            "Found non-https repository URL in pom file preventing maven older than 3.8.1");

    /**
     * Predicate to check if the flag is applicable for the given Document and XPath
     */
    private final BiFunction<Document, XPath, Boolean> isApplicable;

    /**
     * Remediation function to fix the error transforming plugin before OpenRewrite
     * This function should return true if the remediation was successful, false otherwise
     */
    private final Function<Plugin, Boolean> remediation;

    /**
     * Error message
     */
    private final String error;

    /**
     * Constructor
     *
     * @param isApplicable Predicate to check if the flag is applicable for the given XML document
     */
    PreconditionError(
            BiFunction<Document, XPath, Boolean> isApplicable, Function<Plugin, Boolean> remediation, String error) {
        this.isApplicable = isApplicable;
        this.remediation = remediation;
        this.error = error;
    }

    /**
     * Check if the flag is applicable for the given Document and XPath
     *
     * @param Document the XML document
     * @param xpath    the XPath object
     * @return true if the flag is applicable, false otherwise
     */
    public boolean isApplicable(Document Document, XPath xpath) {
        return isApplicable.apply(Document, xpath);
    }

    /**
     * Remediate the error for the given plugin
     *
     * @param plugin the plugin to remediate
     */
    public boolean remediate(Plugin plugin) {
        return remediation.apply(plugin);
    }

    /**
     * Get the error message
     *
     * @return the error message
     */
    public String getError() {
        return error;
    }
}
