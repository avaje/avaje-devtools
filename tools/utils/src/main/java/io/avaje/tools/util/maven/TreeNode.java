package io.avaje.tools.util.maven;

import java.util.List;
import java.util.Optional;

public sealed interface TreeNode permits LineNode, SingleLineTagNode, TagNode {

  default String name() {
    return "";
  }

  default List<TreeNode> children() {
    return List.of();
  }

  default void addChild(TreeNode tagNode) {
    throw new UnsupportedOperationException();
  }

  default void addChildBefore(TreeNode child, String endTag) {
    throw new UnsupportedOperationException();
  }

  default boolean isEndTag(String line) {
    return false;
  }

  default void match(List<TreeNode> matching, String dotPath) {
    // do nothing by default
  }

  default Optional<TreeNode> match(String dotPath) {
    return Optional.empty();
  }

  void asLines(List<String> lines);

  String innerContent();
}
