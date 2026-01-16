package io.avaje.tools.devtool.state;

import io.avaje.recordbuilder.RecordBuilder;
import io.avaje.tools.devtool.data.TaskMeta;

import java.util.Comparator;
import java.util.Objects;

@RecordBuilder
public record Task(String uniqueTaskId, TaskMeta meta, String preview, String all, String displayOrder, java.io.File parentDir) {

  public static final Comparator<Task> DISPLAY_ORDER = Comparator.comparing(Task::displayName);

  public String displayName() {
    return meta.displayName();
  }

  public String type() {
    return meta.type();
  }

  public String description() {
    return meta.description();
  }

  public boolean isAction() {
    return "action".equalsIgnoreCase(type());
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
  public boolean equals(Object obj) {
    return obj instanceof Task t && uniqueTaskId.equals(t.uniqueTaskId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(uniqueTaskId);
  }
}
