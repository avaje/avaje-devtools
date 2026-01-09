package io.ebean.tools.init;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class MavenPomWriter {

  private final MavenPom pom;
  private final FileWriter writer;

  private String newLine = "\n";

  private final String indent0;
  private final String indent1;
  private final String indent2;
  private final String indent3;
  private final String indent4;
  private final String indent5;

  private List<Dependency> additionalDependencies;
  private List<String> additionalTiles;

  public MavenPomWriter(MavenPom pom, File newPom) throws IOException {
    this.pom = pom;
    this.writer = new FileWriter(newPom);
    this.indent0 = pom.getBaseIndent();
    this.indent1 = indent0 + indent0;
    this.indent2 = indent1 + indent0;
    this.indent3 = indent2 + indent0;
    this.indent4 = indent3 + indent0;
    this.indent5 = indent4 + indent0;
  }

  public void addTiles(List<String> tiles) {
    this.additionalTiles = tiles;
  }

  public void addDependencies(List<Dependency> additionalDependencies) {
    this.additionalDependencies = additionalDependencies;
  }

  private int posTilesStart;
  private int posTilesContinue;
  private boolean addBuildElement;
  private boolean addPluginsElement;
  private boolean addDependenciesElement;

  public void writeToFile() throws IOException {

    final List<String> lines = pom.getLines();


    MavenPom.MavenPlugin tilesPlugin = pom.findTilesPlugin();

    if (tilesPlugin != null) {
      // existing tiles plugin we merge into
      posTilesStart = tilesPlugin.start;
      posTilesContinue = tilesPlugin.end + 1;

    } else {
      final int buildPluginsStart = pom.getBuildPluginsStart();
      tilesPlugin = new MavenPom.MavenPlugin(buildPluginsStart, "io.avaje.maven", "tiles-maven-plugin", "1.2"); //2.15

      if (buildPluginsStart > 1) {
        // insert into <build><plugins>
        posTilesStart = buildPluginsStart;
        posTilesContinue = buildPluginsStart;
      } else {
        addPluginsElement = true;
        // add just prior to </build>
        final int buildEnd = pom.getBuildEnd();
        if (buildEnd == Integer.MAX_VALUE) {
          // add just prior to </project>
          addBuildElement = true;
          posTilesContinue = lines.size() - 1;
          posTilesStart = posTilesContinue;
        } else {
          posTilesStart = buildEnd;
          posTilesContinue = buildEnd;
        }
      }
    }

    int line  = pom.getDependenciesAfterLine();
    if (line == 1) {
      // no <dependencies>
      addDependenciesElement = true;
      line = pom.getBuildStart();
      if (line == 0) {
        // no <build>
        line = posTilesStart;
      }
    }

    for (int i = 0; i < line; i++) {
      writer.write(lines.get(i));
      writer.write(newLine);
    }

    writeDependencies();

    for (int i = line; i < posTilesStart; i++) {
      writer.write(lines.get(i));
      writer.write(newLine);
    }

    writeTilesPlugin(tilesPlugin);

    for (int i = posTilesContinue; i < lines.size(); i++) {
      writer.write(lines.get(i));
      writer.write(newLine);
    }

    writer.close();
  }

  private void writeTilesPlugin(MavenPom.MavenPlugin tilesPlugin) throws IOException {

    if (addBuildElement) {
      writer.append(newLine).append(indent0).append("<build>").append(newLine);
    }
    if (addBuildElement || addPluginsElement) {
      writer.append(indent1).append("<plugins>").append(newLine).append(newLine);
    } else if (tilesPlugin.adding()) {
      writer.append(newLine);
    }

    writer.append(indent2).append("<plugin>").append(newLine);

    writeElement(indent3, "<groupId>", tilesPlugin.getGroupId(), "</groupId>");
    writeElement(indent3, "<artifactId>", tilesPlugin.getArtifactId(), "</artifactId>");
    writeElement(indent3, "<version>", tilesPlugin.getVersion(), "</version>");

    writer.append(indent3).append("<extensions>true</extensions>").append(newLine);
    writer.append(indent3).append("<configuration>").append(newLine);
    writer.append(indent4).append("<tiles>").append(newLine);


    final List<String> existingTiles = tilesPlugin.getTiles();
    if (existingTiles != null) {
      for (String existingTile : existingTiles) {
        writeElement(indent5, "<tile>", existingTile, "</tile>");
      }
    }
    for (String additionalTile : additionalTiles) {
      writeElement(indent5, "<tile>", additionalTile, "</tile>");
    }

    writer.append(indent4).append("</tiles>").append(newLine);
    writer.append(indent3).append("</configuration>").append(newLine);
    writer.append(indent2).append("</plugin>").append(newLine);

    if (addBuildElement || addPluginsElement) {
      writer.append(newLine).append(indent1).append("</plugins>").append(newLine);
    }
    if (addBuildElement) {
      writer.append(indent0).append("</build>").append(newLine).append(newLine);
    }

  }

  private void writeDependencies() throws IOException {
    if (additionalDependencies != null) {

      if (addDependenciesElement) {
        writer.append(newLine).append(indent0).append("<dependencies>").append(newLine);
      }

      for (Dependency dependency : additionalDependencies) {

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
        writer.write(indent1);
        writer.write("</dependency>");
        writer.write(newLine);
      }

      if (addDependenciesElement) {
        writer.append(newLine).append(indent0).append("</dependencies>").append(newLine);
      }
    }
  }

  private void writeElement(String startTag, String value, String endTag) throws IOException {
    writeElement(indent2, startTag, value, endTag);
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
