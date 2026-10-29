package io.avaje.dev;

/** Console output for the dev launcher, prefixed so it stands apart from application logging. */
interface DevLog {

  DevLog CONSOLE = message -> System.out.println("[avaje-dev] " + message);

  void log(String message);

  default void log(String message, Object... args) {
    log(String.format(message, args));
  }
}
