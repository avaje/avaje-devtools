package io.ebean.tools.init;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class GradleBuildWriterTest {

  @Test
  public void writeToFile() throws IOException {

    File example = new File("src/test/resources/gradle/one.build");
    GradleBuild build = new GradleBuild(example);

    File out = new File("target/one-out.build");
    GradleBuildWriter writer = new GradleBuildWriter(build, out);
    writer.addDependencies(createDependencies());
    writer.addEbeanPlugin("id 'io.ebean' version '11.41.1'");
    writer.writeToFile();

    File compare = new File("src/test/resources/gradle/one-out.build");
    assertThat(out).hasSameContentAs(compare);
  }

  @Test
  public void noPlugins() throws IOException {

    File example = new File("src/test/resources/gradle/gNoPlugins.build");
    GradleBuild build = new GradleBuild(example);

    File out = new File("target/gNoPlugins-out.build");
    GradleBuildWriter writer = new GradleBuildWriter(build, out);
    writer.addDependencies(createDependencies());
    writer.addEbeanPlugin("id 'io.ebean' version '11.41.1'");
    writer.writeToFile();

    File compare = new File("src/test/resources/gradle/gNoPlugins-out.build");
    assertThat(out).hasSameContentAs(compare);
  }

  @Test
  public void noDependencies() throws IOException {

    File example = new File("src/test/resources/gradle/gNoDependencies.build");
    GradleBuild build = new GradleBuild(example);

    File out = new File("target/gNoDependencies-out.build");
    GradleBuildWriter writer = new GradleBuildWriter(build, out);
    writer.addDependencies(createDependencies());
    writer.addEbeanPlugin("id 'io.ebean' version '11.41.1'");
    writer.writeToFile();

    File compare = new File("src/test/resources/gradle/gNoDependencies-out.build");
    assertThat(out).hasSameContentAs(compare);
  }

  private List<Dependency> createDependencies() {
    List<Dependency> add = new ArrayList<>();
    add.add(new Dependency("io.ebean:ebean:11.41.1"));
    add.add(new Dependency("io.ebean:ebean-querybean:11.41.1"));
    add.add(new Dependency("io.ebean:querybean-generator:11.41.1", "Annotation processor").withScope("annotationProcessor"));
    add.add(new Dependency("io.ebean.test:ebean-test-config:11.41.1", "Test dependencies").withScope("testCompile"));
    return add;
  }
}
