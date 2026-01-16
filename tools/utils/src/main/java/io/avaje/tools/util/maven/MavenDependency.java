package io.avaje.tools.util.maven;

import io.avaje.recordbuilder.RecordBuilder;
import org.jspecify.annotations.NonNull;

@RecordBuilder
public record MavenDependency (
  @NonNull String groupId,
  @NonNull String artifactId,
  String version,
  String scope,
  String optional,
  String classifier,
  String type) {

  public String key() {
    return groupId + ':' + artifactId;
  }

  public static MavenDependencyBuilder builder() {
    return MavenDependencyBuilder.builder();
  }

  public static MavenDependencyBuilder builder(MavenDependency from) {
    return MavenDependencyBuilder.builder(from);
  }

  public static MavenDependencyBuilder of(String raw) {
    final String[] vals = raw.split(":");
    final int length = vals.length;

    var builder = builder();
    String groupId = vals[0];
    builder.groupId(groupId);
    if (length > 1) {
      builder.artifactId(vals[1]);
    }
    if (length > 2) {
      builder.version(vals[2]);
    }
    if (length > 3) {
      builder.scope(vals[3]);
    }
    return builder;
  }

}
