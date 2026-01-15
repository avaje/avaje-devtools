package io.avaje.tools.devtool.data;

import io.avaje.jsonb.Json;

import java.util.ArrayList;
import java.util.List;

import static java.util.Objects.requireNonNull;

/**
 * The persistent application model containing projects, data sources and project sources.
 */
@Json
public class ApplicationModel {

  private List<KBaseSource> dataSources = new ArrayList<>();
  private List<ProjectsSource> projectSources = new ArrayList<>();
  private List<MProject> projects = new ArrayList<>();

  public List<MProject> projects() {
    return projects;
  }

  public List<KBaseSource> dataSources() {
    return dataSources;
  }

  public List<ProjectsSource> projectSources() {
    return projectSources;
  }

  public void setProjects(List<MProject> combined, ProjectsSource newProjectSource) {
    projects.clear();
    projects.addAll(combined);
    projectSources.stream()
      .filter(p -> p.path().equals(newProjectSource.path()))
      .findAny()
      .ifPresentOrElse(_ -> {}, () -> projectSources.add(newProjectSource));
  }

  public ApplicationModel setProjects(List<MProject> projects) {
    this.projects = requireNonNull(projects);
    return this;
  }

  public ApplicationModel setDataSources(List<KBaseSource> dataSources) {
    this.dataSources = requireNonNull(dataSources);
    return this;
  }

  public ApplicationModel setProjectSources(List<ProjectsSource> projectSources) {
    this.projectSources = requireNonNull(projectSources);
    return this;
  }
}
