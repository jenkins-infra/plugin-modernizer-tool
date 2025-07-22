package io.jenkins.tools.pluginmodernizer.core.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import io.jenkins.tools.pluginmodernizer.core.model.ModernizerException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JsonUtils {

    private static final Logger LOG = LoggerFactory.getLogger(JsonUtils.class);

    private static final Gson gson;

    private JsonUtils() {
        // Hide constructor
    }

    static {
        gson = new Gson();
    }

    /**
     * Convert an object to a JSON string
     * @param object The object to convert
     * @return The JSON string
     */
    public static String toJson(Object object) {
        return gson.toJson(object);
    }

    /**
     * Return of the two JSON strings are semantically equals
     * @param jsonString1 The first JSON string
     * @param jsonString2 The second JSON string
     * @return True or False
     */
    public static boolean equals(String jsonString1, String jsonString2) {
        try {
            ObjectMapper mapper = new ObjectMapper();

            // Parse JSON strings into JsonNode
            JsonNode node1 = mapper.readTree(jsonString1);
            JsonNode node2 = mapper.readTree(jsonString2);

            // Perform deep comparison with array order ignored
            return deepEqualsIgnoreArrayOrder(node1, node2);

        } catch (IOException e) {
            throw new ModernizerException("Unable to parse JSON strings due to IO error", e);
        }
    }

    /**
     * Merge two JSON strings
     * @param jsonString1 The first JSON string
     * @param jsonString2 The second JSON string
     * @return The merged JSON string
     */
    public static String merge(String jsonString1, String jsonString2) {
        try {
            ObjectMapper mapper = new ObjectMapper();

            // Parse JSON strings into JsonNode
            JsonNode node1 = mapper.readTree(jsonString1);
            JsonNode node2 = mapper.readTree(jsonString2);

            // Recursively merge both nodes
            JsonNode mergedNode = mergeNodes(node1, node2);

            // Convert merged JSON back to string
            return mapper.writeValueAsString(mergedNode);
        } catch (IOException e) {
            throw new ModernizerException("Unable to merge JSON strings due to IO error", e);
        }
    }

    /**
     * Merge two JSON nodes
     * @param mainNode The main JSON node
     * @param updateNode The update JSON node
     * @return The merged JSON node
     */
    private static JsonNode mergeNodes(JsonNode mainNode, JsonNode updateNode) {
        if (mainNode.isObject() && updateNode.isObject()) {
            ObjectNode mergedNode = ((ObjectNode) mainNode).deepCopy();
            updateNode.fields().forEachRemaining(entry -> {
                String fieldName = entry.getKey();
                JsonNode updateValue = entry.getValue();

                if (mergedNode.has(fieldName)) {
                    JsonNode existingValue = mergedNode.get(fieldName);

                    // Merge arrays without duplicates
                    if (existingValue.isArray() && updateValue.isArray()) {
                        mergedNode.set(fieldName, mergeArrays((ArrayNode) existingValue, (ArrayNode) updateValue));
                    }
                    // Recursive merge for objects
                    else if (existingValue.isObject() && updateValue.isObject()) {
                        mergedNode.set(fieldName, mergeNodes(existingValue, updateValue));
                    }
                    // Replace scalar values
                    else {
                        mergedNode.set(fieldName, updateValue);
                    }
                } else {
                    mergedNode.set(fieldName, updateValue); // Add new fields
                }
            });
            return mergedNode;
        }
        return updateNode; // Return updated value for non-object types
    }

    /**
     * Convert an object to a JSON file
     * @param object The object to convert
     * @param path The path to the JSON file
     */
    public static void toJsonFile(Object object, Path path) {
        try {
            LOG.debug("Writing JSON file to {}", path);
            String prettyJson = JsonUtils.prettyPrint(gson.toJson(object));
            FileUtils.writeStringToFile(path.toFile(), prettyJson, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new ModernizerException("Unable to write JSON file due to IO error", e);
        }
    }

    /**
     * Convert a JSON string to an object
     * @param json The JSON string
     * @param clazz The class of the object
     * @param <T> The type of the object
     * @return The object
     */
    public static <T> T fromJson(String json, Class<T> clazz) {
        return gson.fromJson(json, clazz);
    }

    /**
     * Convert a JSON string to an object
     * @param path The path to the JSON file
     * @param clazz The class of the object
     * @param <T> The type of the object
     * @return The object
     */
    public static <T> T fromJson(Path path, Class<T> clazz) {
        try {
            return gson.fromJson(FileUtils.readFileToString(path.toFile(), StandardCharsets.UTF_8), clazz);
        } catch (IOException e) {
            throw new ModernizerException("Unable to read JSON file due to IO error", e);
        }
    }

    /**
     * Download JSON data from a URL and convert it to an object
     * @param url The URL to download from
     * @param clazz The class of the object
     * @return The object
     * @param <T> The type of the object
     */
    public static <T> T fromUrl(URL url, Class<T> clazz) {
        try {
            HttpClient client = HttpClient.newBuilder()
                    .followRedirects(HttpClient.Redirect.NORMAL)
                    .build();
            HttpRequest request =
                    HttpRequest.newBuilder().GET().uri(url.toURI()).build();
            LOG.debug("Fetching data from: {}", url);
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new ModernizerException(
                        "Failed to get JSON data. Received response code: " + response.statusCode());
            }
            LOG.debug("Fetched data from: {}", url);
            return JsonUtils.fromJson(response.body(), clazz);
        } catch (IOException | JsonSyntaxException | URISyntaxException | InterruptedException e) {
            throw new ModernizerException("Unable to fetch data from " + url, e);
        }
    }

    /**
     * Merge two JSON arrays
     * @param array1 The first JSON array
     * @param array2 The second JSON array
     * @return The merged JSON array
     */
    private static ArrayNode mergeArrays(ArrayNode array1, ArrayNode array2) {
        ObjectMapper mapper = new ObjectMapper();
        Set<JsonNode> set = new HashSet<>();
        array1.forEach(set::add);
        array2.forEach(set::add);
        ArrayNode mergedArray = mapper.createArrayNode();
        set.forEach(mergedArray::add);
        return mergedArray;
    }

    /**
     * Deep comparison of two JSON nodes with array order ignored
     * @param node1 The first JSON node
     * @param node2 The second JSON node
     * @return True or False
     */
    private static boolean deepEqualsIgnoreArrayOrder(JsonNode node1, JsonNode node2) {
        if (node1.isArray() && node2.isArray()) {
            return arrayEqualsIgnoreOrder((ArrayNode) node1, (ArrayNode) node2);
        } else if (node1.isObject() && node2.isObject()) {
            ObjectNode obj1 = (ObjectNode) node1;
            ObjectNode obj2 = (ObjectNode) node2;
            if (obj1.size() != obj2.size()) {
                return false;
            }
            Iterator<String> fieldNames = obj1.fieldNames();
            while (fieldNames.hasNext()) {
                String fieldName = fieldNames.next();
                if (!obj2.has(fieldName) || !deepEqualsIgnoreArrayOrder(obj1.get(fieldName), obj2.get(fieldName))) {
                    return false;
                }
            }
            return true;
        }
        return node1.equals(node2);
    }

    /**
     * Compare two JSON arrays without order sensitivity
     * @param array1 The first JSON array
     * @param array2 The second JSON array
     * @return True or False
     */
    private static boolean arrayEqualsIgnoreOrder(ArrayNode array1, ArrayNode array2) {
        if (array1.size() != array2.size()) {
            return false;
        }
        Set<JsonNode> set1 = new HashSet<>();
        Set<JsonNode> set2 = new HashSet<>();
        array1.forEach(set1::add);
        array2.forEach(set2::add);
        return set1.equals(set2);
    }

    private static String prettyPrint(String uglyJson) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        JsonElement jsonElement = JsonParser.parseString(uglyJson);
        return gson.toJson(jsonElement);
    }
}
