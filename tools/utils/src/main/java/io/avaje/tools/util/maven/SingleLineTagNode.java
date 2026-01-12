package io.avaje.tools.util.maven;

import java.util.List;

final class SingleLineTagNode implements TreeNode {

  private final String name;
  private final String line;
  private final String innerContent;

  static SingleLineTagNode from(String name, String innerContent) {
    return new SingleLineTagNode(name, "    <" + name + ">" + innerContent + "</" + name + ">", innerContent);
  }

  static SingleLineTagNode from(String name, String innerContent, String indent) {
    return new SingleLineTagNode(name, indent + "<" + name + ">" + innerContent + "</" + name + ">", innerContent);
  }

  SingleLineTagNode(String name, String line, String innerContent) {
    this.name = name;
    this.line = line;
    this.innerContent = innerContent;
  }

  static TreeNode of(String tagName, String line) {
    int start = line.indexOf(">");
    int end = line.lastIndexOf("<");
    String content = line.substring(start + 1, end).trim();
    return new SingleLineTagNode(tagName, line, content);
  }

  @Override
  public String name() {
    return name;
  }

  @Override
  public void asLines(List<String> lines) {
    lines.add(line);
  }

  @Override
  public String innerContent() {
    return innerContent;
  }

  @Override
  public String toString() {
    return line;
  }
}
