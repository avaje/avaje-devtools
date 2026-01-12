package io.avaje.tools.devtool.data;

import io.avaje.jsonb.Json;

@Json
public class MProject {

  private String name;
  private String description;
  private String path;
  private Type type;

  public enum Type {
    MAVEN,
    GRADLE,
    OTHER
  }

  public String name() {
    return name;
  }

  public MProject setName(String name) {
    this.name = name;
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
}
