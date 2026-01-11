package io.ebean.tools.init;

import org.junit.jupiter.api.Test;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;

public class MavenPomTest {

  @Test
  public void parse() {

    File example = new File("src/test/resources/maven/one-pom.xml");

    MavenPom pom = new MavenPom(example);

    final MavenPom.MavenDependency dependency = pom.findLastNonTestDependency();

    assertThat(dependency.end).isEqualTo(37);
    assertThat(dependency.artifactId).isEqualTo("finder-generator");

    assertThat(pom.hasDependencyEbean()).isFalse();
    assertThat(pom.hasDependency("io.ebean", "ebean-querybean")).isFalse();
    assertThat(pom.hasDependency("io.ebean", "querybean-generator")).isFalse();
  }

  @Test
  public void pomWithEbean() {

    File example = new File("src/test/resources/maven/withEbean-pom.xml");

    MavenPom pom = new MavenPom(example);

    assertThat(pom.hasDependencyEbean()).isTrue();
    assertThat(pom.hasDependency("io.ebean", "ebean-querybean")).isTrue();
    assertThat(pom.hasDependency("io.ebean", "querybean-generator")).isTrue();

    final MavenPom.MavenDependency dependency = pom.findLastNonTestDependency();
    assertThat(dependency.end).isEqualTo(40);
    assertThat(dependency.artifactId).isEqualTo("ebean");

  }
}
