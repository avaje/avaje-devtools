package io.avaje.tools.util.maven;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class DependencyTest {

  @Test
  void init() {
    var dependency = MavenDependency.of("io.ebean:ebean:11.41.1").build();

    assertEquals("io.ebean", dependency.groupId());
    assertEquals("ebean", dependency.artifactId());
    assertEquals("11.41.1", dependency.version());
    assertNull(dependency.scope());

    assertEquals("io.ebean:ebean", dependency.key());
  }

  @Test
  void init_withScope() {
    var dependency = MavenDependency.of("io.ebean.test:ebean-test:11.41.1:test").build();

    assertEquals("io.ebean.test", dependency.groupId());
    assertEquals("ebean-test", dependency.artifactId());
    assertEquals("11.41.1", dependency.version());
    assertEquals("test", dependency.scope());

    assertEquals("io.ebean.test:ebean-test", dependency.key());
  }

  @Test
  void init_withScope2() {
    var dependency = MavenDependency.of("io.ebean:querybean-generator:11.41.1:provided").build();

    assertEquals("io.ebean", dependency.groupId());
    assertEquals("querybean-generator", dependency.artifactId());
    assertEquals("11.41.1", dependency.version());
    assertEquals("provided", dependency.scope());

    assertEquals("io.ebean:querybean-generator", dependency.key());
  }

}
