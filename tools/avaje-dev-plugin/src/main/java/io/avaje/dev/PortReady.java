package io.avaje.dev;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.time.Duration;
import java.util.function.BooleanSupplier;

/** Waits for the application to accept connections on its port. */
final class PortReady {

  private static final int CONNECT_TIMEOUT_MILLIS = 250;
  private static final long POLL_MILLIS = 50;

  private PortReady() {}

  /** Return a check blocking until the port accepts a connection, false when it never does. */
  static BooleanSupplier of(int port, Duration timeout, DevLog log) {
    return () -> {
      final var deadline = System.nanoTime() + timeout.toNanos();
      while (true) {
        if (accepts(port)) {
          return true;
        }
        if (System.nanoTime() >= deadline) {
          log.log("nothing is listening on port %d after %ds", port, timeout.toSeconds());
          return false;
        }
        try {
          Thread.sleep(POLL_MILLIS);
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          return false;
        }
      }
    };
  }

  private static boolean accepts(int port) {
    try (var socket = new Socket()) {
      socket.connect(new InetSocketAddress("127.0.0.1", port), CONNECT_TIMEOUT_MILLIS);
      return true;
    } catch (IOException e) {
      return false;
    }
  }
}
