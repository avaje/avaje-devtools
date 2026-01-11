package io.ebean.tools.init;

public class Dependency {

  private final String key;

  private String comment;

  private String groupId;
  private String artifactId;
  private String version;
  private String scope;
  private String type;
  private String optional;
  private String classifier;

  public Dependency(String raw, String comment) {
    this(raw);
    this.comment = comment;
  }

  public Dependency(String raw) {
    final String[] vals = raw.split(":");
    final int length = vals.length;

    groupId = vals[0];
    if (length > 1) {
      artifactId = vals[1];
    }
    if (length > 2) {
      version = vals[2];
    }
    if (length > 3) {
      scope = vals[3];
    }

    key = groupId + ":" + artifactId;
  }

  public String getKey() {
    return key;
  }

  public String getGroupId() {
    return groupId;
  }

  public String getArtifactId() {
    return artifactId;
  }

  public String getVersion() {
    return version;
  }

  public String getScope() {
    return scope;
  }

  public String getComment() {
    return comment;
  }

  public void setComment(String comment) {
    this.comment = comment;
  }

  public String type() {
    return type;
  }

  public Dependency setType(String type) {
    this.type = type;
    return this;
  }

  public String optional() {
    return optional;
  }

  public Dependency setOptional(String optional) {
    this.optional = optional;
    return this;
  }

  public String classifier() {
    return classifier;
  }

  public Dependency setClassifier(String classifier) {
    this.classifier = classifier;
    return this;
  }

  public String gradle(String format) {
    String gradleScope = (scope == null) ? "compile" : scope;
    return String.format(format, gradleScope, groupId, artifactId, version);
  }

  public Dependency withScope(String scope) {
    this.scope = scope;
    return this;
  }
}
