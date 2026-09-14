package io.avaje.dev;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** End to end, a real child JVM restarted by a real file change. */
class DevRunRestartTest {

  private static final int TIMEOUT_SECONDS = 30;

  @Test
  void changedFileRestartsTheApplication(@TempDir Path dir) throws Exception {
    final var watched = Files.createDirectory(dir.resolve("classes"));
    final var starts = dir.resolve("starts.txt");

    final var options =
        new DevOptions(
            TestApp.class.getName(),
            List.of(starts.toString()),
            List.of(),
            "target/classes" + File.pathSeparator + "target/test-classes",
            List.of(watched),
            List.of(),
            List.of(),
            null,
            false,
            Duration.ofMillis(50),
            false,
            0,
            0,
            DevOptions.DEFAULT_PORT_PROPERTY);

    final var watcher = new DirWatcher(options.watchDirs(), options.quietPeriod());
    final var loop =
        new RestartLoop(AppProcess.launcher(options, message -> {}), () -> true, message -> {});
    final var running = CompletableFuture.runAsync(() -> loop.run(watcher));
    try {
      awaitStarts(starts, 1);

      Files.writeString(watched.resolve("App.class"), "changed");

      awaitStarts(starts, 2);
    } finally {
      loop.stop();
      watcher.close();
      running.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
    }

    // the loop stopped the child, nothing is left running to write another line
    assertThat(lines(starts)).isEqualTo(2);
  }

  private static void awaitStarts(Path starts, int expected) throws Exception {
    for (int i = 0; i < TIMEOUT_SECONDS * 20; i++) {
      if (lines(starts) >= expected) {
        return;
      }
      Thread.sleep(50);
    }
    throw new AssertionError("expected " + expected + " starts but saw " + lines(starts));
  }

  private static int lines(Path starts) throws Exception {
    return Files.exists(starts) ? Files.readAllLines(starts).size() : 0;
  }
}
