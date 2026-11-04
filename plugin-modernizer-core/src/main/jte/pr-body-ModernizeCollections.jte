@import io.jenkins.tools.pluginmodernizer.core.model.Plugin
@import io.jenkins.tools.pluginmodernizer.core.model.Recipe
@param Plugin plugin
@param Recipe recipe
Hello `${plugin.getName()}` developers!

This is an automated pull request created by the [Jenkins Plugin Modernizer](https://github.com/jenkins-infra/plugin-modernizer-tool) tool. The tool has applied the following recipe to modernize the plugin:
<details aria-label="Recipe details for ${recipe.getDisplayName()}">
    <summary>${recipe.getDisplayName()}</summary>
    <p><em>${recipe.getName()}</em></p>
    <blockquote>${recipe.getDescription()}</blockquote>
</details>

## Modernize Java Collections

This recipe replaces legacy collection initializations and Guava immutable collections with modern Java standard library factory methods and Stream operations:

- Modernize map initializations to `Map.of()` and `Map.ofEntries()`
- Modernize list initializations to `List.of()`
- Modernize set initializations to `Set.of()`
- Replace `Collections.singletonMap`, `Collections.singletonList`, and `Collections.singleton` with factory methods
- Replace `Collections.unmodifiableList` and `Collections.unmodifiableSet` with factory methods
- Replace `Stream.collect(Collectors.toUnmodifiableList())` with `Stream.toList()`
- Replace Guava `ImmutableMap.of`, `ImmutableList.of`, and `ImmutableSet.of` with standard library equivalents
