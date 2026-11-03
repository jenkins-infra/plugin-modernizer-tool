package io.jenkins.tools.pluginmodernizer.core.recipes;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class AddCodeOwnerTeamNameTest {

    @ParameterizedTest
    @CsvSource({
        "empty, @jenkinsci/empty-plugin-developers",
        "openstack-cloud, @jenkinsci/openstack-cloud-plugin-developers",
        "openstack-cloud-plugin, @jenkinsci/openstack-cloud-plugin-developers",
        "bitbucket-oauth, @jenkinsci/bitbucket-oauth-plugin-developers",
        "foo-parent, @jenkinsci/foo-plugin-developers",
        "FOO, @jenkinsci/foo-plugin-developers"
    })
    void teamNameForArtifactId(String artifactId, String expectedTeam) {
        assertEquals(expectedTeam, AddCodeOwner.teamNameForArtifactId(artifactId));
    }

    @Test
    void teamNameForBlankArtifactId() {
        assertEquals("", AddCodeOwner.teamNameForArtifactId(null));
        assertEquals("", AddCodeOwner.teamNameForArtifactId(""));
        assertEquals("", AddCodeOwner.teamNameForArtifactId("   "));
    }
}
