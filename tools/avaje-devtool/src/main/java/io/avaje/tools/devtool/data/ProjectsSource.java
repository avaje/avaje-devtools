package io.avaje.tools.devtool.data;

import io.avaje.jsonb.Json;

import java.util.Comparator;
import java.util.Objects;

/**
 * Source of projects (typically a directory that contains projects).
 *
 * @param name
 * @param type The type of source (e.g. "directory")
 * @param path The directory path that contains the projects
 */
@Json
public record ProjectsSource(String name, String type, String path) implements Comparable<ProjectsSource> {

  private static final Comparator<ProjectsSource> ORDERING = Comparator
    .comparing(ProjectsSource::name)
    .thenComparing(ProjectsSource::path);

  public boolean matchAll(String[] tokens) {
    for (String token : tokens) {
      if (!name.toLowerCase().contains(token) || !path.toLowerCase().contains(token)) {
        return false;
      }
    }
    return true;
  }

  @Override
  public boolean equals(Object obj) {
    return obj instanceof ProjectsSource s && Objects.equals(path, s.path);
  }

  @Override
  public int hashCode() {
    return Objects.hash(path);
  }

  @Override
  public int compareTo(ProjectsSource other) {
    return ORDERING.compare(this, other);
  }
}
