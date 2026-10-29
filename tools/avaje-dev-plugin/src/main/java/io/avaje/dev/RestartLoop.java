package io.avaje.dev;

import java.util.function.BooleanSupplier;

/** Runs the application, restarting it each time the change source reports a change. */
final class RestartLoop {

  private final AppLauncher launcher;
  private final BooleanSupplier build;
  private final DevLog log;
  private final Object lock = new Object();

  private AppLauncher.AppHandle handle;
  private boolean stopped;

  RestartLoop(AppLauncher launcher, BooleanSupplier build, DevLog log) {
    this.launcher = launcher;
    this.build = build;
    this.log = log;
  }

  /**
   * Start the application and restart it until the change source is closed, we are interrupted, or
   * {@link #stop()} is called.
   */
  void run(ChangeSource changes) {
    start();
    try {
      while (changes.awaitChange()) {
        if (!build.getAsBoolean()) {
          log.log("build failed, leaving the running application in place");
          changes.drain();
          continue;
        }
        changes.drain();
        synchronized (lock) {
          if (stopped) {
            return;
          }
          log.log("change detected, restarting");
          handle.stop();
        }
        start();
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    } finally {
      stop();
    }
  }

  /** Stop the running application, the loop exits once its change source is closed. */
  void stop() {
    synchronized (lock) {
      stopped = true;
      if (handle != null) {
        handle.stop();
        handle = null;
      }
    }
  }

  private void start() {
    synchronized (lock) {
      if (!stopped) {
        handle = launcher.start();
      }
    }
  }
}
