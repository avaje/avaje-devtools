package io.ebean.tools.init;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class GradleBuildWriter {

  private final GradleBuild build;

  private final File newBuild;

  private final FileWriter writer;

  private final List<String> sourceLines;

  private String newLine = "\n";

  private List<Dependency> additionalDependencies;

  private String ebeanPlugin;

  public GradleBuildWriter(GradleBuild build, File newBuild) throws IOException {
    this.build = build;
    this.sourceLines = build.getLines();
    this.newBuild = newBuild;
    this.writer = new FileWriter(newBuild);
  }

  public void addEbeanPlugin(String ebeanPlugin) {
    //id('io.ebean') version '11.41.1'
    this.ebeanPlugin = ebeanPlugin;
  }

  public void addDependencies(List<Dependency> additionalDependencies) {
    this.additionalDependencies = additionalDependencies;
  }

  public void writeToFile() throws IOException {

    final int start = writePlugins();

    final int i1 = build.insertDependencyLine();

    for (int i = start; i < i1; i++) {
      writer.append(sourceLines.get(i));
      writer.append(newLine);
    }

    writeDependencies();

    for (int i = i1; i < sourceLines.size(); i++) {
      writer.append(sourceLines.get(i));
      writer.append(newLine);
    }

    writer.close();
  }

  private void writeDependencies() throws IOException {

    if (build.addDependenciesSection()) {
      writer.append("dependencies {").append(newLine);
    }
    String format = build.hasOpenBracketForDependencies() ? "  %s('%s:%s:%s')" : "  %s '%s:%s:%s'";

    for (Dependency dep : additionalDependencies) {
      writer.append(dep.gradle(format)).append(newLine);
    }
    if (build.addDependenciesSection()) {
      writer.append("}").append(newLine);
    }
  }

  private int writePlugins() throws IOException {

    // has plugins section?
    //   write to end of plugin
    //   write ebean plugin
    // write end

//    String addPlugin = ebeanPlugin;
//    if (!build.hasOpenBracketForPlugins()) {
//      addPlugin = addPlugin.replace('(', ' ');
//      addPlugin = addPlugin.replace(')', ' ');
//      addPlugin = addPlugin.trim();
//    }

    if (build.hasPluginSection()) {
      int writeTo = build.pluginsEnd();
      for (int i = 0; i < writeTo; i++) {
        writer.append(sourceLines.get(i));
        writer.append(newLine);
      }
      writer.append("  ").append(ebeanPlugin).append(newLine);
      return writeTo;
    } else {

      writer.append("plugins {").append(newLine);
      writer.append("  ").append(ebeanPlugin).append(newLine);
      writer.append("}").append(newLine).append(newLine);
    }

    return 0;
  }

}
