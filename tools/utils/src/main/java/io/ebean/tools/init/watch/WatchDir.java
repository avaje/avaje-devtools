package io.ebean.tools.init.watch;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Map;

import static java.lang.System.Logger.Level.ERROR;
import static java.lang.System.Logger.Level.INFO;
import static java.nio.file.LinkOption.NOFOLLOW_LINKS;
import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
import static java.nio.file.StandardWatchEventKinds.OVERFLOW;

/**
 * Example to watch a directory (or tree) for changes to files.
 */
public class WatchDir implements Runnable {

  private static final System.Logger log = System.getLogger("io.avaje.tools");

  private Thread workerThread;

  private final WatchService watcher;

  private final Map<WatchKey, Path> keys;

  private final boolean recursive;

  private boolean trace;

  private final WatchDirCallback callback;

  private final WatchDirSkip skipDir;

  @SuppressWarnings("unchecked")
  static <T> WatchEvent<T> cast(WatchEvent<?> event) {
    return (WatchEvent<T>) event;
  }

  /**
   * Register the given directory with the WatchService
   */
  private void register(Path dir) throws IOException {
    WatchKey key = dir.register(watcher, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
    if (trace) {
      Path prev = keys.get(key);
      if (prev == null) {
        log.log(INFO, "register: {0}", dir);
      } else {
        if (!dir.equals(prev)) {
          log.log(INFO, "update: {0} -> {1}", prev, dir);
        }
      }
    }
    keys.put(key, dir);
  }

  /**
   * Register the given directory, and all its sub-directories, with the WatchService.
   */
  private void registerAll(final Path start) throws IOException {

    Files.walkFileTree(start, new SimpleFileVisitor<Path>() {

      @Override
      public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {

        if (skipDir != null && skipDir.skip(dir)) {
          return FileVisitResult.SKIP_SUBTREE;
        }

        register(dir);
        return FileVisitResult.CONTINUE;
      }
    });
  }

  /**
   * Creates a WatchService and registers the given directory
   *
   * <p>No WatchDirSkip specified.
   */
  public WatchDir(Path dir, boolean recursive, WatchDirCallback callback) {
    this(dir, recursive, callback, null);
  }

  /**
   * Creates a WatchService and registers the given directory
   */
  public WatchDir(Path dir, boolean recursive, WatchDirCallback callback, WatchDirSkip skipDir) {

    try {
      this.watcher = FileSystems.getDefault().newWatchService();

      this.keys = new HashMap<>();
      this.recursive = recursive;
      this.callback = callback;
      this.skipDir = skipDir;

      if (recursive) {
        log.log(INFO, "Scanning {0} ...", dir);
        registerAll(dir);
        log.log(INFO, "Watching...");
      } else {
        register(dir);
      }
      // enable trace after initial registration
      this.trace = true;

    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }

  public void start() {
    workerThread = new Thread(this, "watcher");
    workerThread.setDaemon(true);
    workerThread.start();
  }

  /**
   * Shutdown this listener.
   */
  public void stop() {
    try {
//      try {
//        workerThread.wait(1000);
//      } catch (InterruptedException e) {
//        // OK to ignore as expected to Interrupt for shutdown.
//      }
      workerThread.interrupt();
      workerThread = null;

    } catch (Exception e) {
      log.log(ERROR, "Error stopping watcher", e);
    }
  }

  @Override
  public void run() {
    processEvents();
  }

  /**
   * Process all events for keys queued to the watcher
   */
  public void processEvents() {
    for (; ; ) {

      // wait for key to be signalled
      WatchKey key;
      try {
        key = watcher.take();
      } catch (InterruptedException x) {
        return;
      }

      Path dir = keys.get(key);
      if (dir == null) {
        log.log(ERROR, "WatchKey {0} not recognized!!", key);
        continue;
      }

      for (WatchEvent<?> event : key.pollEvents()) {
        WatchEvent.Kind<?> kind = event.kind();

        if (kind == OVERFLOW) {
          log.log(ERROR, "OVERFLOW event is not handled");
          continue;
        }

        // Context for directory entry event is the file name of entry
        WatchEvent<Path> ev = cast(event);
        Path name = ev.context();
        Path child = dir.resolve(name);

        // print out event
        if (callback != null) {
          callback.event(ev, child, event.kind().toString());

        } else {
          log.log(INFO, " event: {0} {1}", event.kind().name(), child);
        }

        // if directory is created, and watching recursively, then
        // register it and its sub-directories
        if (recursive && (kind == ENTRY_CREATE)) {
          try {
            if (skipDir != null && skipDir.skip(child)) {

            } else {
              if (Files.isDirectory(child, NOFOLLOW_LINKS)) {
                registerAll(child);
              }
            }
          } catch (IOException e) {
            log.log(ERROR, "Error registering new directory", e);
          }
        }
      }

      // reset key and remove from set if directory no longer accessible
      boolean valid = key.reset();
      if (!valid) {
        keys.remove(key);

        // all directories are inaccessible
        if (keys.isEmpty()) {
          break;
        }
      }
    }
  }
}
