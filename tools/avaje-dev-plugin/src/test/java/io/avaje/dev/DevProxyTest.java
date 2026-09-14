package io.avaje.dev;

import static org.assertj.core.api.Assertions.assertThat;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** The proxy in front of a stand in application, over real sockets. */
class DevProxyTest {

  private final List<String> requests = new ArrayList<>();
  private final Map<String, List<String>> forwarded = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
  private final HttpClient client = HttpClient.newHttpClient();

  private HttpServer application;

  @BeforeEach
  void startApplication() throws IOException {
    application = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    application.createContext("/", this::echo);
    application.start();
  }

  @AfterEach
  void stopApplication() {
    application.stop(0);
    client.close();
  }

  private void echo(HttpExchange exchange) throws IOException {
    try (exchange) {
      final var body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
      requests.add(exchange.getRequestMethod() + " " + exchange.getRequestURI() + " " + body);
      forwarded.putAll(exchange.getRequestHeaders());
      final var response =
          ("echo " + exchange.getRequestURI() + " " + body).getBytes(StandardCharsets.UTF_8);
      exchange.getResponseHeaders().set("X-App", "yes");
      exchange.sendResponseHeaders(200, response.length);
      exchange.getResponseBody().write(response);
    }
  }

  /** A restart that never actually restarts, the application here is already running. */
  private DeferredRestart restart(java.util.function.BooleanSupplier build) {
    return new DeferredRestart(() -> () -> {}, build, () -> true, message -> {});
  }

  private HttpResponse<String> get(DevProxy proxy, String path) throws Exception {
    return send(HttpRequest.newBuilder(uri(proxy, path)).GET());
  }

  private HttpResponse<String> send(HttpRequest.Builder request) throws Exception {
    return client.send(request.build(), HttpResponse.BodyHandlers.ofString());
  }

  private URI uri(DevProxy proxy, String path) {
    return URI.create("http://127.0.0.1:" + proxy.port() + path);
  }

  private DevProxy proxy(DeferredRestart restart) {
    restart.start();
    return DevProxy.start(0, application.getAddress().getPort(), restart, message -> {});
  }

  @Test
  void requestsAreForwardedWithTheirPathQueryAndHeaders() throws Exception {
    try (var proxy = proxy(restart(() -> true))) {

      var response = get(proxy, "/hello?name=dev");

      assertThat(response.statusCode()).isEqualTo(200);
      assertThat(response.body()).isEqualTo("echo /hello?name=dev ");
      assertThat(response.headers().firstValue("X-App")).contains("yes");
      assertThat(requests).containsExactly("GET /hello?name=dev ");
      assertThat(forwarded.get("X-Forwarded-Host")).containsExactly("127.0.0.1:" + proxy.port());
    }
  }

  @Test
  void requestBodiesAreForwarded() throws Exception {
    try (var proxy = proxy(restart(() -> true))) {

      var response =
          send(
              HttpRequest.newBuilder(uri(proxy, "/save"))
                  .POST(HttpRequest.BodyPublishers.ofString("payload")));

      assertThat(response.body()).isEqualTo("echo /save payload");
      assertThat(requests).containsExactly("POST /save payload");
    }
  }

  @Test
  void nothingIsBuiltWhileNothingChanges() throws Exception {
    var builds = new AtomicInteger();
    try (var proxy = proxy(restart(() -> builds.incrementAndGet() > 0))) {

      get(proxy, "/one");
      get(proxy, "/two");

      assertThat(builds).hasValue(0);
    }
  }

  @Test
  void aChangeIsBuiltByTheRequestThatFollowsIt() throws Exception {
    var builds = new AtomicInteger();
    var restart = restart(() -> builds.incrementAndGet() > 0);
    try (var proxy = proxy(restart)) {
      restart.markStale();

      var response = get(proxy, "/after");

      assertThat(builds).hasValue(1);
      assertThat(response.statusCode()).isEqualTo(200);
      assertThat(response.body()).isEqualTo("echo /after ");
    }
  }

  @Test
  void aFailedBuildIsReportedAndTheRequestIsNotForwarded() throws Exception {
    var restart = restart(() -> false);
    try (var proxy = proxy(restart)) {
      restart.markStale();

      var response = get(proxy, "/broken");

      assertThat(response.statusCode()).isEqualTo(500);
      assertThat(response.body()).contains("build failed");
      assertThat(requests).isEmpty();
    }
  }

  @Test
  void anApplicationThatIsNotListeningIsReportedAsABadGateway() throws Exception {
    application.stop(0);
    try (var proxy = proxy(restart(() -> true))) {

      var response = get(proxy, "/gone");

      assertThat(response.statusCode()).isEqualTo(502);
    }
  }

  @Test
  void aWebsocketHandshakeIsRefusedRatherThanForwardedWithoutItsHeaders() throws Exception {
    try (var proxy = proxy(restart(() -> true))) {

      // the http client refuses to set Upgrade at all, so the handshake goes over a raw socket
      var response =
          raw(
              proxy,
              "GET /ws HTTP/1.1",
              "Host: 127.0.0.1",
              "Connection: Upgrade",
              "Upgrade: websocket",
              "Sec-WebSocket-Version: 13");

      assertThat(response).startsWith("HTTP/1.1 501");
      assertThat(response).contains("upgrades are not forwarded");
      // the application never saw it, a handshake stripped of its headers is not a handshake
      assertThat(requests).isEmpty();
    }
  }

  @Test
  void aBuildFailureIsAPageForABrowserAndTextForEverythingElse() throws Exception {
    var restart = restart(() -> false);
    try (var proxy = proxy(restart)) {
      restart.markStale();

      var text = get(proxy, "/");
      assertThat(text.statusCode()).isEqualTo(500);
      assertThat(text.headers().firstValue("Content-Type")).contains("text/plain; charset=utf-8");
      assertThat(text.body()).startsWith("The build failed - ").doesNotContain("<html");

      restart.markStale();
      var page = send(HttpRequest.newBuilder(uri(proxy, "/")).header("Accept", "text/html").GET());
      assertThat(page.statusCode()).isEqualTo(500);
      assertThat(page.headers().firstValue("Content-Type")).contains("text/html; charset=utf-8");
      assertThat(page.body()).contains("<!doctype html>").contains("The build failed");
    }
  }

  /** Send a request the http client will not build for us and read what comes back. */
  private String raw(DevProxy proxy, String... lines) throws Exception {
    try (var socket = new Socket("127.0.0.1", proxy.port())) {
      final var request = String.join("\r\n", lines) + "\r\n\r\n";
      socket.getOutputStream().write(request.getBytes(StandardCharsets.UTF_8));
      socket.getOutputStream().flush();
      // read the headers and then exactly the body, the connection is kept alive after that
      final var in = socket.getInputStream();
      final var response = new StringBuilder();
      while (!response.toString().endsWith("\r\n\r\n")) {
        final int read = in.read();
        if (read == -1) {
          return response.toString();
        }
        response.append((char) read);
      }
      final var length = length(response.toString());
      return response + new String(in.readNBytes(length), StandardCharsets.UTF_8);
    }
  }

  private static int length(String headers) {
    for (var line : headers.split("\r\n")) {
      if (line.toLowerCase(java.util.Locale.ROOT).startsWith("content-length:")) {
        return Integer.parseInt(line.substring(line.indexOf(':') + 1).trim());
      }
    }
    return 0;
  }
}
