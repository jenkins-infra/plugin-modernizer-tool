package io.jenkins.tools.pluginmodernizer.core.recipes;

import io.jenkins.tools.pluginmodernizer.core.visitors.AddLastPropertyVisitor;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;

/**
 * Add a Maven property at the end of the properties section
 */
public class AddProperty extends Recipe {

    @Option(
            displayName = "Property name",
            description = "Key name of the property to remove.",
            example = "configuration-as-code.version")
    String key;

    @Option(displayName = "Property value", description = "Value of the property to add.", example = "1.0.0")
    String value;

    public AddProperty(String key, String value) {
        this.key = key;
        this.value = value;
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new AddLastPropertyVisitor(key, value);
    }

    @Override
    public String getDisplayName() {
        return "Add a Maven property at the end of the properties section";
    }

    @Override
    public String getDescription() {
        return "Add a Maven property at the end of the properties section.";
    }
}
