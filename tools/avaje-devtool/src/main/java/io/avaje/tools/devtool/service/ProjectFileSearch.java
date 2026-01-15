package io.avaje.tools.devtool.service;

import io.avaje.tools.util.maven.MavenTree;
import io.avaje.tools.util.maven.TreeNode;

import java.io.File;
import java.util.*;
import java.util.stream.Stream;

import static java.lang.System.Logger.Level.DEBUG;

/**
 * Search for (maven) project files starting from the given path.
 */
public final class ProjectFileSearch {

  private static final System.Logger log = System.getLogger("app");

  private static final int maxDepth = 10;
  private final String path;
  private final List<ModelProjectMaven> topMavenProjects = new ArrayList<>();
  private final ArrayDeque<ModelProjectMaven> mavenProjectStack = new ArrayDeque<>();
  private final String rootPath;

  private int depth = 0;
  private int totalDirCount = 0;

  private ProjectFileSearch(String startPath) {
    this.path = normalisePath(startPath);
    this.rootPath = new File(path).getAbsolutePath();
  }

  private static String normalisePath(String path) {
    if (!path.startsWith("~/")) {
      return path;
    }
    return System.getProperty("user.home") + path.substring(1);
  }

  public static ProjectFileSearch matchProjectFiles(String path) {
    return new ProjectFileSearch(path).searchPath();
  }

  private ProjectFileSearch searchPath() {
    File dir = new File(path);
    if (dir.exists() && dir.isDirectory()) {
      searchDirectory(dir);
    }
    return this;
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
    String relativePath = relativePath(projectFile);
    ModelProjectMaven mavenProject = new ModelProjectMaven(mavenTree, relativePath, projectFile, parent);
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

  private String relativePath(File projectFile) {
    String absolutePath = projectFile.getAbsolutePath();
    return absolutePath.substring(rootPath.length() + 1);
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
