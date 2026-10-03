package io.avaje.dev;

import java.time.Duration;
import java.util.Arrays;

/**
 * Development launcher that runs an application in a child JVM and restarts it when files change.
 *
 * <p>By default it watches {@code target/classes} and {@code src/main/resources}, so a build run
 * from an IDE or another terminal triggers the restart. Give it a build command to have it compile
 * as well, in which case the sources are watched instead.
 *
 * <p>Given a port it serves that port itself, forwarding to the application on the port above it
 * and restarting on the first request that follows a change, so editing without requesting anything
 * rebuilds nothing.
 */
public final class DevRun {

  /** How long the application is given to start listening before a request gives up on it. */
  private static final Duration STARTUP_TIMEOUT = Duration.ofSeconds(60);

  private DevRun() {}

  /** Run the launcher, exiting the JVM when the command line cannot be used. */
  public static void main(String[] args) {
    if (args.length == 0 || Arrays.asList(args).contains("--help")) {
      System.out.println(DevOptions.USAGE);
      return;
    }
    final DevOptions options;
    try {
      options = DevOptions.parse(args);
    } catch (DevOptions.InvalidOptions e) {
      System.err.println(e.getMessage());
      System.err.println();
      System.err.println(DevOptions.USAGE);
      System.exit(2);
      return;
    }
    run(options, DevLog.CONSOLE);
  }

  /**
   * Run the launcher with the given command line, blocking until it is interrupted.
   *
   * @throws IllegalArgumentException when the command line cannot be used
   */
  public static void run(String... args) {
    try {
      run(DevOptions.parse(args), DevLog.CONSOLE);
    } catch (DevOptions.InvalidOptions e) {
      throw new IllegalArgumentException(e.getMessage(), e);
    }
  }

  static void run(DevOptions options, DevLog log) {
    log.log("watching %s", options.watchDirs());
    if (!options.watchFiles().isEmpty()) {
      log.log("watching %s", options.watchFiles());
      // the classpath was resolved when the goal started and is not resolved again, so a build
      // file change rebuilds and restarts but a dependency added to one does not take effect
      log.log("a dependency added to a build file needs this restarted to reach the classpath");
    }
    try (var watcher =
        new DirWatcher(options.watchDirs(), options.watchFiles(), options.quietPeriod())) {
      if (options.proxied()) {
        runProxied(options, watcher, log);
      } else {
        runEager(options, watcher, log);
      }
    }
  }

  /** Restart as changes arrive */
  private static void runEager(DevOptions options, DirWatcher watcher, DevLog log) {
    final var loop =
        new RestartLoop(
            AppProcess.launcher(options, log),
            BuildCommand.of(options.buildCommand(), options.buildDir(), log),
            log);
    // stop the application on ctrl-c, the loop itself exits once the watcher is closed
    shutdownHook(
        () -> {
          loop.stop();
          watcher.close();
        });
    loop.run(watcher);
  }

  /**
   * Serve the configured port ourselves, restarting on the first request after a change so an edit
   * that is never requested costs nothing, unless {@link DevOptions#eagerRestart()} says to restart
   * as soon as the change settles instead.
   */
  private static void runProxied(DevOptions options, DirWatcher watcher, DevLog log) {
    final var restart =
        new DeferredRestart(
            AppProcess.launcher(options, log),
            BuildCommand.of(options.buildCommand(), options.buildDir(), log),
            PortReady.of(options.appPort(), STARTUP_TIMEOUT, log),
            log);
    final var proxy = DevProxy.start(options.port(), options.appPort(), restart, log);
    shutdownHook(
        () -> {
          proxy.close();
          restart.stop();
          watcher.close();
        });
    try {
      restart.start();
      if (options.eagerRestart()) {
        restart.watchEager(watcher);
      } else {
        restart.watch(watcher);
      }
    } finally {
      proxy.close();
      restart.stop();
    }
  }

  private static void shutdownHook(Runnable stop) {
    Runtime.getRuntime().addShutdownHook(new Thread(stop, "avaje-dev-shutdown"));
  }
}
