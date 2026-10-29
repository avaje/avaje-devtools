package io.avaje.dev;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DirWatcherTest {

  private static final Duration QUIET = Duration.ofMillis(50);
  private static final int TIMEOUT_SECONDS = 20;

  @Test
  void changedFileIsAChange(@TempDir Path dir) throws Exception {
    try (var watcher = new DirWatcher(List.of(dir), QUIET)) {
      var change = awaitChange(watcher);

      Files.writeString(dir.resolve("App.class"), "changed");

      assertThat(change.get(TIMEOUT_SECONDS, TimeUnit.SECONDS)).isTrue();
    }
  }

  @Test
  void fileInANewSubDirectoryIsAChange(@TempDir Path dir) throws Exception {
    try (var watcher = new DirWatcher(List.of(dir), QUIET)) {
      var created = awaitChange(watcher);
      var sub = Files.createDirectory(dir.resolve("org"));
      assertThat(created.get(TIMEOUT_SECONDS, TimeUnit.SECONDS)).isTrue();

      // the new directory is watched too
      var change = awaitChange(watcher);
      Files.writeString(sub.resolve("App.class"), "changed");

      assertThat(change.get(TIMEOUT_SECONDS, TimeUnit.SECONDS)).isTrue();
    }
  }

  @Test
  void existingSubDirectoriesAreWatched(@TempDir Path dir) throws Exception {
    var sub = Files.createDirectories(dir.resolve("org/example"));
    try (var watcher = new DirWatcher(List.of(dir), QUIET)) {
      var change = awaitChange(watcher);

      Files.writeString(sub.resolve("App.class"), "changed");

      assertThat(change.get(TIMEOUT_SECONDS, TimeUnit.SECONDS)).isTrue();
    }
  }

  @Test
  void editorScratchFilesAreIgnored(@TempDir Path dir) throws Exception {
    try (var watcher = new DirWatcher(List.of(dir), QUIET)) {
      var change = awaitChange(watcher);

      Files.writeString(dir.resolve(".App.class.swp"), "vim");
      Files.writeString(dir.resolve("App.class~"), "backup");
      Files.writeString(dir.resolve("App.class.tmp"), "partial");
      // nothing interesting has changed yet
      assertThat(change).isNotDone();

      Files.writeString(dir.resolve("App.class"), "changed");

      assertThat(change.get(TIMEOUT_SECONDS, TimeUnit.SECONDS)).isTrue();
    }
  }

  @Test
  void closingEndsTheWait(@TempDir Path dir) throws Exception {
    var watcher = new DirWatcher(List.of(dir), QUIET);
    var change = awaitChange(watcher);

    watcher.close();

    assertThat(change.get(TIMEOUT_SECONDS, TimeUnit.SECONDS)).isFalse();
  }

  @Test
  void drainDiscardsChangesSeenSoFar(@TempDir Path dir) throws Exception {
    try (var watcher = new DirWatcher(List.of(dir), QUIET)) {
      Files.writeString(dir.resolve("App.class"), "changed");
      Thread.sleep(200);
      watcher.drain();

      var change = awaitChange(watcher);
      Thread.sleep(200);
      assertThat(change).isNotDone();

      Files.writeString(dir.resolve("Other.class"), "changed");
      assertThat(change.get(TIMEOUT_SECONDS, TimeUnit.SECONDS)).isTrue();
    }
  }

  @Test
  void aWatchedFileIsAChangeButItsNeighboursAreNot(@TempDir Path dir) throws Exception {
    var project = Files.createDirectory(dir.resolve("project"));
    var pom = project.resolve("pom.xml");
    Files.writeString(pom, "<project/>");
    var classes = Files.createDirectory(dir.resolve("classes"));

    try (var watcher = new DirWatcher(List.of(classes), List.of(pom), QUIET)) {
      var change = awaitChange(watcher);

      // the directory holding the pom is registered, so its neighbours are seen and passed over
      Files.writeString(project.resolve("notes.txt"), "ignored");
      Files.createDirectory(project.resolve("src"));
      Thread.sleep(200);
      assertThat(change).isNotDone();

      Files.writeString(pom, "<project> </project>");

      assertThat(change.get(TIMEOUT_SECONDS, TimeUnit.SECONDS)).isTrue();
    }
  }

  private static CompletableFuture<Boolean> awaitChange(DirWatcher watcher) throws Exception {
    var started = new CompletableFuture<Void>();
    var result =
        CompletableFuture.supplyAsync(
            () -> {
              try {
                started.complete(null);
                return watcher.awaitChange();
              } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(e);
              }
            });
    started.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
    // let the waiting thread reach the watch service before the test changes a file
    Thread.sleep(100);
    return result;
  }
}
