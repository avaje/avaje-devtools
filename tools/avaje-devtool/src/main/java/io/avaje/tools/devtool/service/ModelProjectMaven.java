package io.avaje.tools.devtool.service;

import io.avaje.tools.util.maven.MavenTree;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

public class ModelProjectMaven implements Comparable<ModelProjectMaven> {

  private static final  Comparator<ModelProjectMaven> NATURAL_ORDER =
    Comparator.comparing(ModelProjectMaven::groupId)
      .thenComparing(ModelProjectMaven::artifactId);

  private final MavenTree pom;
  private final File projectFile;
  private final String groupId;
  private final String artifactId;
  private final String name;
  private final String description;
  private final ModelProjectMaven parent;
  private final List<ModelProjectMaven> modules = new ArrayList<>();
  private final String searchText;

  public ModelProjectMaven(MavenTree pom, File projectFile, ModelProjectMaven parent) {
    this.pom = pom;
    this.projectFile = projectFile;
    this.groupId = pom.groupId();
    this.artifactId = pom.artifactId();
    this.name = pom.name();
    this.description = pom.description();
    this.parent = parent;

    StringBuilder search = new StringBuilder();
    search.append(groupId).append(':').append(artifactId);
    if (name != null && !name.equals(artifactId)) {
      search.append(' ').append(name);
    }
    if (parent == null) {
      search.append(" (*)");
    }
    this.searchText = search.toString().toLowerCase();
  }


  @Override
  public int compareTo(ModelProjectMaven other) {
    return NATURAL_ORDER.compare(this, other);
  }

  public void addModule(ModelProjectMaven modulePom) {
    modules.add(modulePom);
  }

  public MavenTree pom() {
    return pom;
  }

  public File projectFile() {
    return projectFile;
  }

  public ModelProjectMaven parent() {
    return parent;
  }

  public String groupId() {
    return groupId;
  }

  public String artifactId() {
    return artifactId;
  }

  public String name() {
    return name;
  }

  public String description() {
    return description;
  }

  public String searchText() {
    return searchText;
  }

  public List<ModelProjectMaven> modules() {
    return modules;
  }

  public Stream<ModelProjectMaven> flattened() {
    return Stream.concat(
      Stream.of(this), // Include the current node
      modules.stream().flatMap(ModelProjectMaven::flattened) // Recursively flatten children
    );
  }

  @Override
  public String toString() {
    return groupId + ':' +  artifactId + (parent == null ? " (*)" : "");
  }

}
