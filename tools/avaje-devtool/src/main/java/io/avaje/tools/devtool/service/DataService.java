package io.avaje.tools.devtool.service;

import io.avaje.inject.Component;
import io.avaje.tools.devtool.data.*;
import io.avaje.tools.devtool.state.*;

import java.util.List;
import java.util.stream.Stream;

import static java.lang.System.Logger.Level.INFO;

@Component
public final class DataService {

  private static final System.Logger log = System.getLogger("app");

  private final ApplicationRepository repository;
  private final UIState uiState = new UIState();

  DataService(ApplicationRepository repository) {
    this.repository = repository;
  }

  public ApplicationState data() {
    return repository.state();
  }

  void refreshData(ApplicationState data) {
    repository.refreshData(data);
  }

  public List<KBaseSource> searchTaskSources(String search, int limit) {
    if (search == null) return List.of();
    String[] tokens = asTokens(search);
    return searchTaskSources(tokens, limit);
  }

  public List<ProjectsSource> searchProjectSources(String search, int limit) {
    if (search == null) return List.of();
    String[] tokens = asTokens(search);
    return searchProjectSources(tokens, limit);
  }

  public List<MProject> searchProjects(String search, int limit) {
    if (search == null) return List.of();
    String[] tokens = asTokens(search);
    return searchProjects(tokens, limit);
  }

  public List<Task> searchTasks(String search, int limit) {
    var result = searchTasksInternal(search, limit);
    uiState.setTasks(search, result);
    return result;
  }

  private List<Task> searchTasksInternal(String search, int limit) {
    if (search == null) return List.of();
    String[] tokens = asTokens(search);
    return searchTasks(tokens, limit);
  }

  static String[] asTokens(String search) {
    String[] tokens = search.split(" ");
    return Stream.of(tokens).map(String::toLowerCase).toList().toArray(new String[0]);
  }

  private List<Task> searchTasks(String[] tokens, int limit) {
    if (tokens == null || tokens.length == 0) {
      return List.of();
    }
    return data().tasks().stream()
      .filter(task -> task.matchAll(tokens))
      .limit(limit)
      .toList();
  }

  private List<MProject> searchProjects(String[] tokens, int limit) {
    if (tokens == null || tokens.length == 0) {
      return List.of();
    }
    return data().projects().stream()
      .filter(project -> project.matchAll(tokens))
      .limit(limit)
      .toList();
  }

  public List<KBaseSource> searchTaskSources(String[] tokens, int limit) {
    if (tokens == null || tokens.length == 0) {
      return List.of();
    }
    return data().dataSources().stream()
      .filter(source -> source.matchAll(tokens))
      .limit(limit)
      .toList();
  }

  public List<ProjectsSource> searchProjectSources(String[] tokens, int limit) {
    if (tokens == null || tokens.length == 0) {
      return List.of();
    }
    return data().projectSources().stream()
      .filter(source -> source.matchAll(tokens))
      .limit(limit)
      .toList();
  }

  public AddTasksResult addSource(String path) {
    return repository.addTasksSource(path);
  }

  public ProjectFileSearch scanPathForProjects(String path) {
    var lastProjectScan = ProjectFileSearch.matchProjectFiles(path);
    return uiState.setProjectScan(lastProjectScan);
  }

  public void addScannedProjects() {
    var lastScan = uiState.lastProjectScan();
    if (lastScan != null) {
      repository.addScannedProjects(lastScan);
    } else {
      log.log(System.Logger.Level.WARNING, "No scanned projects to add");
    }
  }

  public AddTasksResult addTaskSource(String path) {
    return repository.addTasksSource(path);
  }

  public ModelProjectMaven workingDirectoryPom() {
    return repository.workingDirectoryPom();
  }

  public List<String> taskRun(String taskId) {
    Task currentTask = uiState.currentTask();
    if (currentTask != null && currentTask.uniqueTaskId().equals(taskId)) {
      return runTask(currentTask);
    } else {
      String msg = "No current task or taskId mismatch for running task: " + taskId;
      log.log(System.Logger.Level.WARNING, msg);
      return List.of(msg);
    }
  }

  private List<String> runTask(Task currentTask) {
    log.log(INFO, "Running task: " + currentTask.uniqueTaskId());

    var modelProjectMaven = repository.workingDirectoryPom();
    return TaskRunner.run(currentTask, modelProjectMaven);
  }

  public boolean hasCurrentProject() {
    return workingDirectoryPom() != null;
  }

  public Task findTask(String taskId) {
    return uiState.setCurrentTask(repository.findTask(taskId));
  }
}
