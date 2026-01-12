package io.avaje.tools.util.maven;

import java.util.List;

record LineNode(String line) implements TreeNode {

  @Override
  public String toString() {
    return line;
  }

  @Override
  public void asLines(List<String> lines) {
    lines.add(line);
  }

  @Override
  public String innerContent() {
    return line;
  }

}
