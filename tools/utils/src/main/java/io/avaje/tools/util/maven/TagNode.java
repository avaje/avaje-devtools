package io.avaje.tools.util.maven;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

final class TagNode implements TreeNode {

  private final List<TreeNode> children = new ArrayList<>();
  private final String name;
  private final String detectEndTag;

  TagNode(String name) {
    this.name = name;
    this.detectEndTag = "</" + name + ">";
  }

  @Override
  public String name() {
    return name;
  }

  @Override
  public List<TreeNode> children() {
    return children;
  }

  @Override
  public String innerContent() {
    return "";
  }

  @Override
  public void addChild(TreeNode child) {
    children.add(child);
  }

  @Override
  public void addChildBefore(TreeNode child, Set<String> tags) {
    for (TreeNode treeNode : children) {
      if (treeNode instanceof TagNode tag) {
        if (tags.contains(tag.name())) {
          int index = children.indexOf(treeNode);
          children.add(index, child);
          return;
        }
      }
    }
    addChildBefore(child, "</project>");
  }

  @Override
  public void addChildBefore(TreeNode child, String endTag) {
    for (TreeNode treeNode : children) {
      if (treeNode instanceof LineNode) {
        if (treeNode.toString().contains(endTag)) {
          int index = children.indexOf(treeNode);
          children.add(index, child);
          return;
        }
      }
    }

    children.add(child);
  }

  @Override
  public boolean isEndTag(String line) {
    return line.contains(detectEndTag);
  }

  @Override
  public void asLines(List<String> lines) {
    for (TreeNode child : children) {
      child.asLines(lines);
    }
  }

  @Override
  public void match(List<TreeNode> matching, String dotPath) {
    int pos = dotPath.indexOf(".");
    if (pos > -1) {
      String current = dotPath.substring(0, pos);
      String remaining = dotPath.substring(pos + 1);
      for (TreeNode child : children) {
        if (child.name().equals(current)) {
          child.match(matching, remaining);
        }
      }
    } else {
      for (TreeNode child : children) {
        if (child.name().equals(dotPath)) {
          matching.add(child);
        }
      }
    }
  }

  @Override
  public Optional<TreeNode> match(String dotPath) {
    int pos = dotPath.indexOf(".");
    if (pos > -1) {
      String current = dotPath.substring(0, pos);
      String remaining = dotPath.substring(pos + 1);
      for (TreeNode child : children) {
        if (child.name().equals(current)) {
          return child.match(remaining);
        }
      }
    } else {
      for (TreeNode child : children) {
        if (child.name().equals(dotPath)) {
          return Optional.of(child);
        }
      }
    }
    return Optional.empty();
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    for (TreeNode child : children) {
      sb.append(child.toString()).append("\n");
    }
    return sb.toString();
  }
}
