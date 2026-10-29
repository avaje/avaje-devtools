package io.avaje.dev;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/** Watches directory trees, coalescing bursts of changes into a single notification. */
final class DirWatcher implements ChangeSource, AutoCloseable {

  private final WatchService watchService;
  private final long quietMillis;

  private final Set<Path> treeDirs = new HashSet<>();
  private final Set<Path> watchedFiles = new HashSet<>();

  DirWatcher(List<Path> roots, Duration quietPeriod) {
    this(roots, List.of(), quietPeriod);
  }

  DirWatcher(List<Path> roots, List<Path> files, Duration quietPeriod) {
    this.quietMillis = quietPeriod.toMillis();
    try {
      this.watchService = roots.get(0).getFileSystem().newWatchService();
      for (var root : roots) {
        registerAll(root);
      }
      for (var file : files) {
        registerFile(file);
      }
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  /** Register the directory holding the file, only that name counts as a change in it. */
  private void registerFile(Path file) throws IOException {
    final var absolute = file.toAbsolutePath().normalize();
    final var dir = absolute.getParent();
    if (dir == null || !Files.isDirectory(dir)) {
      return;
    }
    watchedFiles.add(absolute);
    if (!treeDirs.contains(dir)) {
      dir.register(watchService, ENTRY_CREATE, ENTRY_MODIFY, ENTRY_DELETE);
    }
  }

  @Override
  public boolean awaitChange() throws InterruptedException {
    try {
      while (true) {
        final var key = watchService.take();
        if (interesting(key)) {
          settle();
          return true;
        }
      }
    } catch (ClosedWatchServiceException e) {
      return false;
    }
  }

  @Override
  public void drain() {
    try {
      for (WatchKey key = watchService.poll(); key != null; key = watchService.poll()) {
        key.pollEvents();
        key.reset();
      }
    } catch (ClosedWatchServiceException e) {
      // closing while draining is not a problem
    }
  }

  @Override
  public void close() {
    try {
      watchService.close();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  /** Wait until no further change arrives for the quiet period, editors write in bursts. */
  private void settle() throws InterruptedException {
    for (WatchKey key = watchService.poll(quietMillis, TimeUnit.MILLISECONDS);
        key != null;
        key = watchService.poll(quietMillis, TimeUnit.MILLISECONDS)) {
      interesting(key);
    }
  }

  /** Consume the events of the key, registering new directories, and reset it for reuse. */
  private boolean interesting(WatchKey key) {
    boolean interesting = false;
    final var dir = (Path) key.watchable();
    final var tree = treeDirs.contains(dir);
    for (var event : key.pollEvents()) {
      if (event.context() instanceof Path path) {
        final var changed = dir.resolve(path);
        if (!tree) {
          // registered to hear about named files, the rest of the directory is not ours
          interesting |= watchedFiles.contains(changed.toAbsolutePath().normalize());
        } else if (ENTRY_CREATE.equals(event.kind()) && Files.isDirectory(changed)) {
          registerAll(changed);
          interesting = true;
        } else if (!ignored(changed)) {
          interesting = true;
        }
      }
    }
    key.reset();
    return interesting;
  }

  /** Editor scratch files change constantly and never mean the application changed. */
  private static boolean ignored(Path path) {
    final var name = path.getFileName().toString();
    return name.startsWith(".")
        || name.endsWith("~")
        || name.endsWith(".swp")
        || name.endsWith(".swx")
        || name.endsWith(".tmp");
  }

  private void registerAll(Path root) {
    try {
      Files.walkFileTree(root, new Registrar());
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private final class Registrar implements FileVisitor<Path> {

    @Override
    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
        throws IOException {
      if (dir.getFileName() != null && dir.getFileName().toString().startsWith(".")) {
        return FileVisitResult.SKIP_SUBTREE;
      }
      dir.register(watchService, ENTRY_CREATE, ENTRY_MODIFY, ENTRY_DELETE);
      treeDirs.add(dir);
      return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
      return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFileFailed(Path file, IOException exc) {
      return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
      return FileVisitResult.CONTINUE;
    }
  }
}
