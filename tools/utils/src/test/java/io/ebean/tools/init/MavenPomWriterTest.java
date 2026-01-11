package io.ebean.tools.init;



import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class MavenPomWriterTest {

  private List<Dependency> createDependencies() {
    List<Dependency> add = new ArrayList<>();
    add.add(new Dependency("io.ebean:ebean:11.41.1"));
    add.add(new Dependency("io.ebean:ebean-querybean:11.41.1"));
    add.add(new Dependency("io.ebean:querybean-generator:11.41.1:provided", "Annotation processor"));
    add.add(new Dependency("io.ebean.test:ebean-test-config:11.41.1:test", "Test dependencies"));
    return add;
  }

  @Test
  public void write() throws IOException {

    File example = new File("src/test/resources/maven/one-pom.xml");
    MavenPom pom = new MavenPom(example);

    File out = new File("target/one-pom2.xml");
    MavenPomWriter writer = new MavenPomWriter(pom, out);
    writer.addDependencies(createDependencies());
    writer.writeToFile();

    File compare = new File("src/test/resources/maven/one-pom2.xml");
    assertThat(out).hasSameContentAs(compare);
  }

  @Test
  public void write_nothing() throws IOException {

    File example = new File("src/test/resources/maven/nothing-pom.xml");
    MavenPom pom = new MavenPom(example);

    File out = new File("target/nothing-pom2.xml");
    MavenPomWriter writer = new MavenPomWriter(pom, out);
    writer.addDependencies(createDependencies());
    writer.writeToFile();

    File compare = new File("src/test/resources/maven/nothing-pom2.xml");
    assertThat(out).hasSameContentAs(compare);
  }

  @Test
  public void write_noDependencies() throws IOException {

    File example = new File("src/test/resources/maven/noDependencies-pom.xml");
    MavenPom pom = new MavenPom(example);

    File out = new File("target/noDependencies-pom2.xml");
    MavenPomWriter writer = new MavenPomWriter(pom, out);
    writer.addDependencies(createDependencies());
    writer.writeToFile();

    File compare = new File("src/test/resources/maven/noDependencies-pom2.xml");
    assertThat(out).hasSameContentAs(compare);
  }

  @Test
  public void write_noDependenciesBP() throws IOException {

    File example = new File("src/test/resources/maven/noDependenciesBP-pom.xml");
    MavenPom pom = new MavenPom(example);

    File out = new File("target/noDependenciesBP-pom2.xml");
    MavenPomWriter writer = new MavenPomWriter(pom, out);
    writer.addDependencies(createDependencies());
    writer.writeToFile();

    File compare = new File("src/test/resources/maven/noDependenciesBP-pom2.xml");
    assertThat(out).hasSameContentAs(compare);
  }

  @Test
  public void write_noBuild() throws IOException {

    String pl = """
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


    File example = new File("src/test/resources/maven/noBuild-pom.xml");
    MavenPom pom = new MavenPom(example);

    File out = new File("target/noBuild-pom2.xml");
    MavenPomWriter writer = new MavenPomWriter(pom, out);
    writer.addDependencies(createDependencies());
    writer.addBuildPlugin(List.of(pl));
    writer.writeToFile();

    File compare = new File("src/test/resources/maven/noBuild-pom2.xml");
    assertThat(out).hasSameTextualContentAs(compare);
  }

  @Test
  public void write_noBuildPlugins() throws IOException {

    File example = new File("src/test/resources/maven/noBuildPlugins-pom.xml");
    MavenPom pom = new MavenPom(example);

    File out = new File("target/noBuildPlugins-pom2.xml");
    MavenPomWriter writer = new MavenPomWriter(pom, out);
    writer.addDependencies(createDependencies());
    writer.writeToFile();

    File compare = new File("src/test/resources/maven/noBuildPlugins-pom2.xml");
    assertThat(out).hasSameContentAs(compare);
  }

  @Test
  public void writeWithPlugin() throws IOException {

    File example = new File("src/test/resources/maven/withEbean-pom.xml");
    MavenPom pom = new MavenPom(example);

    File out = new File("target/withEbean-pom2.xml");
    MavenPomWriter writer = new MavenPomWriter(pom, out);
    writer.writeToFile();

    File compare = new File("src/test/resources/maven/withEbean-pom2.xml");
    assertThat(out).hasSameContentAs(compare);
  }
}
