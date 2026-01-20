package io.avaje.tools.devtool.service;

import io.avaje.tools.devtool.data.TaskMeta;
import io.avaje.tools.devtool.state.Task;
import io.avaje.tools.util.maven.MavenDependency;
import io.avaje.tools.util.maven.MavenTree;

import java.io.File;
import java.io.IOException;
import java.lang.System.Logger.Level;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static java.lang.System.Logger.Level.*;
import static java.nio.file.Files.*;

public final class TaskRunner {

  private static final System.Logger log = System.getLogger("app");

  private final Set<String> addedDependencies = new LinkedHashSet<>();
  private final Set<String> skippedDependencies = new LinkedHashSet<>();

  private final Task task;
  private final ModelProjectMaven pom;
  private final List<String> output = new ArrayList<>();

  private TaskRunner(Task task, ModelProjectMaven pom) {
    this.task = task;
    this.pom = pom;
  }

  public static List<String> run(Task task, ModelProjectMaven pom) {
    return new TaskRunner(task, pom).run();
  }

  public List<String> run() {
    File taskDir = task.parentDir();
    applyPomChanges(taskDir);
    applyActions();
    return output;
  }

  private void applyActions() {
    List<TaskMeta.TaskAction> actions = task.meta().actions();
    if (actions == null || actions.isEmpty()) {
      return;
    }
    for (TaskMeta.TaskAction action : actions) {
      switch (action.action()) {
        case "copyFile" -> {
          File sourceFile = new File(task.parentDir(), action.source());
          File destFile = new File(pom.projectFile().getParentFile(), action.target());
          try {
            if (destFile.exists()) {
              output(DEBUG, "File already exists, skipping %s".formatted(sourceFile.getName()));
            } else {
              File parentDir = destFile.getParentFile();
              if (!parentDir.exists()) {
                output(DEBUG, "Creating directory: %s".formatted(parentDir.getAbsolutePath()));
                if (!parentDir.mkdirs()) {
                  output(ERROR, "Error: Failed to create directory: %s".formatted(parentDir.getAbsolutePath()));
                }
              }
              copy(sourceFile.toPath(), destFile.toPath());
              output(INFO, "Copied file %s".formatted(sourceFile.getName()));
              log.log(TRACE, "Copied file {0} to {1}", sourceFile.getAbsolutePath(), destFile.getAbsolutePath());
            }
          } catch (IOException e) {
            log.log(ERROR, "Error copying file " + sourceFile.getAbsolutePath() + " to " + destFile.getAbsolutePath(), e);
          }
        }
      }
    }
  }

  private void applyPomChanges(File taskDir) {
    File taskPom = new File(taskDir, "pom.xml");
    if (!taskPom.exists()) {
      return;
    }
    // apply pom changes
    MavenTree pomChanges = MavenTree.read(taskPom);

    // reload the working pom from file system
    MavenTree workingPom = MavenTree.read(pom.projectFile());
    addDependencies(pomChanges, workingPom);
    addProperties(pomChanges, workingPom);
    addPlugins(pomChanges, workingPom);
    try {
      workingPom.write(pom.projectFile().toPath());
      if (!addedDependencies.isEmpty()) {
        output(INFO, "Dependencies added %s".formatted(addedDependencies));
      }
      if (!skippedDependencies.isEmpty()) {
        output(INFO, "Dependencies skipped (already exist): %s".formatted(skippedDependencies));
      }
    } catch (IOException e) {
      output.add("Error writing pom.xml changes to " + pom.projectFile().getAbsolutePath() + " " + e.getMessage());
      log.log(ERROR, "Error writing pom.xml changes to " + pom.projectFile().getAbsolutePath(), e);
    }
  }

  private void output(Level info, String msg) {
    output.add(msg);
    log.log(info, msg);
  }

  private void addPlugins(MavenTree pomChanges, MavenTree workingPom) {
    // pomChanges.plugins();
  }

  private void addProperties(MavenTree pomChanges, MavenTree workingPom) {
    List<String> properties = pomChanges.properties();
    if (!properties.isEmpty()) {
      workingPom.addProperties(properties);
    }
  }

  private void addDependencies(MavenTree pomChanges, MavenTree workingPom) {
    Set<String> dependencyKeys = workingPom.dependencyKeys();

    for (MavenDependency mavenDependency : pomChanges.dependencies().toList()) {
      if (dependencyKeys.contains(mavenDependency.key())) {
        skippedDependencies.add(mavenDependency.key());
      } else {
        addedDependencies.add(mavenDependency.key());
        workingPom.addDependency(mavenDependency);
      }
    }
  }
}
