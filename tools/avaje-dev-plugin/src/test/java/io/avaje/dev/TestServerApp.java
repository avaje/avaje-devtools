package io.avaje.dev;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;

/**
 * Stands in for the application, serving the content of a file as it was when it started, so a
 * response that has not changed means the application was not restarted.
 */
public final class TestServerApp {

  public static void main(String[] args) throws Exception {
    final var body = Files.readString(Path.of(args[0])).getBytes(StandardCharsets.UTF_8);
    final var port = Integer.parseInt(System.getProperty(DevOptions.DEFAULT_PORT_PROPERTY));
    final var server = HttpServer.create(new InetSocketAddress("127.0.0.1", port), 0);
    server.createContext("/", exchange -> respond(exchange, body));
    server.start();
    new CountDownLatch(1).await();
  }

  private static void respond(com.sun.net.httpserver.HttpExchange exchange, byte[] body)
      throws IOException {
    try (exchange) {
      exchange.sendResponseHeaders(200, body.length);
      exchange.getResponseBody().write(body);
    }
  }
}
