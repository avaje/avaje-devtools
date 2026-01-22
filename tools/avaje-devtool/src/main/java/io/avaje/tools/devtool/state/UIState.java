package io.avaje.tools.devtool.state;

import io.avaje.tools.devtool.service.ProjectFileSearch;

import java.util.List;

import static java.util.Objects.requireNonNullElse;

public class UIState {

  private String taskSearch;
  private List<Task> tasks = List.of();
  private ProjectFileSearch lastProjectScan;
  private Task currentTask;

  public void setTasks(String taskSearch, List<Task> tasks) {
    this.taskSearch = taskSearch;
    this.tasks = requireNonNullElse(tasks, List.of());
    this.currentTask = null;
  }

  public ProjectFileSearch setProjectScan(ProjectFileSearch lastProjectScan) {
    this.lastProjectScan = lastProjectScan;
    return lastProjectScan;
  }

  public ProjectFileSearch lastProjectScan() {
    return lastProjectScan;
  }

  public Task currentTask() {
    if (currentTask != null) return currentTask;
    if (!tasks.isEmpty()) {
      return tasks.getFirst();
    }
    return null;
  }

  public Task setCurrentTask(Task currentTask) {
    this.currentTask = currentTask;
    return currentTask;
  }

}
