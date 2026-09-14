package io.avaje.dev;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** End to end, a real child JVM served through the proxy and restarted by a real file change. */
class DevRunProxyTest {

  private static final Duration READY_TIMEOUT = Duration.ofSeconds(30);

  @Test
  void theRequestAfterAChangeIsServedByTheRestartedApplication(@TempDir Path dir) throws Exception {
    final var watched = Files.createDirectory(dir.resolve("classes"));
    final var body = dir.resolve("body.txt");
    Files.writeString(body, "one");
    final var appPort = freePort();

    final var options =
        new DevOptions(
            TestServerApp.class.getName(),
            List.of(body.toString()),
            List.of(),
            "target/classes" + File.pathSeparator + "target/test-classes",
            List.of(watched),
            List.of(),
            List.of(),
            null,
            false,
            Duration.ofMillis(50),
            false,
            appPort + 1,
            appPort,
            DevOptions.DEFAULT_PORT_PROPERTY);

    final var watcher = new DirWatcher(options.watchDirs(), options.quietPeriod());
    final var restart =
        new DeferredRestart(
            AppProcess.launcher(options, message -> {}),
            () -> true,
            PortReady.of(appPort, READY_TIMEOUT, message -> {}),
            message -> {});
    final var proxy = DevProxy.start(0, appPort, restart, message -> {});
    restart.start();
    final var watching = CompletableFuture.runAsync(() -> restart.watch(watcher));
    try (var client = HttpClient.newHttpClient()) {

      assertThat(get(client, proxy)).isEqualTo("one");

      // the application already read the file, only a restart picks the new content up
      Files.writeString(body, "two");
      assertThat(get(client, proxy)).isEqualTo("one");

      Files.writeString(watched.resolve("App.class"), "changed");
      awaitStale(restart);

      assertThat(get(client, proxy)).isEqualTo("two");
    } finally {
      proxy.close();
      restart.stop();
      watcher.close();
      watching.join();
    }
  }

  private static String get(HttpClient client, DevProxy proxy) throws Exception {
    final var request =
        HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + proxy.port() + "/")).build();
    return client.send(request, HttpResponse.BodyHandlers.ofString()).body();
  }

  /** The watcher runs on its own thread, give it the change before asking for the restart. */
  private static void awaitStale(DeferredRestart restart) throws Exception {
    for (int i = 0; i < 600; i++) {
      if (restart.stale()) {
        return;
      }
      Thread.sleep(50);
    }
    throw new AssertionError("the change was never seen");
  }

  private static int freePort() throws IOException {
    try (var socket = new ServerSocket(0)) {
      return socket.getLocalPort();
    }
  }
}
