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
  private TreeNode buildTag;

  MavenTree(TreeNode projectNode) {
    this.projectNode = projectNode;
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

  private static TreeNode buildAsTree(MavenDependency dep) {
    TreeNode node = new TagNode("dependency");
    node.addChild(new LineNode("    <dependency>"));
    node.addChild(SingleLineTagNode.from("groupId", dep.groupId(), "      "));
    node.addChild(SingleLineTagNode.from("artifactId", dep.artifactId(), "      "));
    node.addChild(SingleLineTagNode.from("version", dep.version(), "      "));
    String scope = dep.scope();
    if (scope != null) {
      node.addChild(SingleLineTagNode.from("scope", scope, "      "));
    }
    String optional = dep.optional();
    if (optional != null) {
      node.addChild(SingleLineTagNode.from("optional", optional, "      "));
    }
    String type = dep.type();
    if (type != null) {
      node.addChild(SingleLineTagNode.from("type", type, "      "));
    }
    String classifier = dep.classifier();
    if (classifier != null) {
      node.addChild(SingleLineTagNode.from("classifier", classifier, "      "));
    }
    node.addChild(new LineNode("    </dependency>"));
    return node;
  }

  TreeNode findOrCreateDependencies() {
    List<TreeNode> dependencies = find("dependencies");
    if (!dependencies.isEmpty()) {
      return dependencies.getFirst();
    }
    return appendTag("dependencies", true, true);
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
    TreeNode emptyTag = createEmptyTag(name, "  ", whitespace, endNewLine);
    projectNode.addChildBefore(emptyTag, "</project>");
    return emptyTag;
  }

  private TreeNode createEmptyTag(String name, String indent, boolean outerWhitespace, boolean endNewLine) {
    TreeNode treeNode = new TagNode(name);
    if(outerWhitespace) {
      treeNode.addChild(new LineNode("\n"));
    }
    treeNode.addChild(new LineNode(indent + "<" + name + ">\n"));
    var lastIndent = endNewLine ? "\n" + indent : indent;
    treeNode.addChild(new LineNode(lastIndent + "</" + name + ">\n"));
    return treeNode;
  }

  TreeNode findOrCreateBuildPlugins() {
    TreeNode buildTag = obtainBuildTag();
    return buildTag.match("plugins")
      .orElseGet(() -> {
        TreeNode pluginsTag = createEmptyTag("plugins", "    ", false, true);
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

    String pluginsIndent = "      ";
    TreeNode pluginNode = new TagNode("plugin");
    for (String asLine : asLines) {
      pluginNode.addChild(new LineNode(pluginsIndent + asLine.substring(indent)));
    }
    buildPlugins.addChildBefore(pluginNode, "</plugins>");
  }
}
