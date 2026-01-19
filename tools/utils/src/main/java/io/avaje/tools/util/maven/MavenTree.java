package io.avaje.tools.util.maven;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class MavenTree {

  private final TreeNode projectNode;
  private final String indent1;
  private final String indent2;
  private final String indent3;
  private TreeNode buildTag;

  MavenTree(TreeNode projectNode, String baseIndent) {
    this.projectNode = projectNode;
    this.indent1 = baseIndent;
    this.indent2 = indent1 + indent1;
    this.indent3 = indent1 + indent1 + indent1;
  }

  public static MavenTree read(File file) {
    return TreeReader.read(file);
  }

  public void write(Path path) throws IOException {
    Files.write(path, asLines(), StandardCharsets.UTF_8);
  }

  public String groupId() {
    String groupId = singleAttribute("groupId");
    return groupId != null ? groupId : singleAttribute("parent.groupId");
  }

  public String artifactId() {
    return singleAttribute("artifactId");
  }

  public String name() {
    return singleAttribute("name");
  }

  public String description() {
    return singleAttribute("description");
  }

  private String singleAttribute(String name) {
    List<TreeNode> artifactIds = find(name);
    if (!artifactIds.isEmpty()) {
      return artifactIds.getFirst().innerContent();
    }
    return null;
  }

  public List<String> properties() {
    return find("properties").stream()
      .flatMap(n -> n.children().stream())
      .filter(n -> !(n instanceof TagNode))
      .map(n -> n.toString().trim())
      .toList();
  }

  /**
   * Return all the dependency groupId:artifactId keys.
   *
   * <p>Use this to optimally detect existing dep
   */
  public Set<String> dependencyKeys() {
    return dependencies().
      map(MavenDependency::key)
      .collect(Collectors.toSet());
  }

  public boolean containsDependency(String groupId, String artifactId) {
    return dependencies()
      .anyMatch(dep -> dep.groupId().equals(groupId) && dep.artifactId().equals(artifactId));
  }

  public void addDependencies(List<MavenDependency> dependencies) {
    for (MavenDependency dependency : dependencies) {
      addDependency(dependency);
    }
  }

  public void addDependency(MavenDependency dep) {
    TreeNode addNode = buildAsTree(dep);
    TreeNode dependencies = findOrCreateDependencies();

    boolean addTestDependency = "test".equals(dep.scope());
    if (addTestDependency) {
      // just add it to the end
      dependencies.addChildBefore(addNode, "</dependencies>");
      return;
    }
    // find the first non test dependency and add before that
    int insertPos = findInsertForCompileDependency(dependencies.children());
    if (insertPos != -1) {
      dependencies.children().add(insertPos, addNode);
    } else {
      dependencies.addChildBefore(addNode, "</dependencies>");
    }
  }

  private static int findInsertForCompileDependency(List<TreeNode> children) {
    for (int i = 0; i < children.size(); i++) {
      TreeNode child = children.get(i);
      if (child instanceof LineNode lineNode) {
        if (lineNode.innerContent().toLowerCase().contains("<!-- test dependencies")) {
          return i;
        }
      }
      if (child.name().equals("dependency")) {
        MavenDependency existingDep = toMavenDependency(child);
        if ("test".equals(existingDep.scope())) {
          return i;
        }
      }
    }
    return -1;
  }

  private TreeNode buildAsTree(MavenDependency dep) {
    TreeNode node = new TagNode("dependency");
    node.addChild(new LineNode(indent2 + "<dependency>"));
    node.addChild(SingleLineTagNode.from("groupId", dep.groupId(), indent3));
    node.addChild(SingleLineTagNode.from("artifactId", dep.artifactId(), indent3));
    node.addChild(SingleLineTagNode.from("version", dep.version(), indent3));
    String scope = dep.scope();
    if (scope != null) {
      node.addChild(SingleLineTagNode.from("scope", scope, indent3));
    }
    String optional = dep.optional();
    if (optional != null) {
      node.addChild(SingleLineTagNode.from("optional", optional, indent3));
    }
    String type = dep.type();
    if (type != null) {
      node.addChild(SingleLineTagNode.from("type", type, indent3));
    }
    String classifier = dep.classifier();
    if (classifier != null) {
      node.addChild(SingleLineTagNode.from("classifier", classifier, indent3));
    }
    node.addChild(new LineNode(indent2 + "</dependency>"));
    return node;
  }

  TreeNode findOrCreateDependencies() {
    List<TreeNode> dependencies = find("dependencies");
    if (!dependencies.isEmpty()) {
      return dependencies.getFirst();
    }
    return appendTag("dependencies", true, true);
  }

  public void addProperties(List<String> properties) {
    TreeNode treeNode = findOrCreateProperties();
    for (String property : properties) {
      var line = indent2 + property.trim();
      treeNode.addChildBefore(new LineNode(line), "</properties>");
    }
  }

  TreeNode findOrCreateProperties() {
    List<TreeNode> dependencies = find("properties");
    if (!dependencies.isEmpty()) {
      return dependencies.getFirst();
    }
    TreeNode newTag = createEmptyTag("properties", indent1, false, false);
    projectNode.addChildBefore(newTag, Set.of("dependencies", "dependencyManagement", "build", "profiles"));
    return newTag;
  }

  TreeNode obtainBuildTag() {
    if (buildTag == null) {
      buildTag = findOrCreateBuildTag();
    }
    return buildTag;
  }

  TreeNode findOrCreateBuildTag() {
    List<TreeNode> build = find("build");
    if (!build.isEmpty()) {
      return build.getFirst();
    }
    return appendTag("build", false, false);
  }

  private TreeNode appendTag(String name, boolean whitespace, boolean endNewLine) {
    TreeNode emptyTag = createEmptyTag(name, indent1, whitespace, endNewLine);
    projectNode.addChildBefore(emptyTag, "</project>");
    return emptyTag;
  }

  private TreeNode createEmptyTag(String name, String indent, boolean startNewLine, boolean endNewLine) {
    TreeNode treeNode = new TagNode(name);
    var firstIndent = startNewLine ? "\n" + indent : indent;
    treeNode.addChild(new LineNode(firstIndent + "<" + name + ">\n"));
    var lastIndent = endNewLine ? "\n" + indent : indent;
    treeNode.addChild(new LineNode(lastIndent + "</" + name + ">\n"));
    return treeNode;
  }

  TreeNode findOrCreateBuildPlugins() {
    TreeNode buildTag = obtainBuildTag();
    return buildTag.match("plugins")
      .orElseGet(() -> {
        TreeNode pluginsTag = createEmptyTag("plugins", indent2, false, true);
        buildTag.addChildBefore(pluginsTag, "</build>");
        return pluginsTag;
      });
  }

  public Stream<MavenDependency> dependencies() {
    return find("dependencies.dependency").stream()
      .map(MavenTree::toMavenDependency);
  }

  private static MavenDependency toMavenDependency(TreeNode depNode) {
    var builder = MavenDependency.builder();
    for (TreeNode child : depNode.children()) {
      if (child.name().equals("groupId")) {
        builder.groupId(child.innerContent());
      } else if (child.name().equals("artifactId")) {
        builder.artifactId(child.innerContent());
      } else if (child.name().equals("version")) {
        builder.version(child.innerContent());
      } else if (child.name().equals("scope")) {
        builder.scope(child.innerContent());
      } else if (child.name().equals("optional")) {
        builder.optional(child.innerContent());
      } else if (child.name().equals("classifier")) {
        builder.classifier(child.innerContent());
      } else if (child.name().equals("type")) {
        builder.type(child.innerContent());
      }
    }
    return builder.build();
  }

  public List<TreeNode> find(String dotPath) {
    List<TreeNode> matching = new ArrayList<>();
    projectNode.match(matching, dotPath);
    return matching;
  }

  List<String> asLines() {
    List<String> lines = new ArrayList<>();
    projectNode.asLines(lines);
    return lines;
  }

  public void addBuildPlugin(List<String> addBuildPlugin) {
    for (String buildPlugin : addBuildPlugin) {
      addBuildPlugin(buildPlugin);
    }
  }

  private void addBuildPlugin(String rawBuildPlugin) {
    List<String> asLines = rawBuildPlugin.lines().toList();
    String first = asLines.getFirst();
    int indent = first.indexOf("<plugin>");
    if (indent == -1) {
      throw new IllegalStateException("Missing <plugin> tag from: " + rawBuildPlugin);
    }

    TreeNode buildPlugins = findOrCreateBuildPlugins();

    TreeNode pluginNode = new TagNode("plugin");
    for (String asLine : asLines) {
      pluginNode.addChild(new LineNode(indent3 + asLine.substring(indent)));
    }
    buildPlugins.addChildBefore(pluginNode, "</plugins>");
  }
}
