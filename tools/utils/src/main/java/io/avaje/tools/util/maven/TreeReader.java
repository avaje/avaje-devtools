package io.avaje.tools.util.maven;


import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class TreeReader {

  private static final Pattern XML_START_TAG = Pattern.compile("<([a-zA-Z_][\\w\\-.:]*)\\b[^>]*?>");

  private final ArrayDeque<TreeNode> tree = new ArrayDeque<>();
  private final TreeNode projectNode;
  private TreeNode currentNode;
  private String baseIndent;

  static MavenTree read(File pomFile) {
    var reader = new TreeReader();
    return reader.readFile(pomFile);
  }

  private TreeReader() {
    this.projectNode = new TagNode("project");
    this.currentNode = projectNode;
    tree.push(projectNode);
  }

  MavenTree readFile(File file) {
    try {
      return readLines(new FileReader(file));
    } catch (FileNotFoundException e) {
      throw new UncheckedIOException(e);
    }
  }

  private void parseLine(String line) {
    Matcher matcher = XML_START_TAG.matcher(line);
    if (matcher.find()) {
      String tagName = matcher.group(1); // This is the tag name
      if (tagName.equals("project")) {
        // skip the root project tag
        currentNode.addChild(new LineNode(line));
        return;
      }
      if (baseIndent == null && isTopLevelElement(tagName)) {
        baseIndent = line.substring(0, matcher.start());
      }
      // Optionally, matcher.group(0) gives the full tag, e.g. <tag attr="...">
      var endTag = "</" + tagName + ">";
      if (line.contains(endTag)) {
        currentNode.addChild(SingleLineTagNode.of(tagName, line));
        return;
      }
      if (isSingleLineEmptyXmlTag(line)) {
        currentNode.addChild(new LineNode(line));
        return;
      }
      TreeNode tagNode = new TagNode(tagName);
      currentNode.addChild(tagNode);
      tagNode.addChild(new LineNode(line));
      tree.push(tagNode);
      currentNode = tagNode;
      return;
    }
    if (currentNode.isEndTag(line)) {
      currentNode.addChild(new LineNode(line));
      tree.pop();
      if (!tree.isEmpty()) {
        currentNode = tree.peek();
      }
      return;
    }

    currentNode.addChild(new LineNode(line));
  }

  private static boolean isTopLevelElement(String tagName) {
    return tagName.equals("parent")
      || tagName.equals("artifactId")
      || tagName.equals("dependencies")
      || tagName.equals("build");
  }

  private static final Pattern EMPTY_XML_TAG = Pattern.compile("<\\s*([a-zA-Z0-9:_-]+)\\s*/>");

  static boolean isSingleLineEmptyXmlTag(String line) {
    return EMPTY_XML_TAG.matcher(line).find();
  }


  private MavenTree readLines(Reader source) {
    try {
      try (LineNumberReader reader = new LineNumberReader(source)) {
        String line;
        while ((line = reader.readLine()) != null) {
          parseLine(line);
        }
      }
      return new MavenTree(projectNode, baseIndent);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

}
