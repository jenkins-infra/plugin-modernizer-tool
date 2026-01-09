package io.jenkins.tools.pluginmodernizer.core.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

@Execution(ExecutionMode.CONCURRENT)
public class JDKTest {

    // Write tests for JDK enum here
    @Test
    public void shouldNext() {
        assertEquals(JDK.JAVA_11, JDK.JAVA_8.next());
        assertEquals(JDK.JAVA_17, JDK.JAVA_11.next());
        assertEquals(JDK.JAVA_21, JDK.JAVA_17.next());
        assertEquals(JDK.JAVA_25, JDK.JAVA_21.next());
        assertNull(JDK.JAVA_25.next());
    }

    @Test
    public void shouldBefore() {
        assertNull(JDK.JAVA_8.previous());
        assertEquals(JDK.JAVA_8, JDK.JAVA_11.previous());
        assertEquals(JDK.JAVA_11, JDK.JAVA_17.previous());
        assertEquals(JDK.JAVA_17, JDK.JAVA_21.previous());
        assertEquals(JDK.JAVA_21, JDK.JAVA_25.previous());
    }

    @Test
    public void shouldGet() {
        assertEquals(JDK.JAVA_8, JDK.get(8));
        assertEquals(JDK.JAVA_11, JDK.get(11));
        assertEquals(JDK.JAVA_17, JDK.get(17));
        assertEquals(JDK.JAVA_21, JDK.get(21));
        assertEquals(JDK.JAVA_25, JDK.get(25));
    }

    @Test
    public void currentMax() {
        // Adapt when new JDK are added
        assertEquals(JDK.JAVA_25, JDK.max());
    }

    @Test
    public void all() {
        assertEquals(5, JDK.all().size());
        assertEquals(JDK.JAVA_8, JDK.all().get(0));
        assertEquals(JDK.JAVA_11, JDK.all().get(1));
        assertEquals(JDK.JAVA_17, JDK.all().get(2));
        assertEquals(JDK.JAVA_21, JDK.all().get(3));
        assertEquals(JDK.JAVA_25, JDK.all().get(4));
    }

    @Test
    public void min() {
        assertEquals(JDK.JAVA_8, JDK.min());
        assertEquals(JDK.JAVA_8, JDK.min(Set.of(JDK.values())));
        assertEquals(JDK.JAVA_8, JDK.min(Set.of(JDK.values()), "2.164.1"));
        assertEquals(JDK.JAVA_8, JDK.min(Set.of(JDK.values()), "2.346.1"));
        assertEquals(JDK.JAVA_11, JDK.min(Set.of(JDK.values()), "2.361.1"));
        assertEquals(JDK.JAVA_11, JDK.min(Set.of(JDK.values()), "2.462.3"));
        assertEquals(JDK.JAVA_17, JDK.min(Set.of(JDK.values()), "2.479.3"));
    }

    @Test
    public void getTopTwoJdkVersions() {
        assertEquals(1, JDK.getTopTwoJdkVersions(List.of(JDK.JAVA_11)).size());
        assertEquals(11, JDK.getTopTwoJdkVersions(List.of(JDK.JAVA_11)).get(0));

        assertEquals(
                2, JDK.getTopTwoJdkVersions(List.of(JDK.JAVA_17, JDK.JAVA_21)).size());
        assertEquals(
                21, JDK.getTopTwoJdkVersions(List.of(JDK.JAVA_17, JDK.JAVA_21)).get(0));
        assertEquals(
                17, JDK.getTopTwoJdkVersions(List.of(JDK.JAVA_17, JDK.JAVA_21)).get(1));

        assertEquals(
                25, JDK.getTopTwoJdkVersions(List.of(JDK.JAVA_21, JDK.JAVA_25)).get(0));
        assertEquals(
                21, JDK.getTopTwoJdkVersions(List.of(JDK.JAVA_21, JDK.JAVA_25)).get(1));
        assertEquals(
                2,
                JDK.getTopTwoJdkVersions(List.of(JDK.JAVA_8, JDK.JAVA_17, JDK.JAVA_11))
                        .size());
        assertEquals(
                17,
                JDK.getTopTwoJdkVersions(List.of(JDK.JAVA_8, JDK.JAVA_17, JDK.JAVA_11))
                        .get(0));
        assertEquals(
                11,
                JDK.getTopTwoJdkVersions(List.of(JDK.JAVA_8, JDK.JAVA_17, JDK.JAVA_11))
                        .get(1));
    }

    @Test
    public void filter() {
        assertEquals(1, JDK.filter(Set.of(JDK.JAVA_11), 2).size());
        assertEquals(1, JDK.filter(Set.of(JDK.JAVA_11), 1).size());
        assertEquals(1, JDK.filter(Set.of(JDK.JAVA_11, JDK.JAVA_8), 1).size());
        assertEquals(8, JDK.filter(Set.of(JDK.JAVA_11, JDK.JAVA_8), 1).get(0));
        assertEquals(8, JDK.filter(Set.of(JDK.JAVA_11, JDK.JAVA_8), 2).get(0));
        assertEquals(
                2, JDK.filter(Set.of(JDK.JAVA_11, JDK.JAVA_21, JDK.JAVA_8), 2).size());
        assertEquals(
                11, JDK.filter(Set.of(JDK.JAVA_11, JDK.JAVA_21, JDK.JAVA_8), 2).get(1));
        assertEquals(
                11,
                JDK.filter(Set.of(JDK.JAVA_11, JDK.JAVA_21, JDK.JAVA_8, JDK.JAVA_25), 2)
                        .get(1));
    }

    @Test
    public void getBuildableJdk() {

        assertEquals(1, JDK.get(null).size());
        assertEquals(JDK.JAVA_8, JDK.get(null).get(0));

        assertEquals(1, JDK.get("2.163").size());
        assertEquals(JDK.JAVA_8, JDK.get("2.163").get(0));

        assertEquals(2, JDK.get("2.164.1").size());
        assertEquals(JDK.JAVA_8, JDK.get("2.164.1").get(0));
        assertEquals(JDK.JAVA_11, JDK.get("2.164.1").get(1));

        assertEquals(2, JDK.get("2.339").size());
        assertEquals(JDK.JAVA_8, JDK.get("2.339").get(0));
        assertEquals(JDK.JAVA_11, JDK.get("2.339").get(1));

        assertEquals(3, JDK.get("2.346.1").size());
        assertEquals(JDK.JAVA_8, JDK.get("2.346.1").get(0));
        assertEquals(JDK.JAVA_11, JDK.get("2.346.1").get(1));
        assertEquals(JDK.JAVA_17, JDK.get("2.346.1").get(2));

        assertEquals(2, JDK.get("2.357").size());
        assertEquals(JDK.JAVA_11, JDK.get("2.357").get(0));
        assertEquals(JDK.JAVA_17, JDK.get("2.357").get(1));

        assertEquals(2, JDK.get("2.361.1").size());
        assertEquals(JDK.JAVA_11, JDK.get("2.361.1").get(0));
        assertEquals(JDK.JAVA_17, JDK.get("2.361.1").get(1));

        assertEquals(3, JDK.get("2.426.1").size());
        assertEquals(JDK.JAVA_11, JDK.get("2.426.1").get(0));
        assertEquals(JDK.JAVA_17, JDK.get("2.426.1").get(1));
        assertEquals(JDK.JAVA_21, JDK.get("2.426.1").get(2));

        assertEquals(3, JDK.get("2.462.3").size());
        assertEquals(JDK.JAVA_11, JDK.get("2.462.3").get(0));
        assertEquals(JDK.JAVA_17, JDK.get("2.462.3").get(1));
        assertEquals(JDK.JAVA_21, JDK.get("2.462.3").get(2));

        assertEquals(2, JDK.get("2.463").size());
        assertEquals(JDK.JAVA_17, JDK.get("2.463").get(0));
        assertEquals(JDK.JAVA_21, JDK.get("2.463").get(1));

        assertEquals(2, JDK.get("2.479.1").size());
        assertEquals(JDK.JAVA_17, JDK.get("2.479.1").get(0));
        assertEquals(JDK.JAVA_21, JDK.get("2.479.1").get(1));

        assertEquals(2, JDK.get("2.492.1").size());
        assertEquals(JDK.JAVA_17, JDK.get("2.492.1").get(0));
        assertEquals(JDK.JAVA_21, JDK.get("2.492.1").get(1));

        // Last LTS to support JDK 17
        assertEquals(3, JDK.get("2.541.1").size());
        assertEquals(JDK.JAVA_17, JDK.get("2.541.1").get(0));
        assertEquals(JDK.JAVA_21, JDK.get("2.541.1").get(1));
        assertEquals(JDK.JAVA_25, JDK.get("2.541.1").get(2));

        // First LTS to drop JDK 17 (adapt when baseline is choosen)
        assertEquals(2, JDK.get("2.550.1").size());
        assertEquals(JDK.JAVA_21, JDK.get("2.550.1").get(0));
        assertEquals(JDK.JAVA_25, JDK.get("2.550.1").get(1));
    }

    @Test
    public void getLatestTestHarnessVersion() {
        assertEquals("2225.v04fa_3929c9b_5", JDK.getLatestTestHarnessVersion("2.426.1"));
        assertEquals("1900.v9e128c991ef4", JDK.getLatestTestHarnessVersion("2.346.1"));
        assertNull(JDK.getLatestTestHarnessVersion("2.463"));
        assertNull(JDK.getLatestTestHarnessVersion("2.479.3"));
        assertEquals("2225.v04fa_3929c9b_5", JDK.getLatestTestHarnessVersion("2.361.1"));
        assertEquals("1900.v9e128c991ef4", JDK.getLatestTestHarnessVersion("2.164.1"));
    }
}
