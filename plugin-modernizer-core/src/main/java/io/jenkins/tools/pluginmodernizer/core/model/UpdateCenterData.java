package io.jenkins.tools.pluginmodernizer.core.model;

import com.fasterxml.jackson.databind.JsonNode;
import io.jenkins.tools.pluginmodernizer.core.impl.CacheManager;
import java.io.Serializable;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;

public class UpdateCenterData extends CacheEntry<UpdateCenterData> implements Serializable {

    private final JsonNode jsonNode;

    public UpdateCenterData(CacheManager cacheManager, JsonNode jsonNode) {
        super(cacheManager, UpdateCenterData.class, CacheManager.UPDATE_CENTER_CACHE_KEY, Path.of("."));
        this.jsonNode = Objects.requireNonNull(jsonNode, "jsonNode must not be null");
    }

    public JsonNode getPlugin(Plugin plugin) {
        JsonNode plugins = jsonNode.get("plugins");
        if (plugins == null || !plugins.has(plugin.getName())) {
            plugin.addError("Plugin not found in update center");
            plugin.raiseLastError();
        }
        return plugins.get(plugin.getName());
    }

    public String getScmUrl(Plugin plugin) {
        JsonNode pluginInfo = getPlugin(plugin);
        JsonNode scmNode = pluginInfo.get("scm");

        if (scmNode == null) {
            plugin.addError("SCM information is missing in the provided UC file");
            plugin.raiseLastError();
        }

        if (scmNode.isObject()) {
            return Optional.ofNullable(scmNode.get("url"))
                    .map(JsonNode::asText)
                    .orElseThrow(() -> new PluginProcessingException("SCM URL is missing", plugin));
        } else if (scmNode.isTextual()) {
            return scmNode.asText();
        } else {
            plugin.addError("Unexpected type for SCM URL");
            plugin.raiseLastError();
        }
        return null;
    }
}
