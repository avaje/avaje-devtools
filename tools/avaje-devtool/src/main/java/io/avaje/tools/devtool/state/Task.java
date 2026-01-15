package io.avaje.tools.devtool.state;

import io.avaje.tools.devtool.data.TaskMeta;

import java.util.Objects;

public record Task(TaskMeta meta, String preview, String all) implements Comparable<Task> {

  public String key() {
    return meta.key();
  }

  public String displayName() {
    return meta.displayName();
  }

  public String type() {
    return meta.type();
  }

  public String description() {
    return meta.description();
  }

  public boolean matchAll(String[] tokens) {
    for (String token : tokens) {
      if (!all.contains(token)) {
        return false;
      }
    }
    return true;
  }

  @Override
  public int compareTo(Task other) {
    return Integer.compare(meta.priority(), other.meta.priority());
  }

  @Override
  public boolean equals(Object obj) {
    return obj instanceof Task t && meta.key().equals(t.meta.key());
  }

  @Override
  public int hashCode() {
    return Objects.hash(meta.key());
  }
}
