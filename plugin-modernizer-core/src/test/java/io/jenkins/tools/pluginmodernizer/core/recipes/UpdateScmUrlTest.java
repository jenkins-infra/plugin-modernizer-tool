package io.jenkins.tools.pluginmodernizer.core.recipes;

import static org.openrewrite.maven.Assertions.pomXml;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

public class UpdateScmUrlTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new UpdateScmUrl());
    }

    @Test
    void updateScmUrls() {
        rewriteRun(
            // language=xml
                pomXml(
                        """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                  <modelVersion>4.0.0</modelVersion>
                  <scm>
                    <url>git://github.com/example/repo.git</url>
                    <connection>scm:git:git://github.com/example/repo.git</connection>
                  </scm>
                  <artifactId>example-plugin</artifactId>
                </project>
                """,
                        """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                  <modelVersion>4.0.0</modelVersion>
                  <scm>
                    <url>https://github.com/example/repo.git</url>
                    <connection>scm:git:https://github.com/example/repo.git</connection>
                    </scm>
                  <artifactId>example-plugin</artifactId>
                </project>
                """));
    }

    @Test
    void keepExistingHttpsUrls() {
        rewriteRun(
            // language=xml
            pomXml(
                    """
            <?xml version="1.0" encoding="UTF-8"?>
              <project xmlns="http://maven.apache.org/POM/4.0.0">
                <modelVersion>4.0.0</modelVersion>
                <scm>
                  <url>https://github.com/example/repo.git</url>
                  <connection>scm:git:https://github.com/example/repo.git</connection>
                </scm>
                <artifactId>example-plugin</artifactId>
              </project>
              """));
    }
}

