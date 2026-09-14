package io.avaje.dev;

import java.util.function.BooleanSupplier;

/**
 * Runs the application, rebuilding and restarting it on demand rather than as changes arrive.
 *
 * <p>A change only marks the application stale, the build and restart happen in {@link
 * #awaitCurrent()}, which the proxy calls before it forwards a request. An edit that is never
 * requested therefore costs nothing, and a burst of edits costs one restart.
 */
final class DeferredRestart {

  /** The state of the application after bringing it up to date. */
  enum Result {
    /** The application is running the current code and is accepting connections. */
    READY,
    /** The build failed, the previous application is left running. */
    BUILD_FAILED,
    /** The application did not start listening within the timeout. */
    NOT_READY,
    /** The launcher is shutting down. */
    STOPPED
  }

  private final AppLauncher launcher;
  private final BooleanSupplier build;
  private final BooleanSupplier ready;
  private final DevLog log;
  private final Object lock = new Object();

  private AppLauncher.AppHandle handle;
  private boolean stopped;
  private boolean awaitingStart;
  private boolean buildFailed;
  private volatile boolean stale;

  DeferredRestart(AppLauncher launcher, BooleanSupplier build, BooleanSupplier ready, DevLog log) {
    this.launcher = launcher;
    this.build = build;
    this.ready = ready;
    this.log = log;
  }

  /** Start the application, later changes wait for a request before they are picked up. */
  void start() {
    synchronized (lock) {
      if (!stopped && handle == null) {
        handle = launcher.start();
        awaitingStart = true;
      }
    }
  }

  /** Mark the application stale so the next request rebuilds and restarts it. */
  void markStale() {
    stale = true;
  }

  /** True when a change has been seen and not yet built. */
  boolean stale() {
    return stale;
  }

  /**
   * Mark the application stale for every change until the source is closed or we are interrupted.
   */
  void watch(ChangeSource changes) {
    try {
      while (changes.awaitChange()) {
        changes.drain();
        log.log("change detected, restarting on the next request");
        markStale();
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  /**
   * Rebuild and restart as soon as each change settles, rather than waiting for the next request to
   * notice it is stale.
   */
  void watchEager(ChangeSource changes) {
    try {
      while (changes.awaitChange()) {
        changes.drain();
        log.log("change detected, restarting");
        markStale();
        awaitCurrent();
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  /**
   * Bring the application up to date, returning once it is ready to serve.
   *
   * <p>Callers arriving during a restart wait for it rather than starting one of their own.
   */
  Result awaitCurrent() {
    synchronized (lock) {
      if (stopped) {
        return Result.STOPPED;
      }
      if (stale) {
        // cleared first, a change arriving during the build leaves it stale for the next request
        stale = false;
        buildFailed = false;
        if (!build.getAsBoolean()) {
          buildFailed = true;
          log.log("build failed, leaving the running application in place");
          return Result.BUILD_FAILED;
        }
        log.log("restarting");
        restart();
      } else if (buildFailed) {
        // nothing changed since it failed, so building again would only fail again
        return Result.BUILD_FAILED;
      }
      if (awaitingStart) {
        if (!ready.getAsBoolean()) {
          return Result.NOT_READY;
        }
        awaitingStart = false;
      }
      return Result.READY;
    }
  }

  /** Stop the running application, the watch loop exits once its change source is closed. */
  void stop() {
    synchronized (lock) {
      stopped = true;
      if (handle != null) {
        handle.stop();
        handle = null;
      }
    }
  }

  private void restart() {
    if (handle != null) {
      handle.stop();
      handle = null;
    }
    handle = launcher.start();
    awaitingStart = true;
  }
}
