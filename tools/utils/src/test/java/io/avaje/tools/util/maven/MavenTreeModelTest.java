package io.avaje.tools.util.maven;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MavenTreeModelTest {

  @Test
  void groupIdFromParent() {
    File example = new File("src/test/resources/maven/nothing-pom3.xml");
    MavenTree pom = TreeReader.read(example);

    assertThat(pom.groupId()).isEqualTo("io.ebean.parent");
    assertThat(pom.artifactId()).isEqualTo("ebean-init");
    assertThat(pom.name()).isEqualTo("my-name");
    assertThat(pom.description()).isEqualTo("my-description");
  }

  @Test
  void pomContainsNothing() {
    File example = new File("src/test/resources/maven/nothing-pom.xml");
    MavenTree pom = TreeReader.read(example);

    var artifactId = pom.find("artifactId");
    assertThat(artifactId).hasSize(1);

    assertThat(pom.groupId()).isEqualTo("io.ebean.tools");
    assertThat(pom.artifactId()).isEqualTo("ebean-init");
  }

  @Test
  void pomContains() throws IOException {
    File example = new File("src/test/resources/maven/one-pom.xml");
    var pom = TreeReader.read(example);

    assertThat(pom.find("artifactId")).hasSize(1);
    assertThat(pom.find("dependencies.dependency")).hasSize(5);

    assertThat(pom.containsDependency("org.slf4j", "slf4j-api")).isTrue();
    assertThat(pom.containsDependency("org.fusesource.jansi", "jansi")).isTrue();

    File out = new File("target/one-pom-noChange.xml");
    pom.write(out.toPath());
  }

  @Test
  void noBuild2_add() throws IOException {
    File example = new File("src/test/resources/maven/noBuild-pom2.xml");
    var pom = TreeReader.read(example);
    assertThat(pom.find("build.plugins.plugin")).hasSize(1);

    pom.addDependency(MavenDependency.of("io.add:added-artifact:1.0.0").build());
    pom.addDependency(MavenDependency.of("io.add:my-test-artifact:1:test").build());

    pom.addProperties(List.of("<my.one>one-value</my.one>", "<my.two>two-value</my.two>"));

    File out = new File("target/noBuild-pom2-mod1.xml");
    pom.write(out.toPath());

  }


}
