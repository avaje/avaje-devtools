package io.avaje.tools.devtool.state;

import io.avaje.tools.devtool.data.ApplicationModel;
import io.avaje.tools.devtool.data.KBaseSource;
import io.avaje.tools.devtool.data.MProject;
import io.avaje.tools.devtool.data.ProjectsSource;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Collections.synchronizedList;

public final class ApplicationState {

  private final List<MProject> projects = synchronizedList(new ArrayList<>());
  private final List<Task> tasks = synchronizedList(new ArrayList<>());
  private final List<KBaseSource> dataSources = synchronizedList(new ArrayList<>());
  private final List<ProjectsSource> projectSources = synchronizedList(new ArrayList<>());

  public List<MProject> projects() {
    return projects;
  }

  public List<Task> tasks() {
    return tasks;
  }

  public List<KBaseSource> dataSources() {
    return dataSources;
  }

  public List<ProjectsSource> projectSources() {
    return projectSources;
  }

  public synchronized ApplicationModel toApplicationModel() {
    return new ApplicationModel()
      .setProjects(projects)
      .setDataSources(dataSources)
      .setProjectSources(projectSources);
  }

  /**
   * Add the data source if not already present based on path.
   */
  public synchronized void addDataSource(KBaseSource newSource) {
    dataSources.stream()
      .filter(s -> s.path().equals(newSource.path()))
      .findAny()
      .ifPresentOrElse(_ -> {}, () -> dataSources.add(newSource));
  }

  /**
   * Add tasks ensuring no duplicates based on task key.
   */
  public synchronized void addTasks(List<Task> newTasks) {
    tasks.stream()
      .collect(Collectors.toMap(Task::key, t -> t))
      .putAll(newTasks.stream().collect(Collectors.toMap(Task::key, t -> t)));

    // resort after adding
    tasks.sort(null);
  }

  /**
   * Add the project source if not already present based on path.
   */
  public synchronized void addProjectSource(ProjectsSource newProjectSource) {
    projects.stream()
      .filter(s -> s.path().equals(newProjectSource.path()))
      .findAny()
      .ifPresentOrElse(_ -> {}, () -> projectSources.add(newProjectSource));
  }

  public synchronized void addProjects(List<MProject> list) {
    projects.stream()
      .collect(Collectors.toMap(MProject::key, p -> p))
      .putAll(list.stream().collect(Collectors.toMap(MProject::key, p -> p)));

    // resort after adding
    projects.sort(null);
  }

  public synchronized void init(List<MProject> loadedProjects,
                                List<KBaseSource> loadedSources,
                                List<ProjectsSource> loadedProjectSources,
                                List<Task> loadedTasks) {
     projects.addAll(loadedProjects);
     tasks.addAll(loadedTasks);
     dataSources.addAll(loadedSources);
     projectSources.addAll(loadedProjectSources);
  }
}
