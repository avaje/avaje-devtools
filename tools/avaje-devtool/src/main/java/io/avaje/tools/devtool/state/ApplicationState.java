package io.avaje.tools.devtool.state;

import io.avaje.tools.devtool.data.ApplicationModel;
import io.avaje.tools.devtool.data.KBaseSource;
import io.avaje.tools.devtool.data.MProject;
import io.avaje.tools.devtool.data.ProjectsSource;

import java.util.*;

public final class ApplicationState {

  private final Map<String, MProject> projects = new HashMap<>();
  private final Map<String, Task> tasks = new HashMap<>();
  private final Map<String, KBaseSource> dataSources = new HashMap<>();
  private final Map<String, ProjectsSource> projectSources = new HashMap<>();

  private List<Task> sortedTasks = new ArrayList<>();
  private List<MProject> sortedProjects = new ArrayList<>();
  private List<KBaseSource> sortedDataSources = new ArrayList<>();
  private List<ProjectsSource> sortedProjectSources = new ArrayList<>();

  public List<MProject> projects() {
    return sortedProjects;
  }

  public List<Task> tasks() {
    return sortedTasks;
  }

  public List<KBaseSource> dataSources() {
    return sortedDataSources;
  }

  public List<ProjectsSource> projectSources() {
    return sortedProjectSources;
  }

  public synchronized ApplicationModel toApplicationModel() {
    return new ApplicationModel()
      .setProjects(sortedProjects)
      .setDataSources(sortedDataSources)
      .setProjectSources(sortedProjectSources);
  }

  /**
   * Add the data source if not already present based on path.
   */
  public synchronized void addDataSource(KBaseSource newSource) {
    addDataSources(List.of(newSource));
  }

  private void addDataSources(List<KBaseSource> newSources) {
    for (KBaseSource newSource : newSources) {
      dataSources.putIfAbsent(newSource.path(), newSource);
    }
    sortedDataSources = dataSources.values().stream().sorted(KBaseSource.NAME_ORDER).toList();
  }

  /**
   * Add tasks ensuring no duplicates based on task key.
   *
   * @return The total number of tasks after adding
   */
  public synchronized int addTasks(List<Task> newTasks) {
    for (Task newTask : newTasks) {
      tasks.putIfAbsent(newTask.uniqueTaskId(), newTask);
    }
    sortedTasks = tasks.values().stream().sorted(Task.DISPLAY_ORDER).toList();
    return tasks.size();
  }

  /**
   * Add the project source if not already present based on path.
   */
  public synchronized void addProjectSource(ProjectsSource newProjectSource) {
    addProjectSources(List.of(newProjectSource));
  }

  private void addProjectSources(List<ProjectsSource> newProjectSources) {
    for (ProjectsSource newProjectSource : newProjectSources) {
      projectSources.putIfAbsent(newProjectSource.path(), newProjectSource);
    }
    sortedProjectSources = projectSources.values().stream().sorted(ProjectsSource.NAME_ORDER).toList();
  }

  public synchronized void addProjects(List<MProject> list) {
    for (MProject mProject : list) {
      projects.putIfAbsent(mProject.path(), mProject);
    }
    sortedProjects = projects.values().stream().sorted(MProject.GAV_ORDER).toList();
  }

  public synchronized void init(List<MProject> loadedProjects,
                                List<KBaseSource> loadedSources,
                                List<ProjectsSource> loadedProjectSources,
                                List<Task> loadedTasks) {
    addProjects(loadedProjects);
    addTasks(loadedTasks);
    addDataSources(loadedSources);
    addProjectSources(loadedProjectSources);
  }
}
