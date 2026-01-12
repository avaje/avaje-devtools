package io.avaje.tools.devtool.data;

import io.avaje.jsonb.Json;

import java.util.ArrayList;
import java.util.List;

import static java.util.Objects.requireNonNull;

@Json
public class MProjects {

  private final List<MProject> projects;
  private final List<MDataSource> dataSources;

  public MProjects(List<MProject> projects, List<MDataSource> dataSources) {
    this.projects = requireNonNull(projects);
    this.dataSources = requireNonNull(dataSources);
  }

  public MProjects() {
    this.projects = new ArrayList<>();
    this.dataSources = new ArrayList<>();
  }

  public List<MProject> projects() {
    return projects;
  }

  public List<MDataSource> dataSources() {
    return dataSources;
  }
}
