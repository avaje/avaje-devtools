package io.avaje.dev;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/** The application running in a child JVM. */
final class AppProcess implements AppLauncher.AppHandle {

  private static final int STOP_TIMEOUT_SECONDS = 10;

  /** Any collector the application chose for itself */
  private static final Pattern COLLECTOR = Pattern.compile("-XX:[+-]Use\\w+GC");

  private final Process process;
  private final DevLog log;
  private volatile boolean stopping;

  private AppProcess(Process process, DevLog log) {
    this.process = process;
    this.log = log;
    process
        .onExit()
        .thenAccept(
            exited -> {
              if (!stopping) {
                log.log("application exited with %d, waiting for changes", exited.exitValue());
              }
            });
  }

  /** Return a launcher starting the application described by the given options. */
  static AppLauncher launcher(DevOptions options, DevLog log) {
    final var command = command(options);
    return () -> {
      try {
        log.log("starting %s", options.mainClass());
        return new AppProcess(new ProcessBuilder(command).inheritIO().start(), log);
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }
    };
  }

  /** Flags that favour starting fast over running long. */
  static List<String> startArgs(DevOptions options) {
    final var args = new ArrayList<String>();
    final var jvmArgs = options.jvmArgs();
    if (options.quickStart()) {
      // the code is probably replaced long before c2 would have paid for itself
      args.add("-XX:TieredStopAtLevel=1");
      if (!selects(jvmArgs, COLLECTOR)) {
        args.add("-XX:+UseSerialGC");
      }
    }
    return args;
  }

  private static boolean selects(List<String> jvmArgs, Pattern option) {
    return jvmArgs.stream().anyMatch(arg -> option.matcher(arg).matches());
  }

  @Override
  public void stop() {
    stopping = true;
    process.destroy();
    try {
      if (!process.waitFor(STOP_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
        log.log("application did not stop within %ds, killing it", STOP_TIMEOUT_SECONDS);
        process.destroyForcibly().waitFor();
      }
    } catch (InterruptedException e) {
      process.destroyForcibly();
      Thread.currentThread().interrupt();
    }
  }

  private static List<String> command(DevOptions options) {
    final var command = new ArrayList<String>();
    command.add(Path.of(System.getProperty("java.home"), "bin", "java").toString());
    if (options.proxied()) {
      // ours goes first so an explicitly configured jvm arg still wins
      command.add("-D" + options.portProperty() + "=" + options.appPort());
    }
    command.addAll(startArgs(options));
    command.addAll(options.jvmArgs());
    command.add("-cp");
    command.add(options.classpath());
    command.add(options.mainClass());
    command.addAll(options.appArgs());
    return command;
  }
}
