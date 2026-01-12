package io.avaje.tools.util.maven;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MavenTreeWriteTest {

  private static final String addBuildPlugin = """
            <plugin>
              <groupId>io.avaje.maven</groupId>
              <artifactId>tiles-maven-plugin</artifactId>
              <version>1.2</version>
              <extensions>true</extensions>
              <configuration>
                <tiles>
                  <tile>io.ebean.tile:enhancement:11.41.1</tile>
                </tiles>
              </configuration>
            </plugin>
      """;

  private static final String addBuildPluginEbean = """
            <plugin> <!-- AOT ebean enhancement -->
              <groupId>io.ebean</groupId>
              <artifactId>ebean-maven-plugin</artifactId>
              <version>12.0.0</version>
              <extensions>true</extensions>
            </plugin>
      """;

  private List<MavenDependency> createDependencies() {
    List<MavenDependency> add = new ArrayList<>();
    add.add(MavenDependency.of("io.ebean:ebean:11.41.1").build());
    add.add(MavenDependency.of("io.ebean:ebean-querybean:11.41.1").build());
    add.add(MavenDependency.of("io.ebean:querybean-generator:11.41.1:provided").build());
    add.add(MavenDependency.of("io.ebean.test:ebean-test-config:11.41.1:test").build());
    return add;
  }

  @Test
  void write() throws IOException {

    File example = new File("src/test/resources/maven/one-pom.xml");
    var pom = MavenTree.read(example);
    pom.addDependencies(createDependencies());

    File out = new File("target/one-pom2.xml");
    pom.write(out.toPath());

    File compare = new File("src/test/resources/maven/one-pom2.xml");
    assertThat(out).hasSameTextualContentAs(compare);
  }

  @Test
  void write_nothing() throws IOException {
    File example = new File("src/test/resources/maven/nothing-pom.xml");
    var pom = MavenTree.read(example);

    pom.addDependencies(createDependencies());
    pom.addBuildPlugin(List.of(addBuildPlugin));

    File out = new File("target/nothing-pom2.xml");
    pom.write(out.toPath());

    File compare = new File("src/test/resources/maven/nothing-pom2.xml");
    assertThat(out).hasSameTextualContentAs(compare);
  }

  @Test
  void write_noDependencies() throws IOException {
    File example = new File("src/test/resources/maven/noDependencies-pom.xml");
    var pom = MavenTree.read(example);
    pom.addDependencies(createDependencies());
    pom.addBuildPlugin(List.of(addBuildPlugin));

    File out = new File("target/noDependencies-pom2.xml");
    pom.write(out.toPath());

    File compare = new File("src/test/resources/maven/noDependencies-pom2.xml");
    assertThat(out).hasSameTextualContentAs(compare);
  }

  @Test
  void write_noDependenciesBP() throws IOException {

    File example = new File("src/test/resources/maven/noDependenciesBP-pom.xml");
    var pom = MavenTree.read(example);
    pom.addDependencies(createDependencies());
    pom.addBuildPlugin(List.of(addBuildPlugin));

    File out = new File("target/noDependenciesBP-pom2.xml");
    pom.write(out.toPath());


    File compare = new File("src/test/resources/maven/noDependenciesBP-pom2.xml");
    assertThat(out).hasSameTextualContentAs(compare);
  }

  @Test
  void write_noBuild() throws IOException {

    File example = new File("src/test/resources/maven/noBuild-pom.xml");
    var pom = MavenTree.read(example);
    pom.addDependencies(createDependencies());
    pom.addBuildPlugin(List.of(addBuildPlugin));

    File out = new File("target/noBuild-pom2.xml");
    pom.write(out.toPath());

    File compare = new File("src/test/resources/maven/noBuild-pom2.xml");
    assertThat(out).hasSameTextualContentAs(compare);
  }

  @Test
  void write_noBuildPlugins() throws IOException {

    File example = new File("src/test/resources/maven/noBuildPlugins-pom.xml");
    var pom = MavenTree.read(example);
    pom.addDependencies(createDependencies());
    pom.addBuildPlugin(List.of(addBuildPlugin));

    File out = new File("target/noBuildPlugins-pom2.xml");
    pom.write(out.toPath());

    File compare = new File("src/test/resources/maven/noBuildPlugins-pom2.xml");
    assertThat(out).hasSameTextualContentAs(compare);
  }

  @Test
  void writeWithPlugin() throws IOException {

    File example = new File("src/test/resources/maven/withEbean-pom.xml");
    var pom = MavenTree.read(example);
    pom.addBuildPlugin(List.of(addBuildPluginEbean));

    File out = new File("target/withEbean-pom2.xml");
    pom.write(out.toPath());

    File compare = new File("src/test/resources/maven/withEbean-pom2.xml");
    assertThat(out).hasSameTextualContentAs(compare);
  }
}
