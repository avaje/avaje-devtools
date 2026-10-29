package io.avaje.dev;

import com.sun.net.httpserver.HttpExchange;
import io.avaje.jex.Jex;
import io.avaje.jex.http.Context;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Serves the dev port, bringing the application up to date before each request is forwarded to it.
 *
 * <p>The application itself listens on another port, so an edit is picked up by the request that
 * follows it and nothing is rebuilt while the code sits untouched.
 */
final class DevProxy implements AutoCloseable {

  private static final int COPY_BUFFER = 8192;

  /**
   * Headers belonging to a single connection, they describe the hop we terminate rather than the
   * message, and the client rejects most of them outright.
   */
  private static final Set<String> HOP_BY_HOP =
      Set.of(
          "connection",
          "content-length",
          "expect",
          "host",
          "keep-alive",
          "proxy-authenticate",
          "proxy-authorization",
          "te",
          "trailer",
          "transfer-encoding",
          "upgrade");

  /** The page the browser is given when we answer instead of the application. */
  private static final String PAGE =
      """
      <!doctype html>
      <html lang='en'>
      <head>
      <meta charset='utf-8'>
      <meta name='viewport' content='width=device-width, initial-scale=1'>
      <title>%d %s</title>
      <style>
      :root { color-scheme: light dark; }
      body { margin: 0; display: grid; place-items: center; min-height: 100vh;
             font: 16px/1.6 system-ui, sans-serif; }
      main { max-width: 34rem; padding: 2rem; }
      p.status { margin: 0 0 .5rem; font-size: .8rem; letter-spacing: .1em; opacity: .6; }
      h1 { margin: 0 0 1rem; font-size: 1.5rem; font-weight: 600; }
      p.detail { margin: 0; opacity: .8; }
      footer { margin-top: 2rem; font-size: .8rem; opacity: .5; }
      </style>
      </head>
      <body>
      <main>
      <p class='status'>HTTP %d</p>
      <h1>%s</h1>
      <p class='detail'>%s</p>
      <footer>avaje dev &middot; reload once it is sorted</footer>
      </main>
      </body>
      </html>
      """;

  private final HttpClient client;
  private final ExecutorService executor;
  private final URI target;
  private final DeferredRestart restart;
  private final DevLog log;
  private Jex.Server server;

  private DevProxy(ExecutorService executor, URI target, DeferredRestart restart, DevLog log) {
    this.executor = executor;
    this.target = target;
    this.restart = restart;
    this.log = log;
    this.client =
        HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();
  }

  /** Start serving the dev port, forwarding to the application port. */
  static DevProxy start(int port, int appPort, DeferredRestart restart, DevLog log) {
    final var executor = Executors.newVirtualThreadPerTaskExecutor();
    final var proxy =
        new DevProxy(executor, URI.create("http://127.0.0.1:" + appPort), restart, log);
    proxy.server =
        Jex.create()
            .config(cfg -> cfg.port(port).health(false).executor(executor))
            .get("/*", proxy::handle)
            .post("/*", proxy::handle)
            .put("/*", proxy::handle)
            .patch("/*", proxy::handle)
            .delete("/*", proxy::handle)
            .options("/*", proxy::handle)
            .query("/*", proxy::handle)
            .routing(
                routing ->
                    routing
                        .head("/*", proxy::handle)
                        .trace("/*", proxy::handle)
                        .connect("/*", proxy::handle))
            .start();
    log.log("serving http://localhost:%d, application on port %d", proxy.server.port(), appPort);
    return proxy;
  }

  /** The port being served, useful when the proxy was given port 0. */
  int port() {
    return server.port();
  }

  @Override
  public void close() {
    server.shutdown();
    client.close();
    executor.shutdownNow();
  }

  private void handle(Context ctx) throws IOException {
    if (upgrade(ctx)) {
      respond(
          ctx,
          501,
          "Connection upgrades are not forwarded",
          "The dev proxy terminates HTTP and cannot hand the connection over. Run without a port"
              + " and the application owns the port itself, which upgrades fine.");
      return;
    }
    switch (restart.awaitCurrent()) {
      case READY -> forward(ctx);
      case BUILD_FAILED ->
          respond(
              ctx,
              500,
              "The build failed",
              "The compiler output is on the console. Nothing is built again until something"
                  + " changes, so fix it and ask for this again.");
      case NOT_READY ->
          respond(
              ctx,
              503,
              "The application is not listening yet",
              "It was started and has not taken its port within the timeout.");
      case STOPPED -> respond(ctx, 503, "The launcher is shutting down", "");
    }
  }

  /** True when the request asks to leave HTTP behind, a websocket handshake being the one seen. */
  private static boolean upgrade(Context ctx) {
    final var upgrade = ctx.header("Upgrade");
    return upgrade != null && !upgrade.toLowerCase(Locale.ROOT).startsWith("h2");
  }

  private void forward(Context ctx) throws IOException {
    final HttpResponse<InputStream> response;
    try {
      response = client.send(request(ctx), HttpResponse.BodyHandlers.ofInputStream());
    } catch (ConnectException e) {
      respond(ctx, 502, "The application is not accepting connections", "");
      return;
    } catch (IOException e) {
      log.log("forwarding failed: %s", e.getMessage());
      respond(ctx, 502, "Forwarding to the application failed", e.getMessage());
      return;
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      respond(ctx, 502, "Forwarding to the application was interrupted", "");
      return;
    }
    final var headers = ctx.responseHeaders();
    response
        .headers()
        .map()
        .forEach(
            (name, values) -> {
              if (!HOP_BY_HOP.contains(name.toLowerCase(Locale.ROOT))) {
                headers.put(name, new ArrayList<>(values));
              }
            });
    final var length = response.headers().firstValueAsLong("content-length").orElse(-1);
    final var status = response.statusCode();
    final var exchange = ctx.exchange();
    if (length == 0 || bodyless(status) || "HEAD".equalsIgnoreCase(ctx.method())) {
      exchange.sendResponseHeaders(status, -1);
      response.body().close();
      return;
    }
    // zero means the length is unknown and the response is chunked as it is written
    exchange.sendResponseHeaders(status, Math.max(length, 0));
    try (var in = response.body();
        var out = exchange.getResponseBody()) {
      copy(in, out);
    }
  }

  private HttpRequest request(Context ctx) {
    final var uri = URI.create(target + path(ctx.exchange()));
    final var builder =
        HttpRequest.newBuilder(uri)
            .version(HttpClient.Version.HTTP_1_1)
            .method(ctx.method(), body(ctx));
    ctx.requestHeaders()
        .forEach(
            (name, values) -> {
              if (!HOP_BY_HOP.contains(name.toLowerCase(Locale.ROOT))) {
                values.forEach(value -> builder.header(name, value));
              }
            });
    forwardedBy(ctx, builder);
    return builder.build();
  }

  /** The application sees our loopback host, so pass on where the request actually came from. */
  private static void forwardedBy(Context ctx, HttpRequest.Builder builder) {
    final var host = ctx.host();
    if (host != null) {
      builder.header("X-Forwarded-Host", host);
    }
    builder.header("X-Forwarded-Proto", "http");
    builder.header("X-Forwarded-For", ctx.ip());
  }

  /** Keep the path and query exactly as they arrived, resolving would reinterpret them. */
  private static String path(HttpExchange exchange) {
    final var requested = exchange.getRequestURI();
    final var path = requested.getRawPath() == null ? "/" : requested.getRawPath();
    final var query = requested.getRawQuery();
    return query == null ? path : path + "?" + query;
  }

  /** Only send a body when the request has one, a chunked GET is not what the client sent us. */
  private static HttpRequest.BodyPublisher body(Context ctx) {
    if (ctx.header("Transfer-Encoding") != null || length(ctx.header("Content-Length")) > 0) {
      return HttpRequest.BodyPublishers.ofInputStream(ctx::bodyAsInputStream);
    }
    return HttpRequest.BodyPublishers.noBody();
  }

  private static long length(String value) {
    try {
      return value == null ? 0 : Long.parseLong(value.trim());
    } catch (NumberFormatException e) {
      return 0;
    }
  }

  /** These responses carry no body, sending one hangs the exchange. */
  private static boolean bodyless(int status) {
    return status == 204 || status == 304 || (status >= 100 && status < 200);
  }

  /**
   * Flush as we go so a streamed response like server sent events arrives as the application writes
   * it rather than when its buffer happens to fill.
   */
  private static void copy(InputStream in, OutputStream out) throws IOException {
    final var buffer = new byte[COPY_BUFFER];
    for (int read = in.read(buffer); read != -1; read = in.read(buffer)) {
      out.write(buffer, 0, read);
      out.flush();
    }
  }

  private static void respond(Context ctx, int status, String title, String detail) {
    final var html = wantsHtml(ctx);
    final var text = html ? page(status, title, detail) : plain(title, detail);
    final var body = text.getBytes(StandardCharsets.UTF_8);
    ctx.contentType((html ? "text/html" : "text/plain") + "; charset=utf-8");
    ctx.status(status);
    ctx.write(body, body.length);
  }

  private static boolean wantsHtml(Context ctx) {
    final var accept = ctx.header("Accept");
    return accept != null && accept.toLowerCase(Locale.ROOT).contains("text/html");
  }

  private static String plain(String title, String detail) {
    return (detail.isEmpty() ? title : title + " - " + detail) + System.lineSeparator();
  }

  /** Plain enough to read in either theme and small enough to keep in one string. */
  private static String page(int status, String title, String detail) {
    return PAGE.formatted(status, escape(title), status, escape(title), escape(detail));
  }

  private static String escape(String value) {
    return value
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;");
  }
}
