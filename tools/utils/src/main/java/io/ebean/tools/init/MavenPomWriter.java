package io.ebean.tools.init;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MavenPomWriter {

  private final MavenPom pom;
  private final FileWriter writer;
  private final String newLine = "\n";

  private final String indent0;
  private final String indent1;
  private final String indent2;
  private final String indent3;
  private final String indent4;
  private final String indent5;

  private List<Dependency> additionalDepMgmt = List.of();
  private List<Dependency> additionalDependencies = List.of();
  private List<String> additionalProfiles = List.of();
  private List<String> additionalBuildPlugins = List.of();

  private final List<String> sourceLines;
  private int sourceLinesWritten = 0;

  public MavenPomWriter(MavenPom pom, File newPom) throws IOException {
    this.pom = pom;
    this.sourceLines = pom.getLines();

    this.writer = new FileWriter(newPom);
    this.indent0 = pom.getBaseIndent();
    this.indent1 = indent0 + indent0;
    this.indent2 = indent1 + indent0;
    this.indent3 = indent2 + indent0;
    this.indent4 = indent3 + indent0;
    this.indent5 = indent4 + indent0;
  }

  public void addBuildPlugin(List<String> pl) {
    this.additionalBuildPlugins = pl;
  }

  public void addDepManagement(List<Dependency> additionalDepMgmt) {
    this.additionalDepMgmt = additionalDepMgmt;
  }

  public void addDependencies(List<Dependency> additionalDependencies) {
    this.additionalDependencies = additionalDependencies;
  }

  public void writeToFile() throws IOException {
    List<MavenPom.Section> sectionsCopy = new ArrayList<>(pom.sectionOrdering());

    if (!additionalDepMgmt.isEmpty() && !pom.sectionExists(MavenPom.Section.DEP_MGMT)) {
      writeDepMgmt();
      sectionsCopy.remove(MavenPom.Section.DEP_MGMT);
    }

    if (!additionalDependencies.isEmpty() && !pom.sectionExists(MavenPom.Section.DEP)) {
      writeDependencies();
      sectionsCopy.remove(MavenPom.Section.DEP);
    }

    // check for new build and profiles sections, we desire those at the end of the pom
    boolean addNewBuildSection = !additionalBuildPlugins.isEmpty() && !pom.sectionExists(MavenPom.Section.BUILD);
    boolean addNewProfilesSection = !additionalProfiles.isEmpty() && !pom.sectionExists(MavenPom.Section.PROFILES);

    // update pom sections that have additions
    for (MavenPom.Section nextSection : sectionsCopy) {
      if (hasAdditionsTo(nextSection)) {
        writeSection(nextSection);
      }
    }
    if (addNewBuildSection) {
      writeBuildPlugins();
    }
    if (addNewProfilesSection) {
      writeProfiles();
    }

    writeSourceLines(sourceLines.size());
    writer.close();
  }

  private void writeSection(MavenPom.Section nextSection) throws IOException {
    if (nextSection == MavenPom.Section.DEP) {
      writeDependencies();
    } else if (nextSection == MavenPom.Section.DEP_MGMT) {
      writeDepMgmt();
    } else if (nextSection == MavenPom.Section.BUILD) {
      writeBuildPlugins();
    } else if (nextSection == MavenPom.Section.PROFILES) {
      writeProfiles();
    } else {
      throw new RuntimeException("Not yet supported");
    }
  }

  private void writeSourceLines(int line) throws IOException {
    for (int i = sourceLinesWritten; i < line; i++) {
      writer.write(sourceLines.get(i));
      writer.write(newLine);
    }
    sourceLinesWritten = line;
  }

  private boolean hasAdditionsTo(MavenPom.Section nextSection) {
    return switch (nextSection) {
      case PROFILES -> !additionalProfiles.isEmpty();
      case DEP_MGMT -> !additionalDepMgmt.isEmpty();
      case DEP -> !additionalDependencies.isEmpty();
      case BUILD -> !additionalBuildPlugins.isEmpty();
    };
  }

  private void writeDepMgmt() throws IOException {
    boolean addTag = pom.addDepMgmtTag();
    if (addTag) {
      writer.append(newLine);
      writer.append(newLine).append(indent0).append("<dependencyManagement>").append(newLine);
    }

    int end = pom.getDependencyManagementEnd();
    if (end < Integer.MAX_VALUE) {
      writeSourceLines(end);
    }
    for (Dependency dependency : additionalDepMgmt) {
      writeDependency(dependency);
    }
    if (addTag) {
      writer.append(newLine).append(indent0).append("</dependencyManagement>").append(newLine);
    }
  }

  private void writeProfiles() throws IOException {
    boolean addProfilesTag = pom.addProfilesTag();
    if (addProfilesTag) {
      writer.append(newLine);
      writer.append(newLine).append(indent0).append("<profiles>").append(newLine);
    }

    int profilesEnd = pom.getProfilesEnd();
    if (profilesEnd < Integer.MAX_VALUE) {
      writeSourceLines(profilesEnd);
    }
    for (String profile : additionalProfiles) {
      writeProfile(profile);
    }

    if (addProfilesTag) {
      writer.append(newLine).append(indent0).append("</profiles>").append(newLine);
    }
  }

  private void writeBuildPlugins() throws IOException {
    boolean addBuildTag = pom.addBuildTag();
    boolean addPluginsTag = !pom.hasBuildPluginsTag();
    if (addBuildTag) {
      writer.append(newLine).append(indent0).append("<build>").append(newLine);
    }
    if (addPluginsTag) {
      if (!addBuildTag) {
        writer.append(newLine);
      }
      writer.append(indent1).append("<plugins>").append(newLine);
    }
    int buildPluginsEnd = pom.getBuildPluginsEnd();
    if (buildPluginsEnd < Integer.MAX_VALUE) {
      writeSourceLines(buildPluginsEnd);
    }
    for (String buildPlugin : additionalBuildPlugins) {
      writeBuildPlugin(buildPlugin);
    }
    if (addPluginsTag) {
      writer.append(newLine).append(newLine).append(indent1).append("</plugins>").append(newLine);
    }
    if (addBuildTag) {
      if (!addPluginsTag) {
        writer.append(newLine);
      }
      writer.append(indent0).append("</build>").append(newLine);
    }
  }

  private void writeProfile(String profile) throws IOException {
    List<String> asLines = profile.lines().toList();
    String first = asLines.getFirst();
    int indent = first.indexOf("<profile>");
    if (indent == -1) {
      throw new IllegalStateException("Missing <profile> tag from: " + profile);
    }
    for (String asLine : asLines) {
      writer.append(newLine).append(indent1).append(asLine.substring(indent));
    }
  }

  private void writeBuildPlugin(String buildPlugin) throws IOException {
    List<String> asLines = buildPlugin.lines().toList();
    String first = asLines.getFirst();
    int indent = first.indexOf("<plugin>");
    if (indent == -1) {
      throw new IllegalStateException("Missing <plugin> tag from: " + buildPlugin);
    }
    for (String asLine : asLines) {
      writer.append(newLine).append(indent2).append(asLine.substring(indent));
    }
  }

  private void writeDependencies() throws IOException {
    boolean addDependenciesTag = pom.addDependenciesTag();
    if (addDependenciesTag) {
      writer.append(newLine).append(indent0).append("<dependencies>").append(newLine);
    } else {
      int line = pom.injectDependenciesAfterLine();
      if (line < Integer.MAX_VALUE) {
        writeSourceLines(line);
      }
    }
    for (Dependency dependency : additionalDependencies) {
      writeDependency(dependency);
    }
    if (addDependenciesTag) {
      writer.append(newLine).append(indent0).append("</dependencies>").append(newLine);
    } else {
      writeSourceLines(pom.getDependenciesEnd() + 1);
    }
  }

  private void writeDependency(Dependency dependency) throws IOException {
    final String comment = dependency.getComment();
    if (comment != null) {
      writer.write(newLine);
      writer.write(indent1);
      writer.write("<!-- ");
      writer.write(comment);
      writer.write("-->");
      writer.write(newLine);
    }
    writer.write(newLine);
    writer.write(indent1);
    writer.write("<dependency>");
    writer.write(newLine);
    writeElement("<groupId>", dependency.getGroupId(), "</groupId>");
    writeElement("<artifactId>", dependency.getArtifactId(), "</artifactId>");
    writeElement("<version>", dependency.getVersion(), "</version>");
    writeElement("<scope>", dependency.getScope(), "</scope>");
    writeElement("<optional>", dependency.optional(), "</optional>");
    writeElement("<type>", dependency.type(), "</type>");
    writeElement("<classifier>", dependency.classifier(), "</classifier>");
    writer.write(indent1);
    writer.write("</dependency>");
    writer.write(newLine);
  }

  private void writeElement(String startTag, String value, String endTag) throws IOException {
    if (value != null && !value.isBlank()) {
      writeElement(indent2, startTag, value, endTag);
    }
  }

  private void writeElement(String indent, String startTag, String value, String endTag) throws IOException {
    if (value != null) {
      writer.write(indent);
      writer.write(startTag);
      writer.write(value);
      writer.write(endTag);
      writer.write(newLine);
    }
  }
}
