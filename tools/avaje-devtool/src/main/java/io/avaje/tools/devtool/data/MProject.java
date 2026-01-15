package io.avaje.tools.devtool.data;

import io.avaje.jsonb.Json;

import java.util.Comparator;
import java.util.Objects;

@Json
public class MProject implements Comparable<MProject> {

  private static final Comparator<MProject> M_PROJECT_COMPARATOR =
    Comparator.comparing(MProject::groupId)
      .thenComparing(MProject::artifactId);

  private String name;
  private String groupId;
  private String artifactId;
  private String description;
  private String path;
  private Type type;

  @Json.Ignore
  private String searchText;

  @Override
  public int compareTo(MProject other) {
    return M_PROJECT_COMPARATOR.compare(this, other);
  }

  public boolean matchAll(String[] tokens) {
    for (String token : tokens) {
      if (!searchText.contains(token)) {
        return false;
      }
    }
    return true;
  }

  public void initialiseSearchText() {
    searchText = initSearchText();
  }

  private String initSearchText() {
    StringBuilder sb = new StringBuilder();
    if (name != null) {
      sb.append(name).append(' ');
    }
    if (groupId != null) {
      sb.append(groupId).append(' ');
    }
    if (artifactId != null) {
      sb.append(artifactId).append(' ');
    }
    if (description != null) {
      sb.append(description).append(' ');
    }
    return sb.toString().toLowerCase();
  }

  public enum Type {
    MAVEN,
    GRADLE,
    OTHER
  }

  public String key() {
    return path;
  }

  public String name() {
    return name;
  }

  public MProject setName(String name) {
    this.name = name;
    return this;
  }

  public String groupId() {
    return groupId;
  }

  public MProject setGroupId(String groupId) {
    this.groupId = groupId;
    return this;
  }

  public String artifactId() {
    return artifactId;
  }

  public MProject setArtifactId(String artifactId) {
    this.artifactId = artifactId;
    return this;
  }

  public String description() {
    return description;
  }

  public MProject setDescription(String description) {
    this.description = description;
    return this;
  }

  public String path() {
    return path;
  }

  public MProject setPath(String path) {
    this.path = path;
    return this;
  }

  public Type type() {
    return type;
  }

  public MProject setType(Type type) {
    this.type = type;
    return this;
  }

  @Override
  public int hashCode() {
    return Objects.hash(path);
  }

  @Override
  public boolean equals(Object obj) {
    return obj instanceof MProject p && Objects.equals(path, p.path);
  }
}
