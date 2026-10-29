package io.avaje.dev;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.function.BooleanSupplier;

/** Runs the configured build command, reporting whether it succeeded. */
final class BuildCommand {

  private BuildCommand() {}

  /** Return a build returning true when there is nothing to build or the build succeeded. */
  static BooleanSupplier of(List<String> command, Path dir, DevLog log) {
    if (command.isEmpty()) {
      return () -> true;
    }
    return () -> {
      log.log("building with %s", String.join(" ", command));
      try {
        final var builder = new ProcessBuilder(command).inheritIO();
        if (dir != null) {
          builder.directory(dir.toFile());
        }
        return builder.start().waitFor() == 0;
      } catch (IOException e) {
        log.log("build failed to run - %s", e.getMessage());
        return false;
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        return false;
      }
    };
  }
}
