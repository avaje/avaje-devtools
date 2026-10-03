package io.avaje.dev;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Path;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class DevOptionsTest {

  @Test
  void mainClassAndAppArgs() {
    var options =
        DevOptions.parse(new String[] {"--watch", "src", "org.example.Main", "--port", "8080"});

    assertThat(options.mainClass()).isEqualTo("org.example.Main");
    assertThat(options.appArgs()).containsExactly("--port", "8080");
    assertThat(options.watchDirs()).containsExactly(Path.of("src"));
    assertThat(options.buildCommand()).isEmpty();
    assertThat(options.quietPeriod()).isEqualTo(Duration.ofMillis(300));
    assertThat(options.classpath()).isEqualTo(System.getProperty("java.class.path"));
  }

  @Test
  void optionsAfterMainClassBelongToTheApplication() {
    var options = DevOptions.parse(new String[] {"--watch", "src", "org.example.Main", "--watch"});

    assertThat(options.appArgs()).containsExactly("--watch");
    assertThat(options.watchDirs()).containsExactly(Path.of("src"));
  }

  @Test
  void repeatedOptions() {
    var options =
        DevOptions.parse(
            new String[] {
              "--watch",
              "src",
              "--watch",
              "target",
              "--jvm-arg",
              "-Xmx64m",
              "--jvm-arg",
              "-Dfoo=bar",
              "--classpath",
              "a.jar:b.jar",
              "--quiet-period",
              "50",
              "org.example.Main"
            });

    assertThat(options.watchDirs()).containsExactly(Path.of("src"), Path.of("target"));
    assertThat(options.jvmArgs()).containsExactly("-Xmx64m", "-Dfoo=bar");
    assertThat(options.classpath()).isEqualTo("a.jar:b.jar");
    assertThat(options.quietPeriod()).isEqualTo(Duration.ofMillis(50));
  }

  @Test
  void buildCommandSplitOnWhitespace() {
    var options =
        DevOptions.parse(
            new String[] {"--watch", "src", "--build", "mvn -q  compile", "org.example.Main"});

    assertThat(options.buildCommand()).containsExactly("mvn", "-q", "compile");
  }

  @Test
  void buildCommandKeepsQuotedSections() {
    var options =
        DevOptions.parse(
            new String[] {
              "--watch", "src", "--build", "mvn -Dfoo=\"a b\" compile", "org.example.Main"
            });

    assertThat(options.buildCommand()).containsExactly("mvn", "-Dfoo=a b", "compile");
  }

  @Test
  void dashDashEndsOptions() {
    var options =
        DevOptions.parse(new String[] {"--watch", "src", "--", "org.example.Main", "--build"});

    assertThat(options.mainClass()).isEqualTo("org.example.Main");
    assertThat(options.appArgs()).containsExactly("--build");
  }

  @Test
  void watchDirsDefaultToClassesWhenThereIsNoBuild() {
    var options = DevOptions.parse(new String[] {"org.example.Main"});

    assertThat(options.watchDirs()).contains(Path.of("target/classes"));
    assertThat(options.watchDirs()).doesNotContain(Path.of("src/main/java"));
  }

  @Test
  void watchDirsDefaultToSourcesWhenBuilding() {
    var options = DevOptions.parse(new String[] {"--build", "mvn -q compile", "org.example.Main"});

    assertThat(options.watchDirs()).contains(Path.of("src/main/java"));
    assertThat(options.watchDirs()).doesNotContain(Path.of("target/classes"));
  }

  @Test
  void missingMainClass() {
    assertThatThrownBy(() -> DevOptions.parse(new String[] {"--watch", "src"}))
        .isInstanceOf(DevOptions.InvalidOptions.class)
        .hasMessage("No main class given");
  }

  @Test
  void theBuildRunsInTheGivenDirectory() {
    var options =
        DevOptions.parse(
            new String[] {
              "--watch",
              "src",
              "--build",
              "mvn -q compile",
              "--build-dir",
              "target",
              "org.example.Main"
            });

    assertThat(options.buildDir()).isEqualTo(Path.of("target"));
  }

  @Test
  void theBuildDirectoryDefaultsToTheCurrentOne() {
    var options =
        DevOptions.parse(new String[] {"--watch", "src", "--build", "mvn -q compile", "x"});

    assertThat(options.buildDir()).isNull();
  }

  @Test
  void aBuildDirectoryWithoutABuildIsRejected() {
    assertThatThrownBy(
            () -> DevOptions.parse(new String[] {"--watch", "src", "--build-dir", "target", "x"}))
        .hasMessageContaining("--build-dir is only used with --build");
  }

  @Test
  void aBuildDirectoryThatIsNotADirectoryIsRejected() {
    assertThatThrownBy(
            () -> DevOptions.parse(new String[] {"--watch", "src", "--build-dir", "pom.xml", "x"}))
        .hasMessageContaining("--build-dir is not a directory");
  }

  @Test
  void theChildJvmStartsQuicklyUnlessToldOtherwise() {
    assertThat(DevOptions.parse(new String[] {"--watch", "src", "x"}).quickStart()).isTrue();
    assertThat(
            DevOptions.parse(new String[] {"--watch", "src", "--no-quick-start", "x"}).quickStart())
        .isFalse();
  }

  @Test
  void unknownOption() {
    assertThatThrownBy(() -> DevOptions.parse(new String[] {"--nope", "org.example.Main"}))
        .isInstanceOf(DevOptions.InvalidOptions.class)
        .hasMessage("Unknown option --nope");
  }

  @Test
  void missingOptionValue() {
    assertThatThrownBy(() -> DevOptions.parse(new String[] {"--watch"}))
        .isInstanceOf(DevOptions.InvalidOptions.class)
        .hasMessage("--watch requires a value");
  }

  @Test
  void quietPeriodMustBeANumber() {
    assertThatThrownBy(
            () -> DevOptions.parse(new String[] {"--quiet-period", "soon", "org.example.Main"}))
        .isInstanceOf(DevOptions.InvalidOptions.class)
        .hasMessageContaining("soon");
  }

  @Test
  void portsDefaultToNoProxy() {
    var options = DevOptions.parse(new String[] {"--watch", "src", "org.example.Main"});

    assertThat(options.port()).isZero();
    assertThat(options.appPort()).isZero();
    assertThat(options.proxied()).isFalse();
  }

  @Test
  void aPortDefaultsTheApplicationToTheOneAboveIt() {
    var options =
        DevOptions.parse(new String[] {"--watch", "src", "--port", "8080", "org.example.Main"});

    assertThat(options.port()).isEqualTo(8080);
    assertThat(options.appPort()).isEqualTo(8081);
    assertThat(options.proxied()).isTrue();
  }

  @Test
  void thePortPropertyDefaultsToTheOneAvajeConfigReads() {
    var options =
        DevOptions.parse(new String[] {"--watch", "src", "--port", "8080", "org.example.Main"});

    assertThat(options.portProperty()).isEqualTo("server.port");
  }

  @Test
  void thePortPropertyCanBeGiven() {
    var options =
        DevOptions.parse(
            new String[] {
              "--watch", "src", "--port", "8080", "--port-property", "http.port", "org.example.Main"
            });

    assertThat(options.portProperty()).isEqualTo("http.port");
  }

  @Test
  void theApplicationPortCanBeGiven() {
    var options =
        DevOptions.parse(
            new String[] {
              "--watch", "src", "--port", "80", "--app-port", "9000", "org.example.Main"
            });

    assertThat(options.appPort()).isEqualTo(9000);
  }

  @Test
  void aPortAloneRestartsOnTheNextRequest() {
    var options =
        DevOptions.parse(new String[] {"--watch", "src", "--port", "8080", "org.example.Main"});

    assertThat(options.eagerRestart()).isFalse();
  }

  @Test
  void aPortWithAnExplicitQuietPeriodRestartsEagerly() {
    var options =
        DevOptions.parse(
            new String[] {
              "--watch", "src", "--port", "8080", "--quiet-period", "50", "org.example.Main"
            });

    assertThat(options.eagerRestart()).isTrue();
  }

  @Test
  void anExplicitQuietPeriodWithoutAPortIsNotEager() {
    var options =
        DevOptions.parse(
            new String[] {"--watch", "src", "--quiet-period", "50", "org.example.Main"});

    assertThat(options.eagerRestart()).isFalse();
  }

  @Test
  void portsThatCannotWork() {
    assertThatThrownBy(
            () -> DevOptions.parse(new String[] {"--watch", "src", "--port", "http", "x"}))
        .hasMessageContaining("--port expects a port number");
    assertThatThrownBy(
            () -> DevOptions.parse(new String[] {"--watch", "src", "--port", "99999", "x"}))
        .hasMessageContaining("between 1 and 65535");
    assertThatThrownBy(
            () ->
                DevOptions.parse(
                    new String[] {"--watch", "src", "--port", "80", "--app-port", "80", "x"}))
        .hasMessageContaining("--app-port must differ");
    assertThatThrownBy(
            () -> DevOptions.parse(new String[] {"--watch", "src", "--app-port", "80", "x"}))
        .hasMessageContaining("--app-port is only used with --port");
  }
}
