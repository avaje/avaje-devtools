package io.avaje.tools.devtool.service;

import io.avaje.tools.util.maven.MavenTree;
import io.avaje.tools.util.maven.TreeNode;

import java.io.File;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static java.lang.System.Logger.Level.DEBUG;

/**
 * Search for (maven) project files starting from the given path.
 */
public class ProjectFileSearch {

  private static final System.Logger log = System.getLogger("app");

  private static final int maxDepth = 10;
  private final String path;
  private final List<ModelProjectMaven> topMavenProjects = new ArrayList<>();
  private final ArrayDeque<ModelProjectMaven> mavenProjectStack = new ArrayDeque<>();

  private int depth = 0;
  private int totalDirCount = 0;

  public ProjectFileSearch(String path) {
    this.path = path;
  }

  public static ProjectFileSearch matchProjectFiles(String path) {
    var search = new ProjectFileSearch(path);
    search.searchPath(path);
    return search;
  }

  private void searchPath(String directory) {
    if (directory.startsWith("~/")) {
      directory = System.getProperty("user.home") + directory.substring(1);
    }
    File dir = new File(directory);
    if (dir.exists() && dir.isDirectory()) {
      searchDirectory(dir);
    }
  }

  private void searchDirectory(File dir) {
    if (depth >= maxDepth) {
      log.log(DEBUG, "max directory scan depth at {0}", dir.getAbsolutePath());
      return; // max depth reached
    }
    File[] files = dir.listFiles();
    if (files == null) {
      return; // empty directory
    }
    totalDirCount++;
    File projectFile = searchDirectoryForProject(files);
    if (projectFile == null) {
      // continue searching subdirectories
      searchSubDirectories(files);
    } else {
      if (isMavenPom(projectFile)) {
        processMavenPom(projectFile, files);
      }
    }
  }

  private void processMavenPom(File projectFile, File[] files) {
    boolean topLevelPom = mavenProjectStack.isEmpty();

    ModelProjectMaven parent = null;
    if (!topLevelPom) {
      parent = mavenProjectStack.peek();
    }

    MavenTree mavenTree = MavenTree.read(projectFile);
    ModelProjectMaven mavenProject = new ModelProjectMaven(mavenTree, projectFile, parent);
    if (topLevelPom) {
      topMavenProjects.add(mavenProject);
    } else {
      // add as module to parent
      parent.addModule(mavenProject);
    }
    List<TreeNode> modules = mavenTree.find("modules");
    if (!modules.isEmpty()) {
      mavenProjectStack.push(mavenProject);
      // need to recurse into module poms
      searchSubDirectories(files);
      mavenProjectStack.pop();
    }
  }

  private void searchSubDirectories(File[] files) {
    for (File child : files) {
      if (child.isDirectory()) {
        if (child.getName().startsWith(".")) {
          // skipping hidden directory
          continue;
        }
        boolean insideMaven = !mavenProjectStack.isEmpty();
        if (!insideMaven || !ignoreInsideMaven(child)) {
          depth++;
          searchDirectory(child);
          depth--;
        }
      }
    }
  }

  private static boolean ignoreInsideMaven(File child) {
    return child.getName().equals("src")
      || child.getName().equals("target")
      || child.getName().equals("tmp")
      || child.getName().equals("build");
  }

  private File searchDirectoryForProject(File[] files) {
    for (File file : files) {
      if (isMavenPom(file)) {
        return file;
      }
    }
    return null;
  }

  private static boolean isMavenPom(File file) {
    return file.getName().endsWith("pom.xml");
  }

  public List<ModelProjectMaven> topLevel() {
    return topMavenProjects;
  }

  public Stream<ModelProjectMaven> all() {
    return topMavenProjects.stream()
      .flatMap(ModelProjectMaven::flattened);
  }

  public int totalDirectoriesSearched() {
    return totalDirCount;
  }

  public String path() {
    return path;
  }
}
