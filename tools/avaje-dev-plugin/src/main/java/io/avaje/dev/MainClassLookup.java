package io.avaje.dev;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import org.apache.maven.model.Plugin;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.Xpp3Dom;

/** Finds the application main class in the project when the goal was not given one. */
final class MainClassLookup {

  private static final List<String> PROPERTIES =
      List.of("exec.mainClass", "start-class", "mainClass", "main.class");

  private MainClassLookup() {}

  static Optional<String> find(MavenProject project) {
    if (project == null) {
      return Optional.empty();
    }
    return fromPlugins(project.getBuildPlugins()).or(() -> fromProperties(project.getProperties()));
  }

  /** The main class named by a packaging plugin, like the jar plugin */
  static Optional<String> fromPlugins(List<Plugin> plugins) {
    if (plugins == null) {
      return Optional.empty();
    }
    for (var plugin : plugins) {
      for (var configuration : configurations(plugin)) {
        final var mainClass = fromConfiguration(configuration);
        if (mainClass.isPresent()) {
          return mainClass;
        }
      }
    }
    return Optional.empty();
  }

  /** The main class named by one of the conventional properties. */
  static Optional<String> fromProperties(Properties properties) {
    if (properties == null) {
      return Optional.empty();
    }
    return PROPERTIES.stream()
        .map(properties::getProperty)
        .flatMap(value -> className(value).stream())
        .findFirst();
  }

  /** The plugin configuration and that of each of its executions. */
  private static List<Xpp3Dom> configurations(Plugin plugin) {
    var configurations = new ArrayList<Xpp3Dom>();
    dom(plugin.getConfiguration()).ifPresent(configurations::add);
    for (var execution : plugin.getExecutions()) {
      dom(execution.getConfiguration()).ifPresent(configurations::add);
    }
    return configurations;
  }

  /**
   * The places a plugin names a main class. Such as the manifest in the jar and assembly plugins
   * write, or in a shade plugin manifest transformer.
   */
  private static Optional<String> fromConfiguration(Xpp3Dom configuration) {
    return value(configuration, "mainClass")
        .or(() -> value(configuration, "archive", "manifest", "mainClass"))
        .or(() -> fromTransformers(configuration));
  }

  /** Shade names the main class in whichever of its transformers writes the manifest. */
  private static Optional<String> fromTransformers(Xpp3Dom configuration) {
    final var transformers = configuration.getChild("transformers");
    if (transformers == null) {
      return Optional.empty();
    }
    for (var transformer : transformers.getChildren()) {
      final var mainClass = value(transformer, "mainClass");
      if (mainClass.isPresent()) {
        return mainClass;
      }
    }
    return Optional.empty();
  }

  private static Optional<String> value(Xpp3Dom parent, String... path) {
    var node = parent;
    for (var name : path) {
      node = node.getChild(name);
      if (node == null) {
        return Optional.empty();
      }
    }
    return className(node.getValue());
  }

  /** Values that were never given, or hold a property maven could not resolve, name no class. */
  private static Optional<String> className(String value) {
    if (value == null) {
      return Optional.empty();
    }
    final var trimmed = value.trim();
    if (trimmed.isEmpty() || trimmed.contains("${")) {
      return Optional.empty();
    }
    return Optional.of(trimmed);
  }

  private static Optional<Xpp3Dom> dom(Object configuration) {
    return configuration instanceof Xpp3Dom dom ? Optional.of(dom) : Optional.empty();
  }
}
