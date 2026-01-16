package io.avaje.tools.devtool.state;

import io.avaje.tools.devtool.service.ProjectFileSearch;

import java.util.List;

import static java.util.Objects.requireNonNullElse;

public class UIState {

  private String taskSearch;
  private List<Task> tasks = List.of();
  private ProjectFileSearch lastProjectScan;

  public void setTasks(String taskSearch, List<Task> tasks) {
    this.taskSearch = taskSearch;
    this.tasks = requireNonNullElse(tasks, List.of());
  }

  public ProjectFileSearch setProjectScan(ProjectFileSearch lastProjectScan) {
    this.lastProjectScan = lastProjectScan;
    return lastProjectScan;
  }

  public ProjectFileSearch lastProjectScan() {
    return lastProjectScan;
  }

  public Task currentTask() {
    if (!tasks.isEmpty()) {
      return tasks.getFirst();
    }
    return null;
  }
}
