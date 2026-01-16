package io.avaje.tools.devtool.service;

import io.avaje.tools.util.maven.MavenTree;

import java.io.File;
import java.util.*;
import java.util.stream.Stream;

public class ModelProjectMaven implements Comparable<ModelProjectMaven> {

  private static final  Comparator<ModelProjectMaven> NATURAL_ORDER =
    Comparator.comparing(ModelProjectMaven::groupId)
      .thenComparing(ModelProjectMaven::artifactId);

  private final MavenTree pom;
  private final String relativePath;
  private final File projectFile;
  private final String groupId;
  private final String artifactId;
  private final String name;
  private final String description;
  private final ModelProjectMaven parent;
  private final List<ModelProjectMaven> modules = new ArrayList<>();
  private final String searchText;

  public ModelProjectMaven(MavenTree pom, String relativePath, File projectFile, ModelProjectMaven parent) {
    this.pom = pom;
    this.relativePath = relativePath;
    this.projectFile = projectFile;
    this.groupId = pom.groupId();
    this.artifactId = pom.artifactId();
    this.name = pom.name();
    this.description = pom.description();
    this.parent = parent;

    var searchTokens = new HashSet<String>();
    searchTokens.add(groupId.toLowerCase());
    searchTokens.add(artifactId.toLowerCase());

    var search = new StringBuilder();
    search.append(groupId.toLowerCase()).append(':').append(artifactId.toLowerCase());
    if (name != null) {
      for (String s : name.toLowerCase().split(" ")) {
        if (!s.isBlank() && searchTokens.add(s)){
          search.append(' ').append(s);
        }
      }
    }
    for (String s : relativePath.toLowerCase().split("/")) {
      if (!s.isBlank() && !s.equals("pom.xml")) {
        if (searchTokens.add(s)){
          search.append(' ').append(s);
        }
      }
    }
    if (parent == null) {
      search.append(" (*)");
    }
    this.searchText = search.toString();
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

  public String gav() {
    return groupId + ':' + artifactId;
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

  public String relativePath() {
    return relativePath;
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
