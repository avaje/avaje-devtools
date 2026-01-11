package io.ebean.tools.init;

import org.junit.jupiter.api.Test;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class GradleBuildTest {

  @Test
  public void isNewProject() {

    File example = new File("src/test/resources/gradle/one.build");

    GradleBuild build = new GradleBuild(example);
    assertThat(build.isNewProject()).isTrue();

    assertFalse(build.hasEbeanPlugin());
    assertTrue(build.hasOpenBracketForDependencies());

    assertFalse(build.hasDependencyEbean());
    assertFalse(build.hasDependencyEbeanQueryBean());
    assertFalse(build.hasDependencyEbeanQueryBeanGenerator());
  }

}
