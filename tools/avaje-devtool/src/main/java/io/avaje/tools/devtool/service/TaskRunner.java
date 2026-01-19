package io.avaje.tools.devtool.service;

import io.avaje.tools.devtool.data.TaskMeta;
import io.avaje.tools.devtool.state.Task;
import io.avaje.tools.util.maven.MavenDependency;
import io.avaje.tools.util.maven.MavenTree;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static java.lang.System.Logger.Level.*;
import static java.nio.file.Files.*;

public class TaskRunner {

  private static final System.Logger log = System.getLogger("app");

  private final Set<String> addedDependencies = new LinkedHashSet<>();
  private final Set<String> skippedDependencies = new LinkedHashSet<>();

  private final Task task;
  private final ModelProjectMaven pom;

  private TaskRunner(Task task, ModelProjectMaven pom) {
    this.task = task;
    this.pom = pom;
  }

  public static void run(Task task, ModelProjectMaven pom) {
    new TaskRunner(task, pom).run();
  }

  public void run() {
    File taskDir = task.parentDir();
    applyPomChanges(taskDir);
    applyActions();
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
              log.log(DEBUG, "File already exists, skipping {0}", action.source());
            } else {
              File parentDir = destFile.getParentFile();
              if (!parentDir.exists()) {
                log.log(DEBUG, "Creating directory: {0}", parentDir.getAbsolutePath());
                if (!parentDir.mkdirs()) {
                  log.log(ERROR, "Failed to create directory: {0}", parentDir.getAbsolutePath());
                }
              }
              copy(sourceFile.toPath(), destFile.toPath());
              log.log(INFO, "Copied file {0} to {1}", sourceFile.getAbsolutePath(), destFile.getAbsolutePath());
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
      log.log(INFO, "Dependencies added {0} - {1}", addedDependencies.size(), addedDependencies);
      if (!skippedDependencies.isEmpty()) {
        log.log(INFO, "Dependencies skipped (already exist): {0} - {1}", skippedDependencies.size(), skippedDependencies);
      }
    } catch (IOException e) {
      log.log(ERROR, "Error writing pom.xml changes to " + pom.projectFile().getAbsolutePath(), e);
    }
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
