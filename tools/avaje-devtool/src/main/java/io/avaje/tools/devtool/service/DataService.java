package io.avaje.tools.devtool.service;

import io.avaje.inject.Component;
import io.avaje.tools.devtool.data.*;
import io.avaje.tools.devtool.state.ApplicationRepository;
import io.avaje.tools.devtool.state.ApplicationState;
import io.avaje.tools.devtool.state.Task;

import java.util.List;
import java.util.stream.Stream;

@Component
public final class DataService {

  private static final System.Logger log = System.getLogger("app");

  private final ApplicationRepository repository;

  DataService(ApplicationRepository repository) {
    this.repository = repository;
  }

  public ApplicationState data() {
    return repository.state();
  }

  void refreshData(ApplicationState data) {
    repository.refreshData(data);
  }

  public List<KBaseSource> searchSources(String search, int limit) {
    if (search == null) return List.of();
    String[] tokens = asTokens(search);
    return searchSources(tokens, limit);
  }

  public List<MProject> searchProjects(String search, int limit) {
    if (search == null) return List.of();
    String[] tokens = asTokens(search);
    return searchProjects(tokens, limit);
  }

  public List<Task> searchTasks(String search, int limit) {
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

  public List<KBaseSource> searchSources(String[] tokens, int limit) {
    if (tokens == null || tokens.length == 0) {
      return List.of();
    }
    return data().dataSources().stream()
      .filter(source -> source.matchAll(tokens))
      .limit(limit)
      .toList();
  }

  public long addSource(String path) {
    return repository.addTasksSource(path);
  }

  ProjectFileSearch lastProjectScan;

  public ProjectFileSearch scanPathForProjects(String path) {
    lastProjectScan = ProjectFileSearch.matchProjectFiles(path);
    return lastProjectScan;
  }

  public void addScannedProjects() {

    if (lastProjectScan != null) {
      repository.addScannedProjects(lastProjectScan);
    } else {
      log.log(System.Logger.Level.WARNING, "No scanned projects to add");
    }

  }
}
