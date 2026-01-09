package io.ebean.tools.init;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class DependencyTest {

  @Test
  public void init() {

    Dependency dependency = new Dependency("io.ebean:ebean:11.41.1");

    assertEquals("io.ebean", dependency.getGroupId());
    assertEquals("ebean", dependency.getArtifactId());
    assertEquals("11.41.1", dependency.getVersion());
    assertNull(dependency.getScope());

    assertEquals("io.ebean:ebean", dependency.getKey());
  }

  @Test
  public void init_withScope() {

    Dependency dependency = new Dependency("io.ebean.test:ebean-test:11.41.1:test");

    assertEquals("io.ebean.test", dependency.getGroupId());
    assertEquals("ebean-test", dependency.getArtifactId());
    assertEquals("11.41.1", dependency.getVersion());
    assertEquals("test", dependency.getScope());

    assertEquals("io.ebean.test:ebean-test", dependency.getKey());
  }

  @Test
  public void init_withScope2() {

    Dependency dependency = new Dependency("io.ebean:querybean-generator:11.41.1:provided");

    assertEquals("io.ebean", dependency.getGroupId());
    assertEquals("querybean-generator", dependency.getArtifactId());
    assertEquals("11.41.1", dependency.getVersion());
    assertEquals("provided", dependency.getScope());

    assertEquals("io.ebean:querybean-generator", dependency.getKey());
  }

}
