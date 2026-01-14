package io.avaje.tools.util.maven;

import org.junit.jupiter.api.Test;

import static io.avaje.tools.util.maven.TreeReader.isSingleLineEmptyXmlTag;
import static org.junit.jupiter.api.Assertions.*;

class TreeReaderTest {

  @Test
  void expect_true() {
    assertTrue(isSingleLineEmptyXmlTag("<resources/>"));
    assertTrue(isSingleLineEmptyXmlTag("<resources />"));
    assertTrue(isSingleLineEmptyXmlTag("<resources  />"));
    assertTrue(isSingleLineEmptyXmlTag("<relativePath />"));
    assertTrue(isSingleLineEmptyXmlTag("<foo  />"));
  }

  @Test
  void expect_false() {
    assertFalse(isSingleLineEmptyXmlTag("<resources> </resources>"));
    assertFalse(isSingleLineEmptyXmlTag("<resources>"));
    assertFalse(isSingleLineEmptyXmlTag("</resources>"));
  }
}
