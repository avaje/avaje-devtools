package io.avaje.dev;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;

class DevMojoTest {

  @Test
  void configurationMapsOntoTheLauncherOptions() {
    var options =
        DevMojo.toOptions(
            "org.example.Main",
            List.of("--port", "8080"),
            List.of("-Xmx64m"),
            List.of(Path.of("target/classes"), Path.of("src/main/resources")),
            List.of(),
            "",
            null,
            true,
            250L,
            0,
            0,
            "server.port",
            "a.jar:b.jar");

    assertThat(options.mainClass()).isEqualTo("org.example.Main");
    assertThat(options.appArgs()).containsExactly("--port", "8080");
    assertThat(options.jvmArgs()).containsExactly("-Xmx64m");
    assertThat(options.watchDirs())
        .containsExactly(Path.of("target/classes"), Path.of("src/main/resources"));
    assertThat(options.buildCommand()).isEmpty();
    assertThat(options.quietPeriod()).isEqualTo(Duration.ofMillis(250));
    assertThat(options.classpath()).isEqualTo("a.jar:b.jar");
    assertThat(options.proxied()).isFalse();
  }

  @Test
  void portsMapOntoTheProxyOptions() {
    var options =
        DevMojo.toOptions(
            "org.example.Main",
            List.of(),
            List.of(),
            List.of(Path.of("src/main/java")),
            List.of(),
            "",
            null,
            true,
            300L,
            8080,
            9000,
            "app.port",
            "a.jar");

    assertThat(options.port()).isEqualTo(8080);
    assertThat(options.appPort()).isEqualTo(9000);
    assertThat(options.portProperty()).isEqualTo("app.port");
    assertThat(options.proxied()).isTrue();
  }

  @Test
  void theApplicationPortDefaultsToTheOneAboveTheProxy() {
    var options =
        DevMojo.toOptions(
            "org.example.Main",
            List.of(),
            List.of(),
            List.of(Path.of("src/main/java")),
            List.of(),
            "",
            null,
            true,
            300L,
            8080,
            0,
            "server.port",
            "a.jar");

    assertThat(options.appPort()).isEqualTo(8081);
  }

  @Test
  void buildCommandIsSplit() {
    var options =
        DevMojo.toOptions(
            "org.example.Main",
            List.of(),
            List.of(),
            List.of(Path.of("src/main/java")),
            List.of(),
            "mvn -q compile",
            Path.of("target"),
            true,
            300L,
            0,
            0,
            "server.port",
            "a.jar");

    assertThat(options.buildCommand()).containsExactly("mvn", "-q", "compile");
    assertThat(options.buildDir()).isEqualTo(Path.of("target"));
  }

  @Test
  void withoutABuildThereIsNoDirectoryToRunItIn() {
    var options =
        DevMojo.toOptions(
            "org.example.Main",
            List.of(),
            List.of(),
            List.of(Path.of("target/classes")),
            List.of(),
            "",
            null,
            true,
            300L,
            0,
            0,
            "server.port",
            "a.jar");

    assertThat(options.buildDir()).isNull();
  }

  @Test
  void quietPeriodDefaultsWhenNotConfigured() {
    var options =
        DevMojo.toOptions(
            "org.example.Main",
            List.of(),
            List.of(),
            List.of(Path.of("target/classes")),
            List.of(),
            "",
            null,
            true,
            null,
            8080,
            0,
            "server.port",
            "a.jar");

    assertThat(options.quietPeriod()).isEqualTo(Duration.ofMillis(300));
    assertThat(options.eagerRestart()).isFalse();
  }

  @Test
  void anExplicitQuietPeriodWithAPortRestartsEagerly() {
    var options =
        DevMojo.toOptions(
            "org.example.Main",
            List.of(),
            List.of(),
            List.of(Path.of("target/classes")),
            List.of(),
            "",
            null,
            true,
            250L,
            8080,
            0,
            "server.port",
            "a.jar");

    assertThat(options.quietPeriod()).isEqualTo(Duration.ofMillis(250));
    assertThat(options.eagerRestart()).isTrue();
  }

  @Test
  void applicationArgsThatLookLikeOptionsAreNotParsedAsOurs() {
    var options =
        DevMojo.toOptions(
            "org.example.Main",
            List.of("--watch", "--build", "--help"),
            List.of(),
            List.of(Path.of("target/classes")),
            List.of(),
            "",
            null,
            true,
            300L,
            0,
            0,
            "server.port",
            "a.jar");

    assertThat(options.appArgs()).containsExactly("--watch", "--build", "--help");
    assertThat(options.watchDirs()).containsExactly(Path.of("target/classes"));
    assertThat(options.buildCommand()).isEmpty();
  }
}
