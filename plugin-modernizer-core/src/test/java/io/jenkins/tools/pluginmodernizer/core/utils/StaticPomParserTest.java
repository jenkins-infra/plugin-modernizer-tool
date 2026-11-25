package io.jenkins.tools.pluginmodernizer.core.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class StaticPomParserTest {

    @TempDir
    Path tempDir;

    @Test
    void readsJavaVersion() throws IOException {
        Path pom = writePom("""
                <project>
                  <properties>
                    <java.version>21</java.version>
                  </properties>
                </project>
                """);
        assertEquals(21, new StaticPomParser(pom.toString()).getPluginJavaMajor());
    }

    @Test
    void readsJavaLevelOneDotEight() throws IOException {
        Path pom = writePom("""
                <project>
                  <properties>
                    <java.level>1.8</java.level>
                  </properties>
                </project>
                """);
        assertEquals(8, new StaticPomParser(pom.toString()).getPluginJavaMajor());
    }

    @Test
    void prefersJavaVersionOverCompilerRelease() throws IOException {
        Path pom = writePom("""
                <project>
                  <properties>
                    <java.version>17</java.version>
                    <maven.compiler.release>11</maven.compiler.release>
                  </properties>
                </project>
                """);
        assertEquals(17, new StaticPomParser(pom.toString()).getPluginJavaMajor());
    }

    @Test
    void returnsNullWhenAbsent() throws IOException {
        Path pom = writePom("""
                <project>
                  <properties>
                    <jenkins.version>2.479.3</jenkins.version>
                  </properties>
                </project>
                """);
        assertNull(new StaticPomParser(pom.toString()).getPluginJavaMajor());
    }

    private Path writePom(String content) throws IOException {
        Path pom = tempDir.resolve("pom.xml");
        Files.writeString(pom, content);
        return pom;
    }
}
