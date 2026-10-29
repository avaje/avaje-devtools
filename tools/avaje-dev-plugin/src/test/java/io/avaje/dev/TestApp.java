package io.avaje.dev;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.CountDownLatch;

/** Stands in for the application, appending a line each time it starts and then running forever. */
public final class TestApp {

  public static void main(String[] args) throws Exception {
    Files.writeString(
        Path.of(args[0]), "started\n", StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    new CountDownLatch(1).await();
  }
}
