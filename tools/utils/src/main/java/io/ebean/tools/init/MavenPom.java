package io.ebean.tools.init;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.util.ArrayList;
import java.util.List;

public class MavenPom {

  private final File pom;

  private List<String> lines;

  private List<MavenDependency> dependencies = new ArrayList<>();
  private List<MavenPlugin> plugins = new ArrayList<>();

  private MavenPlugin tilesPlugin;

  private int dependenciesStart;
  private int dependenciesEnd = Integer.MAX_VALUE;
  private int dependencyStart;
  private int pluginStart;

  private int buildStart;
  private int buildPluginsStart;
  private int buildPluginsEnd = Integer.MAX_VALUE;
  private int buildEnd = Integer.MAX_VALUE;

  private String pluginGroupId;
  private String pluginArtifactId;
  private String pluginVersion;

  private boolean currentlyExcludingDependencies;
  private String dependencyGroupId;
  private String dependencyArtifactId;
  private String dependencyScope;

  private String baseIndent = "  ";

  MavenPom(File pom) {
    this.pom = pom;
    this.lines = readLines();
    parseLines();
  }

  boolean isNewProject() {
    return (tilesPlugin == null || !tilesPlugin.contains("io.ebean.tile:enhancement:")) && !hasDependencyEbean();
  }

  String getBaseIndent() {
    return baseIndent;
  }

  List<String> getLines() {
    return lines;
  }

  int getDependenciesAfterLine() {

    final MavenDependency nonTestDependency = findLastNonTestDependency();
    if (nonTestDependency == null) {
      return dependenciesStart + 1;
    }
    return nonTestDependency.end + 1;
  }

  int getBuildPluginsStart() {
    return buildPluginsStart + 1;
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
    for (MavenDependency dependency : dependencies) {
      if (groupId.equals(dependency.groupId) && artifactId.equals(dependency.artifactId)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Return the last non test dependency. New dependencies would be added after this.
   */
  MavenPlugin findTilesPlugin() {
    return tilesPlugin;
  }

  /**
   * Return the last non test dependency. New dependencies would be added after this.
   */
  MavenDependency findLastNonTestDependency() {

    MavenDependency lastNonTestDependency = null;

    for (final MavenDependency dependency : dependencies) {
      if ("test".equalsIgnoreCase(dependency.scope)) {
        return lastNonTestDependency;
      } else {
        lastNonTestDependency = dependency;
      }
    }
    return lastNonTestDependency;
  }

  private void parseLines() {
    if (lines != null) {
      for (int i = 0; i < lines.size(); i++) {
        parseLine(lines.get(i), i);
      }
    }
  }


  private void parseLine(String line, int lineIndex) {

    if (dependenciesStart == 0 && line.contains("<dependencies>")) {
      dependenciesStart = lineIndex;
      setBaseIndent(line);
      return;
    }
    if (dependenciesStart != 0 && line.contains("</dependencies>")) {
      dependenciesEnd = lineIndex;
      return;
    }
    if (buildStart == 0 && line.contains("<build>")) {
      buildStart = lineIndex;
      setBaseIndent(line);
      return;
    }
    if (buildStart != 0 && line.contains("</build>")) {
      buildEnd = lineIndex;
      return;
    }
    if (buildStart != 0 && buildPluginsStart == 0 && line.contains("<plugins>")) {
      buildPluginsStart = lineIndex;
      return;
    }
    if (buildStart != 0 && buildPluginsStart != 0 && line.contains("</plugins>")) {
      buildPluginsEnd = lineIndex;
      return;
    }
    if (inDependenciesSection(lineIndex)) {
      parseDependencies(line, lineIndex);
    } else if (inBuildPlugins(lineIndex)) {
      parseBuildPlugins(line, lineIndex);
    }
  }

  private void setBaseIndent(String line) {
    final int pos = line.indexOf('<');
    baseIndent = line.substring(0, pos);
  }

  private boolean inBuildPlugins(int lineIndex) {
    return buildPluginsStart < lineIndex && lineIndex < buildPluginsEnd;
  }

  private boolean inDependenciesSection(int lineIndex) {
    return dependenciesStart < lineIndex && lineIndex < dependenciesEnd;
  }

  private void parseBuildPlugins(String line, int lineIndex) {
    if (pluginStart == 0 && line.contains("<plugin>")) {
      pluginStart = lineIndex;
    } else if (line.contains("<groupId>") && pluginGroupId == null) {
      pluginGroupId = extractFromLine("groupId", line);
    } else if (line.contains("<artifactId>") && pluginArtifactId == null) {
      pluginArtifactId = extractFromLine("artifactId", line);
    } else if (line.contains("<version>") && pluginVersion == null) {
      pluginVersion = extractFromLine("version", line);

    } else if (line.contains("</plugin>")) {
      addPlugin(lineIndex);
    }
  }

  private void addPlugin(int endLineIndex) {

    MavenPlugin plugin = new MavenPlugin(pluginStart, endLineIndex, pluginGroupId, pluginArtifactId, pluginVersion);
    plugins.add(plugin);

    if (pluginArtifactId.equalsIgnoreCase("tiles-maven-plugin")) {
      tilesPlugin = plugin;
      plugin.setTiles(readTiles(endLineIndex));
    }

    pluginStart = 0;
    pluginGroupId = null;
    pluginArtifactId = null;
    pluginVersion = null;
  }

  private List<String> readTiles(int endLineIndex) {
    List<String> tiles = new ArrayList<>();
    for (int i = pluginStart + 1; i < endLineIndex; i++) {
      final String line = lines.get(i);
      if (line.contains("<tile>")) {
        tiles.add(extractFromLine("tile", line));
      }
    }
    return tiles;
  }

  private void parseDependencies(String line, int lineIndex) {

    if (line.contains("<exclusions>")) {
      currentlyExcludingDependencies = true;
    } else if (line.contains("</exclusions>")) {
      currentlyExcludingDependencies = false;
    }
    if (currentlyExcludingDependencies) {
      return;
    }
    if (dependencyStart == 0 && line.contains("<dependency>")) {
      dependencyStart = lineIndex;
    } else if (line.contains("<groupId>")) {
      extractGroupId(line);
    } else if (line.contains("<artifactId>")) {
      extractArtifactId(line);
    } else if (line.contains("<scope>")) {
      extractScope(line);
    } else if (line.contains("</dependency>")) {
      addDependency(lineIndex);
    }
  }

  private void addDependency(int endLineIndex) {
    dependencies.add(new MavenDependency(dependencyStart, endLineIndex, dependencyGroupId, dependencyArtifactId, dependencyScope));
    dependencyStart = 0;
    dependencyGroupId = null;
    dependencyArtifactId = null;
    dependencyScope = null;
  }

  private void extractScope(String line) {
    dependencyScope = extractFromLine("scope", line);
  }

  private void extractArtifactId(String line) {
    dependencyArtifactId = extractFromLine("artifactId", line);
  }

  private void extractGroupId(String line) {
    dependencyGroupId = extractFromLine("groupId", line);
  }

  private String extractFromLine(String tag, String line) {
    int start = line.indexOf("<" + tag + ">");
    int end = line.indexOf("</" + tag + ">", start);

    return line.substring(start + tag.length() + 2, end).trim();
  }


  private List<String> readLines() {

    try {
      List<String> lines = new ArrayList<>();
      try (LineNumberReader reader = new LineNumberReader(new FileReader(pom))) {
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

  public File getPomFile() {
    return pom;
  }

  public int getBuildEnd() {
    return buildEnd;
  }

  public int getBuildStart() {
    return buildStart;
  }

  static class MavenPlugin {

    List<String> tiles;
    final int start;
    final int end;
    final String groupId;
    final String artifactId;
    final String version;

    private MavenPlugin(int start, int end, String groupId, String artifactId, String version) {
      this.start = start;
      this.end = end;
      this.groupId = groupId;
      this.artifactId = artifactId;
      this.version = version;
    }

    MavenPlugin(int start, String groupId, String artifactId, String version) {
      this(start, start, groupId, artifactId, version);
    }

    boolean adding() {
      return start == end;
    }

    String getGroupId() {
      return groupId;
    }

    String getArtifactId() {
      return artifactId;
    }

    String getVersion() {
      return version;
    }

    List<String> getTiles() {
      return tiles;
    }

    void setTiles(List<String> tiles) {
      this.tiles = tiles;
    }

    boolean contains(String tileId) {
      if (tiles != null) {
        for (String tile : tiles) {
          if (tile.startsWith(tileId)) {
            return true;
          }
        }
      }
      return false;
    }
  }

  static class MavenDependency {

    final int start;
    final int end;
    final String groupId;
    final String artifactId;
    final String scope;

    private MavenDependency(int start, int end, String groupId, String artifactId, String scope) {
      this.start = start;
      this.end = end;
      this.groupId = groupId;
      this.artifactId = artifactId;
      this.scope = scope;
    }
  }

}
