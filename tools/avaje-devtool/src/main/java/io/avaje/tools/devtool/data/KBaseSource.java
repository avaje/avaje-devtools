package io.avaje.tools.devtool.data;

import io.avaje.jsonb.Json;

import java.util.Comparator;
import java.util.Objects;

/**
 * Typically a directory that contains "knowledge bases" which contain "tasks".
 *
 * @param name
 * @param type The type of data source (e.g. "directory")
 * @param path The directory path that containers the knowledge bases
 */
@Json
public record KBaseSource(String name, String type, String path) implements Comparable<KBaseSource> {

  private static final Comparator<KBaseSource> ORDERING = Comparator.comparing(KBaseSource::name).thenComparing(KBaseSource::path);

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
    return obj instanceof KBaseSource s && Objects.equals(path, s.path);
  }

  @Override
  public int hashCode() {
    return Objects.hash(path);
  }

  @Override
  public int compareTo(KBaseSource other) {
    return ORDERING.compare(this, other);
  }
}
