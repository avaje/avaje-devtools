package io.avaje.dev;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;

class AppProcessTest {

  private static DevOptions options(boolean quickStart, String... jvmArgs) {
    return new DevOptions(
        "org.example.Main",
        List.of(),
        List.of(jvmArgs),
        "a.jar",
        List.of(Path.of("target/classes")),
        List.of(),
        List.of(),
        null,
        quickStart,
        Duration.ofMillis(300),
        false,
        0,
        0,
        DevOptions.DEFAULT_PORT_PROPERTY);
  }

  @Test
  void theQuickStartFlagsFavourStartingOverRunningLong() {
    assertThat(AppProcess.startArgs(options(true)))
        .containsExactly("-XX:TieredStopAtLevel=1", "-XX:+UseSerialGC");
  }

  @Test
  void quickStartIsTurnedOff() {
    assertThat(AppProcess.startArgs(options(false))).isEmpty();
  }

  @Test
  void aCollectorOfTheirOwnIsLeftAlone() {
    // naming a second collector fails the JVM outright rather than overriding the first
    assertThat(AppProcess.startArgs(options(true, "-XX:+UseG1GC")))
        .containsExactly("-XX:TieredStopAtLevel=1");
    assertThat(AppProcess.startArgs(options(true, "-XX:-UseSerialGC")))
        .containsExactly("-XX:TieredStopAtLevel=1");
  }
}
