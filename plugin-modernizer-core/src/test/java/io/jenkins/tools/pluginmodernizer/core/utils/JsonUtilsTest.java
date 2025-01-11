package io.jenkins.tools.pluginmodernizer.core.utils;

import static org.junit.jupiter.api.Assertions.*;

import io.jenkins.tools.pluginmodernizer.core.extractor.MetadataFlag;
import io.jenkins.tools.pluginmodernizer.core.extractor.PluginMetadata;
import io.jenkins.tools.pluginmodernizer.core.model.JDK;
import io.jenkins.tools.pluginmodernizer.core.model.PreconditionError;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

@Execution(ExecutionMode.CONCURRENT)
public class JsonUtilsTest {

    @Test
    public void testMetaDataToJson() {
        PluginMetadata metadata = new PluginMetadata();
        metadata.setKey("plugin-api-key");
        metadata.setFlags(Set.of(MetadataFlag.IS_API_PLUGIN));
        metadata.setJenkinsVersion("2.479.1");
        metadata.setErrors(Set.of(PreconditionError.MAVEN_REPOSITORIES_HTTP));
        assertEquals(
                "{\"flags\":[\"IS_API_PLUGIN\"],\"errors\":[\"MAVEN_REPOSITORIES_HTTP\"],\"jenkinsVersion\":\"2.479.1\",\"key\":\"plugin-api-key\",\"path\":\".\"}",
                JsonUtils.toJson(metadata));
    }

    @Test
    public void testEquals() {
        String jsonString1 =
                "{\"key1\":\"value1\",\"key2\":\"value2\",\"key3\":{\"subKey1\":\"subValue1\",\"subKey2\":\"subValue2\"},\"key4\":[\"elem1\",\"elem2\"]}";
        String jsonString2 =
                "{\"key2\":\"value2\",\"key1\":\"value1\",\"key3\":{\"subKey2\":\"subValue2\",\"subKey1\":\"subValue1\"},\"key4\":[\"elem2\",\"elem1\"]}";
        assertTrue(JsonUtils.equals(jsonString1, jsonString2), "JSON strings are not semantically equals");
    }

    @Test
    public void testEqualsWithDifferentValues() {
        String jsonString1 = "{\"key1\":\"value1\",\"key2\":\"value2\"}";
        String jsonString2 = "{\"key1\":\"value1\",\"key2\":\"value3\"}";
        assertFalse(
                JsonUtils.equals(jsonString1, jsonString2),
                "JSON strings should not be semantically equal due to different values");
    }

    @Test
    public void testEqualsWithMissingKey() {
        String jsonString1 = "{\"key1\":\"value1\",\"key2\":\"value2\"}";
        String jsonString2 = "{\"key1\":\"value1\"}";
        assertFalse(
                JsonUtils.equals(jsonString1, jsonString2),
                "JSON strings should not be semantically equal due to missing key");
    }

    @Test
    public void testEqualsWithExtraKey() {
        String jsonString1 = "{\"key1\":\"value1\"}";
        String jsonString2 = "{\"key1\":\"value1\",\"key2\":\"value2\"}";
        assertFalse(
                JsonUtils.equals(jsonString1, jsonString2),
                "JSON strings should not be semantically equal due to extra key");
    }

    @Test
    public void testEqualsWtihDifferentElements() {
        String jsonString1 = "{\"key1\":[\"elem1\",\"elem2\"]}";
        String jsonString2 = "{\"key1\":[\"elem1\",\"elem3\"]}";
        assertFalse(
                JsonUtils.equals(jsonString1, jsonString2),
                "JSON strings should not be semantically equal due to different array elements");
    }

    @Test
    public void testEqualsWithDifferentNestedStructure() {
        String jsonString1 = "{\"key1\":{\"subKey1\":\"value1\"}}";
        String jsonString2 = "{\"key1\":{\"subKey1\":\"value2\"}}";
        assertFalse(
                JsonUtils.equals(jsonString1, jsonString2),
                "JSON strings should not be semantically equal due to nested structure difference");
    }

    @Test
    public void testEqualsWithEmptyVsNonEmpty() {
        String jsonString1 = "{}";
        String jsonString2 = "{\"key1\":\"value1\"}";
        assertFalse(
                JsonUtils.equals(jsonString1, jsonString2),
                "JSON strings should not be semantically equal due to one being empty");
    }

    @Test
    public void testEqualsWithDifferentArrayLengths() {
        String jsonString1 = "{\"key1\":[\"elem1\"]}";
        String jsonString2 = "{\"key1\":[\"elem1\",\"elem2\"]}";
        assertFalse(
                JsonUtils.equals(jsonString1, jsonString2),
                "JSON strings should not be semantically equal due to different array lengths");
    }

    @Test
    public void testMerge() {

        // Preps
        PluginMetadata metadata1 = new PluginMetadata();
        metadata1.setPluginName("name1");
        metadata1.setBomArtifactId("bom-weekly");
        metadata1.setBomVersion("1.234");
        metadata1.setJdks(Set.of(JDK.JAVA_11));
        metadata1.setProperties(Map.of("key1", "value1"));

        PluginMetadata metadata2 = new PluginMetadata();
        metadata2.setJdks(Set.of(JDK.JAVA_8));
        metadata2.setProperties(Map.of("key2", "value2"));
        metadata2.setParentVersion("4.88");

        // Merge
        PluginMetadata merged =
                JsonUtils.fromJson(JsonUtils.merge(metadata1.toJson(), metadata2.toJson()), PluginMetadata.class);

        // String and scalar are merged
        assertEquals("name1", merged.getPluginName());
        assertEquals("bom-weekly", merged.getBomArtifactId());
        assertEquals("1.234", merged.getBomVersion());
        assertEquals("4.88", merged.getParentVersion());

        // List are merged
        assertEquals(Set.of(JDK.JAVA_11, JDK.JAVA_8), merged.getJdks());

        // Maps are merged
        assertEquals(Map.of("key1", "value1", "key2", "value2"), merged.getProperties());
    }
}
