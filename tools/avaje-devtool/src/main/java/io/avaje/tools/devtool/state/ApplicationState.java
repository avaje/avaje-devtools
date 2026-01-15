package io.avaje.tools.devtool.state;

import io.avaje.tools.devtool.data.ApplicationModel;
import io.avaje.tools.devtool.data.KBaseSource;
import io.avaje.tools.devtool.data.MProject;
import io.avaje.tools.devtool.data.ProjectsSource;

import java.util.*;
import java.util.stream.Collectors;

import static java.util.Collections.synchronizedList;
import static java.util.Collections.synchronizedSet;

public final class ApplicationState {

  private final Set<MProject> projects = synchronizedSet(new TreeSet<>());
  private final List<Task> tasks = synchronizedList(new ArrayList<>());
  private final Set<KBaseSource> dataSources = synchronizedSet(new TreeSet<>());
  private final Set<ProjectsSource> projectSources = synchronizedSet(new TreeSet<>());

  public Collection<MProject> projects() {
    return projects;
  }

  public SequencedCollection<Task> tasks() {
    return tasks;
  }

  public Collection<KBaseSource> dataSources() {
    return dataSources;
  }

  public Collection<ProjectsSource> projectSources() {
    return projectSources;
  }

  public synchronized ApplicationModel toApplicationModel() {
    return new ApplicationModel()
      .setProjects(new ArrayList<>(projects))
      .setDataSources(new ArrayList<>(dataSources))
      .setProjectSources(new ArrayList<>(projectSources));
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
    projectSources.add(newProjectSource);
//    projects.stream()
//      .filter(s -> s.path().equals(newProjectSource.path()))
//      .findAny()
//      .ifPresentOrElse(
//        _ -> {},
//        () ->
//          projectSources.add(newProjectSource));
  }

  public synchronized void addProjects(List<MProject> list) {
//    Set<MProject> copy = new HashSet<>(projects);
//    copy.addAll(list);
    projects.addAll(list);
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
