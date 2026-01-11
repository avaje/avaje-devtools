package io.ebean.tools.init;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.util.ArrayList;
import java.util.List;

import static io.ebean.tools.init.GradleBuild.ParseState.DEPENDENCIES;
import static io.ebean.tools.init.GradleBuild.ParseState.OUTSIDE;
import static io.ebean.tools.init.GradleBuild.ParseState.PLUGINS;

public class GradleBuild {

  private final File build;

  private List<String> lines;

  private List<GradleDependency> dependencies = new ArrayList<>();
  private List<GradlePlugin> plugins = new ArrayList<>();

  private GradlePlugin ebeanPlugin;

  private int dependenciesStart;
  private int dependenciesEnd = Integer.MAX_VALUE;

  private int pluginsStart;
  private int pluginsEnd = Integer.MAX_VALUE;

  private String baseIndent = "  ";

  GradleBuild(File build) {
    this.build = build;
    this.lines = readLines();
    parseLines();
  }

  int pluginsEnd() {
    return pluginsEnd;
  }

  boolean isNewProject() {
    return ebeanPlugin == null && !hasDependencyEbean();
  }

  String getBaseIndent() {
    return baseIndent;
  }

  List<String> getLines() {
    return lines;
  }

  boolean hasEbeanPlugin() {
    for (GradlePlugin plugin : plugins) {
      if (plugin.contains("io.ebean")) {
        return true;
      }
    }
    return false;
  }

  boolean hasDependencyEbean() {
    return hasDependency("io.ebean", "ebean");
  }

  boolean hasDependencyEbeanQueryBean() {
    return hasDependency("io.ebean", "ebean-querybean");
  }

  boolean hasDependencyEbeanQueryBeanGenerator() {
    return hasDependency("io.ebean", "querybean-generator");
  }

  boolean hasDependency(String groupId, String artifactId) {
    String match = groupId+":"+artifactId;
    for (GradleDependency dependency : dependencies) {
      if (dependency.contains(match)) {
        return true;
      }
    }
    return false;
  }

  private void parseLines() {
    if (lines != null) {
      for (int i = 0; i < lines.size(); i++) {
        parseLine(lines.get(i), i);
      }
    }
  }

  public boolean hasPluginSection() {
    return pluginsEnd != Integer.MAX_VALUE;
  }

  private static final String[] COMPILE_SCOPES = {"compile", "api", "implementation"};

  public boolean addDependenciesSection() {
    return dependenciesStart == 0;
  }

  public int insertDependencyLine() {

    if (dependencies.isEmpty() && dependenciesStart == 0) {
      return lines.size();
    }

    int lastCompileLine = dependenciesStart + 1;
    for (GradleDependency dependency : dependencies) {
      for (String compileScope : COMPILE_SCOPES) {
        if (dependency.contains(compileScope)) {
          lastCompileLine = dependency.line + 1;
        }
      }
    }
    return lastCompileLine;
  }

  public boolean hasOpenBracketForDependencies() {
    for (GradleDependency dependency : dependencies) {
      if (dependency.contains("(")) {
        return true;
      }
    }
    return false;
  }

  public boolean hasOpenBracketForPlugins() {
    for (GradlePlugin plugin : plugins) {
      if (plugin.contains("(")) {
        return true;
      }
    }
    return false;
  }


  enum ParseState {
    OUTSIDE,
    PLUGINS,
    DEPENDENCIES
  }

  private ParseState state = ParseState.OUTSIDE;

  private void parseLine(String line, int lineIndex) {

    String lineTrimmed = line.trim();

    if (state == ParseState.PLUGINS) {
      processPluginLine(line, lineIndex, lineTrimmed);
      return;
    }

    if (state == ParseState.DEPENDENCIES) {
      processDependencyLine(line, lineIndex, lineTrimmed);
      return;
    }

    if (lineTrimmed.equals("plugins {")) {
      pluginsStart = lineIndex;
      state = ParseState.PLUGINS;

    } else if (lineTrimmed.equals("dependencies {")) {
      dependenciesStart = lineIndex;
      state = ParseState.DEPENDENCIES;
    }
  }

  private void processDependencyLine(String line, int lineIndex, String lineTrimmed) {
    if (lineTrimmed.equals("}")) {
      dependenciesEnd = lineIndex;
      state = ParseState.OUTSIDE;
    } else {
      dependencies.add(new GradleDependency(lineIndex, line));
    }
  }

  private void processPluginLine(String line, int lineIndex, String lineTrimmed) {
    if (lineTrimmed.equals("}")) {
      pluginsEnd = lineIndex;
      state = ParseState.OUTSIDE;
    } else {
      plugins.add(new GradlePlugin(lineIndex, line));
    }
  }

  private List<String> readLines() {

    try {
      List<String> lines = new ArrayList<>();
      try (LineNumberReader reader = new LineNumberReader(new FileReader(build))) {
        String line;
        while ((line = reader.readLine()) != null) {
          lines.add(line);
        }
      }
      return lines;
    } catch (IOException e) {
      e.printStackTrace();
      return null;
    }
  }

  public File getBuildFile() {
    return build;
  }

  static class GradlePlugin {

    final int line;
    final String content;

    private GradlePlugin(int line, String content) {
      this.line = line;
      this.content = content;
    }
    boolean contains(String match) {
      return content.contains(match);
    }
  }

  static class GradleDependency {

    final int line;
    final String content;

    private GradleDependency(int line, String content) {
      this.line = line;
      this.content = content;
    }

    boolean contains(String match) {
      return content.contains(match);
    }
  }

}
