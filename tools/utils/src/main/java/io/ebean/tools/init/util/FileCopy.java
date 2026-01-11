package io.ebean.tools.init.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

public class FileCopy {

  public static void copy(File toFile, String fromResource) throws IOException {

    new FileCopy().copyResource(toFile, fromResource);
  }

  void copyResource(File toFile, String fromResource) throws IOException {

    try (InputStream in = this.getClass().getResourceAsStream(fromResource)) {
      Files.copy(in, toFile.toPath());
    }
  }
}
