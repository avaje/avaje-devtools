package io.ebean.tools.init;

import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DetectionTest {

  @Test
  public void diff() {

    File f0 = new File(".");
    File f1 = new File(".");

    Detection detection = new Detection(new DetectionMeta());
    assertEquals("", detection.diff(f0, f1));
  }
}
