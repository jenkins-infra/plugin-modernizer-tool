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
 * Test for {@link RemovePropertyCommentVisitor}
 */
@Execution(ExecutionMode.CONCURRENT)
public class RemovePropertyCommentVisitorTest implements RewriteTest {

    @Test
    void replacePropertyComment() {
        rewriteRun(
                spec -> spec.recipe(toRecipe(() -> new MavenIsoVisitor<>() {
                    @Override
                    public Xml.Document visitDocument(Xml.Document x, ExecutionContext ctx) {
                        doAfterVisit(new RemovePropertyCommentVisitor(" My other property "));
                        doAfterVisit(new RemovePropertyCommentVisitor(" Unrelated comment "));
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
                   <!-- My properties -->
                   <properties>
                        <!-- My other property -->
                        <my.other.property>value</my.other.property>
                        <!-- Unrelated comment -->
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
                   <!-- My properties -->
                   <properties>
                        <my.other.property>value</my.other.property>
                        <jenkins.version>2.440</jenkins.version>
                   </properties>
                 </project>
                  """));
    }

    @Test
    void notChangesIfCommentIsNotMatching() {
        rewriteRun(
                spec -> spec.recipe(toRecipe(() -> new MavenIsoVisitor<>() {
                    @Override
                    public Xml.Document visitDocument(Xml.Document x, ExecutionContext ctx) {
                        doAfterVisit(new RemovePropertyCommentVisitor(" Comment 1 "));
                        doAfterVisit(new RemovePropertyCommentVisitor(" Comment 2 "));
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
                   <!-- My properties -->
                   <properties>
                        <!-- My other property -->
                        <my.other.property>value</my.other.property>
                        <!-- Unrelated comment -->
                        <jenkins.version>2.440</jenkins.version>
                   </properties>
                 </project>
                 """));
    }

    @Test
    void noChangesIfEmptyProperties() {
        rewriteRun(
                spec -> spec.recipe(toRecipe(() -> new MavenIsoVisitor<>() {
                    @Override
                    public Xml.Document visitDocument(Xml.Document x, ExecutionContext ctx) {
                        doAfterVisit(new RemovePropertyCommentVisitor(" No property here "));
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
                   <!-- No property here -->
                   <properties />
                 </project>
                 """));
    }

    @Test
    void noChangesIfNoProperties() {
        rewriteRun(
                spec -> spec.recipe(toRecipe(() -> new MavenIsoVisitor<>() {
                    @Override
                    public Xml.Document visitDocument(Xml.Document x, ExecutionContext ctx) {
                        doAfterVisit(new RemovePropertyCommentVisitor(" No property here "));
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
                 </project>
                 """));
    }
}
