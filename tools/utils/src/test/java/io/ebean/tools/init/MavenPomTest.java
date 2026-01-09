package io.ebean.tools.init;

import org.junit.jupiter.api.Test;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;

public class MavenPomTest {

  @Test
  public void parse() {

    File example = new File("src/test/resources/maven/one-pom.xml");

    MavenPom pom = new MavenPom(example);
    assertThat(pom.findTilesPlugin()).isNull();

    final MavenPom.MavenDependency dependency = pom.findLastNonTestDependency();

    assertThat(dependency.end).isEqualTo(37);
    assertThat(dependency.artifactId).isEqualTo("finder-generator");

    assertThat(pom.hasDependencyEbean()).isFalse();
    assertThat(pom.hasDependencyEbeanQueryBean()).isFalse();
    assertThat(pom.hasDependencyEbeanQueryBeanGenerator()).isFalse();
  }

  @Test
  public void pomWithEbean() {

    File example = new File("src/test/resources/maven/withEbean-pom.xml");

    MavenPom pom = new MavenPom(example);

    assertThat(pom.hasDependencyEbean()).isTrue();
    assertThat(pom.hasDependencyEbeanQueryBean()).isTrue();
    assertThat(pom.hasDependencyEbeanQueryBeanGenerator()).isTrue();

    final MavenPom.MavenDependency dependency = pom.findLastNonTestDependency();
    assertThat(dependency.end).isEqualTo(40);
    assertThat(dependency.artifactId).isEqualTo("ebean");

    final MavenPom.MavenPlugin tilesPlugin = pom.findTilesPlugin();
    assertThat(tilesPlugin.start).isEqualTo(77);

  }
}
