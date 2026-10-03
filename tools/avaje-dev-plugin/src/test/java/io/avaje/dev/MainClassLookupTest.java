package io.avaje.dev;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Properties;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginExecution;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.junit.jupiter.api.Test;

class MainClassLookupTest {

  @Test
  void theJarPluginManifestNamesIt() {
    var plugin =
        plugin(
            "maven-jar-plugin",
            node(
                "configuration",
                node("archive", node("manifest", leaf("mainClass", "org.example.Main")))));

    assertThat(MainClassLookup.fromPlugins(List.of(plugin))).contains("org.example.Main");
  }

  @Test
  void aPluginNamesItDirectly() {
    var plugin =
        plugin(
            "spring-boot-maven-plugin",
            node("configuration", leaf("mainClass", "org.example.Boot")));

    assertThat(MainClassLookup.fromPlugins(List.of(plugin))).contains("org.example.Boot");
  }

  @Test
  void aShadeTransformerNamesIt() {
    var transformers =
        node(
            "transformers",
            node("transformer"),
            node("transformer", leaf("mainClass", "org.example.Shaded")));
    var plugin = plugin("maven-shade-plugin", node("configuration", transformers));

    assertThat(MainClassLookup.fromPlugins(List.of(plugin))).contains("org.example.Shaded");
  }

  @Test
  void anExecutionNamesIt() {
    var plugin = plugin("exec-maven-plugin", null);
    var execution = new PluginExecution();
    execution.setConfiguration(node("configuration", leaf("mainClass", "org.example.Exec")));
    plugin.addExecution(execution);

    assertThat(MainClassLookup.fromPlugins(List.of(plugin))).contains("org.example.Exec");
  }

  @Test
  void pluginsWithoutOneAreSkipped() {
    var compiler = plugin("maven-compiler-plugin", node("configuration", leaf("release", "21")));
    var jar =
        plugin(
            "maven-jar-plugin",
            node(
                "configuration",
                node("archive", node("manifest", leaf("mainClass", "org.example.Main")))));

    assertThat(MainClassLookup.fromPlugins(List.of(compiler, jar))).contains("org.example.Main");
    assertThat(MainClassLookup.fromPlugins(List.of(compiler))).isEmpty();
    assertThat(MainClassLookup.fromPlugins(List.of())).isEmpty();
  }

  @Test
  void anUnresolvedPropertyNamesNothing() {
    var plugin =
        plugin("maven-jar-plugin", node("configuration", leaf("mainClass", "${start-class}")));

    assertThat(MainClassLookup.fromPlugins(List.of(plugin))).isEmpty();
  }

  @Test
  void aBlankValueNamesNothing() {
    var plugin = plugin("maven-jar-plugin", node("configuration", leaf("mainClass", "  ")));

    assertThat(MainClassLookup.fromPlugins(List.of(plugin))).isEmpty();
  }

  @Test
  void theConventionalPropertiesNameIt() {
    assertThat(MainClassLookup.fromProperties(properties("exec.mainClass", "org.example.Exec")))
        .contains("org.example.Exec");
    assertThat(MainClassLookup.fromProperties(properties("start-class", "org.example.Boot")))
        .contains("org.example.Boot");
    assertThat(MainClassLookup.fromProperties(properties("mainClass", "org.example.Main")))
        .contains("org.example.Main");
    assertThat(MainClassLookup.fromProperties(properties("main.class", "org.example.Main")))
        .contains("org.example.Main");
    assertThat(MainClassLookup.fromProperties(properties("other", "org.example.Main"))).isEmpty();
    assertThat(MainClassLookup.fromProperties(new Properties())).isEmpty();
  }

  @Test
  void execWinsOverTheOtherProperties() {
    var properties = properties("start-class", "org.example.Boot");
    properties.setProperty("exec.mainClass", "org.example.Exec");

    assertThat(MainClassLookup.fromProperties(properties)).contains("org.example.Exec");
  }

  @Test
  void nothingIsFoundWithoutAProject() {
    assertThat(MainClassLookup.find(null)).isEmpty();
  }

  private static Properties properties(String key, String value) {
    var properties = new Properties();
    properties.setProperty(key, value);
    return properties;
  }

  private static Plugin plugin(String artifactId, Xpp3Dom configuration) {
    var plugin = new Plugin();
    plugin.setArtifactId(artifactId);
    plugin.setConfiguration(configuration);
    return plugin;
  }

  private static Xpp3Dom node(String name, Xpp3Dom... children) {
    var node = new Xpp3Dom(name);
    for (var child : children) {
      node.addChild(child);
    }
    return node;
  }

  private static Xpp3Dom leaf(String name, String value) {
    var node = new Xpp3Dom(name);
    node.setValue(value);
    return node;
  }
}
