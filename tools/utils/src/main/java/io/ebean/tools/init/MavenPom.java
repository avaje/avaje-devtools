package io.ebean.tools.init;

import java.io.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MavenPom {


  public enum Section {
    PROFILES,
    DEP_MGMT,
    DEP,
    BUILD
  }
  private final File pom;

  private final List<String> lines;

  private final List<Section> sectionOrdering = new ArrayList<>();

  private final List<MavenDependency> dependencies = new ArrayList<>();
  private final Set<String> dependencyKeyset = new HashSet<>();
  private final List<MavenPlugin> plugins = new ArrayList<>();

  private int profilesStart;
  private int profilesEnd = Integer.MAX_VALUE;


  private int dependencyManagementStart;
  private int dependencyManagementEnd = Integer.MAX_VALUE;

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
//  private String dependencyType;
//  private String dependencyOptional;
//  private String dependencyClassifier;

  private String baseIndent = "  ";

  MavenPom(File pom) {
    this.pom = pom;
    this.lines = readLines();
    parseLines();
  }

  String getBaseIndent() {
    return baseIndent;
  }

  private void setBaseIndent(String line) {
    final int pos = line.indexOf('<');
    baseIndent = line.substring(0, pos);
  }

  List<String> getLines() {
    return lines;
  }

  public List<Section> sectionOrdering() {
    return sectionOrdering;
  }

  public boolean sectionExists(Section section) {
    return sectionOrdering.contains(section);
  }


  int injectDependenciesAfterLine() {
    final MavenDependency nonTestDependency = findLastNonTestDependency();
    if (nonTestDependency != null) {
      return nonTestDependency.end + 1;
    }
    return dependenciesEnd - 1; // dependenciesStart + 1;
  }

  int getBuildPluginsStart() {
    return buildPluginsStart + 1;
  }
  int getBuildPluginsEnd() {
    return buildPluginsEnd;
  }

  int getProfilesEnd() {
    return profilesEnd;
  }

  int getDependencyManagementEnd() {
    return dependencyManagementEnd;
  }

  int getDependencyManagementStart() {
    return dependencyManagementStart;
  }

  public int getDependenciesStart() {
    return dependenciesStart;
  }

  public int getDependenciesEnd() {
    return dependenciesEnd;
  }

  public int getProfilesStart() {
    return profilesStart;
  }

  public boolean addProfilesTag() {
    return profilesStart <= 0;
  }

  public boolean addDepMgmtTag() {
    return dependencyManagementStart <= 0;
  }

  public boolean hasBuildPluginsTag() {
    return buildPluginsStart > 0;
  }

  public boolean addBuildTag() {
    return buildPluginsStart <= 0;
  }

  public boolean addDependenciesTag() {
    return dependenciesStart <= 0;
  }


  boolean hasDependencyEbean() {
    return hasDependency("io.ebean", "ebean");
  }

  boolean hasDependency(String groupId, String artifactId) {
    return dependencyKeyset.contains(groupId + ':' + artifactId);
  }

  /**
   * Return the last non-test dependency. New dependencies would be added after this.
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
    if (profilesStart == 0 && line.contains("<profiles>")) {
      sectionOrdering.add(Section.PROFILES);
      profilesStart = lineIndex;
      return;
    }
    if (profilesStart != 0 && line.contains("</profiles>")) {
      profilesEnd = lineIndex;
      return;
    }
    if (dependencyManagementStart == 0 && line.contains("<dependencyManagement>")) {
      sectionOrdering.add(Section.DEP_MGMT);
      dependencyManagementStart = lineIndex;
      return;
    }
    if (dependencyManagementStart != 0 && line.contains("</dependencyManagement>")) {
      dependencyManagementEnd = lineIndex;
      return;
    }
    if (dependenciesStart == 0 && line.contains("<dependencies>")) {
      sectionOrdering.add(Section.DEP);
      dependenciesStart = lineIndex;
      setBaseIndent(line);
      return;
    }
    if (dependenciesStart != 0 && line.contains("</dependencies>")) {
      dependenciesEnd = lineIndex;
      return;
    }
    if (buildStart == 0 && line.contains("<build>")) {
      sectionOrdering.add(Section.BUILD);
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
    plugins.add(new MavenPlugin(pluginStart, endLineIndex, pluginGroupId, pluginArtifactId, pluginVersion));

    pluginStart = 0;
    pluginGroupId = null;
    pluginArtifactId = null;
    pluginVersion = null;
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
//    } else if (line.contains("<type>")) {
//      dependencyType = extractFromLine("type", line);
//    } else if (line.contains("<optional>")) {
//      dependencyOptional = extractFromLine("optional", line);
//    } else if (line.contains("<classifier>")) {
//      dependencyClassifier = extractFromLine("classifier", line);
    } else if (line.contains("</dependency>")) {
      addDependency(lineIndex);
    }
  }

  private void addDependency(int endLineIndex) {
    var dep = new MavenDependency(dependencyStart, endLineIndex, dependencyGroupId, dependencyArtifactId, dependencyScope);
    dependencies.add(dep);
    dependencyKeyset.add(dep.key());
    dependencyStart = 0;
    dependencyGroupId = null;
    dependencyArtifactId = null;
    dependencyScope = null;
//    dependencyType = null;
//    dependencyOptional = null;
//    dependencyClassifier = null;
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
      throw new UncheckedIOException(e);
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

    final int start;
    final int end;
    final String groupId;
    final String artifactId;
    final String version;
    List<String> tiles;

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

    public String key() {
      return groupId + ':' + artifactId;
    }
  }

}
