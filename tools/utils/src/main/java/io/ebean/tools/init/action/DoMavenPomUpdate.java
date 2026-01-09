package io.ebean.tools.init.action;

import io.ebean.tools.init.Dependency;
import io.ebean.tools.init.MavenPom;
import io.ebean.tools.init.MavenPomWriter;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DoMavenPomUpdate {

  private static final String version = "11.41.1";

  private Map<String, String> currentDependencies;

  public void run(MavenPom mavenPom) {

    // DoDatabaseDependency dbDep = new DoDatabaseDependency(help);
    // dbDep.run();

    currentDependencies = getCurrentDependencies();

    final File pomFile = mavenPom.getPomFile();
    try {
      MavenPomWriter writer = new MavenPomWriter(mavenPom, pomFile);

      writer.addDependencies(buildDependencies());
      writer.addTiles(buildTiles());
      writer.writeToFile();

      // help.ackDone("...updated pom.xml adding dependencies and enhancement plugin");

    } catch (IOException e) {
      e.printStackTrace();
      // help.ackErr("Error updating pom.xml - " + e.getMessage());
    }
  }

  private List<String> buildTiles() {
    List<String> tiles = new ArrayList<>();
    tiles.add(dependency("ebean-tile","io.ebean.tile:enhancement:"+version));
    return tiles;
  }

  private List<Dependency> buildDependencies() {

    List<Dependency> add = new ArrayList<>();

//    final String databaseDependency = help.detection().getDatabaseDependency();
//    if (databaseDependency != null) {
//      add.add(new Dependency(databaseDependency));
//    }
    add.add(dep("ebean", "io.ebean:ebean:" + version));
    add.add(dep("ebean-querybean", "io.ebean:ebean-querybean:" + version));
//    if (!help.detection().isSourceModeKotlin()) {
//      add.add(dep("querybean-generator","io.ebean:querybean-generator:" + version, "Annotation processor").withScope("provided"));
//    }
    add.add(dep("ebean-test","io.ebean.test:ebean-test-config:" + version, "Test dependencies").withScope("test"));
    return add;
  }

  private Dependency dep(String key, String fallback) {
    return dep(key, fallback, null);
  }

  private Dependency dep(String key, String fallback, String comment) {
    return new Dependency(dependency(key, fallback), comment);
  }

  private String dependency(String key, String fallback) {
    final String val = dependency(key);
    return (val != null)? val: fallback;
  }
  private String dependency(String key) {
    return (currentDependencies == null) ? null : currentDependencies.get(key);
  }

  private Map<String, String> getCurrentDependencies() {
    try {
      final HttpGet.Response response = HttpGet.get("dependencies.properties");
      if (response.ok()) {
        return HttpGet.asMap(response);
      }
      return null;
    } catch (Exception e) {
      // help.error("Error getting current dependencies from https://ebean.io - will use a recent version but not latest. Err: " + e.getMessage());
      return null;
    }
  }
}
