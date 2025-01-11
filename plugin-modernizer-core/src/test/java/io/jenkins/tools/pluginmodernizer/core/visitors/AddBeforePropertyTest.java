package io.jenkins.tools.pluginmodernizer.core.visitors;

import static org.openrewrite.maven.Assertions.pomXml;
import static org.openrewrite.test.RewriteTest.toRecipe;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.openrewrite.ExecutionContext;
import org.openrewrite.maven.MavenIsoVisitor;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.xml.tree.Xml;

/**
 * Test for {@link AddBeforePropertyVisitor}
 */
@Execution(ExecutionMode.CONCURRENT)
public class AddBeforePropertyTest implements RewriteTest {

    @Test
    void addProperty() {
        rewriteRun(
                spec -> spec.recipe(toRecipe(() -> new MavenIsoVisitor<>() {
                    @Override
                    public Xml.Document visitDocument(Xml.Document x, ExecutionContext ctx) {
                        doAfterVisit(new AddBeforePropertyVisitor("jenkins.version", "jenkins.baseline", "2.440"));
                        return super.visitDocument(x, ctx);
                    }
                })),
                // language=xml
                pomXml(
                        """
                 <?xml version="1.0" encoding="UTF-8"?>
                 <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
                   <modelVersion>4.0.0</modelVersion>
                   <groupId>io.jenkins.plugins</groupId>
                   <artifactId>empty</artifactId>
                   <version>1.0.0-SNAPSHOT</version>
                   <packaging>hpi</packaging>
                   <name>Empty Plugin</name>
                   <properties>
                        <jenkins.version>2.440</jenkins.version>
                   </properties>
                 </project>
                 """,
                        """
                 <?xml version="1.0" encoding="UTF-8"?>
                 <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
                   <modelVersion>4.0.0</modelVersion>
                   <groupId>io.jenkins.plugins</groupId>
                   <artifactId>empty</artifactId>
                   <version>1.0.0-SNAPSHOT</version>
                   <packaging>hpi</packaging>
                   <name>Empty Plugin</name>
                   <properties>
                        <jenkins.baseline>2.440</jenkins.baseline>
                        <jenkins.version>2.440</jenkins.version>
                   </properties>
                 </project>
                  """));
    }

    @Test
    void addPropertyWithOtherProperties() {
        rewriteRun(
                spec -> spec.recipe(toRecipe(() -> new MavenIsoVisitor<>() {
                    @Override
                    public Xml.Document visitDocument(Xml.Document x, ExecutionContext ctx) {
                        doAfterVisit(new AddBeforePropertyVisitor("jenkins.version", "jenkins.baseline", "2.440"));
                        return super.visitDocument(x, ctx);
                    }
                })),
                // language=xml
                pomXml(
                        """
                 <?xml version="1.0" encoding="UTF-8"?>
                 <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
                   <modelVersion>4.0.0</modelVersion>
                   <groupId>io.jenkins.plugins</groupId>
                   <artifactId>empty</artifactId>
                   <version>1.0.0-SNAPSHOT</version>
                   <packaging>hpi</packaging>
                   <name>Empty Plugin</name>
                   <properties>
                        <foo.bar>baz</foo.bar>
                        <jenkins.version>2.440</jenkins.version>
                   </properties>
                 </project>
                 """,
                        """
                 <?xml version="1.0" encoding="UTF-8"?>
                 <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
                   <modelVersion>4.0.0</modelVersion>
                   <groupId>io.jenkins.plugins</groupId>
                   <artifactId>empty</artifactId>
                   <version>1.0.0-SNAPSHOT</version>
                   <packaging>hpi</packaging>
                   <name>Empty Plugin</name>
                   <properties>
                        <foo.bar>baz</foo.bar>
                        <jenkins.baseline>2.440</jenkins.baseline>
                        <jenkins.version>2.440</jenkins.version>
                   </properties>
                 </project>
                  """));
    }

    @Test
    void addPropertyWithOtherPropertiesWithComment() {
        rewriteRun(
                spec -> spec.recipe(toRecipe(() -> new MavenIsoVisitor<>() {
                    @Override
                    public Xml.Document visitDocument(Xml.Document x, ExecutionContext ctx) {
                        doAfterVisit(new AddBeforePropertyVisitor("jenkins.version", "jenkins.baseline", "2.440"));
                        return super.visitDocument(x, ctx);
                    }
                })),
                // language=xml
                pomXml(
                        """
                 <?xml version="1.0" encoding="UTF-8"?>
                 <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
                   <modelVersion>4.0.0</modelVersion>
                   <groupId>io.jenkins.plugins</groupId>
                   <artifactId>empty</artifactId>
                   <version>1.0.0-SNAPSHOT</version>
                   <packaging>hpi</packaging>
                   <name>Empty Plugin</name>
                   <!-- Properties -->
                   <properties>
                        <!-- Just a property -->
                        <foo.bar>baz</foo.bar>
                        <bar.bar>foo</bar.bar>
                        <!-- Jenkins version -->
                        <jenkins.version>2.440</jenkins.version>
                   </properties>
                 </project>
                 """,
                        """
                 <?xml version="1.0" encoding="UTF-8"?>
                 <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
                   <modelVersion>4.0.0</modelVersion>
                   <groupId>io.jenkins.plugins</groupId>
                   <artifactId>empty</artifactId>
                   <version>1.0.0-SNAPSHOT</version>
                   <packaging>hpi</packaging>
                   <name>Empty Plugin</name>
                   <!-- Properties -->
                   <properties>
                        <!-- Just a property -->
                        <foo.bar>baz</foo.bar>
                        <bar.bar>foo</bar.bar>
                        <jenkins.baseline>2.440</jenkins.baseline>
                        <!-- Jenkins version -->
                        <jenkins.version>2.440</jenkins.version>
                   </properties>
                 </project>
                  """));
    }
}
