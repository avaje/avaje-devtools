package io.avaje.dev;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class RestartLoopTest {

  /** Reports the given number of changes and is then "closed". */
  static final class Changes implements ChangeSource {
    private final AtomicInteger remaining;
    int drained;

    Changes(int changes) {
      this.remaining = new AtomicInteger(changes);
    }

    @Override
    public boolean awaitChange() {
      return remaining.getAndDecrement() > 0;
    }

    @Override
    public void drain() {
      drained++;
    }
  }

  static final class Launcher implements AppLauncher {
    final List<String> events = new ArrayList<>();
    private int started;

    @Override
    public AppHandle start() {
      final var id = ++started;
      events.add("start" + id);
      return () -> events.add("stop" + id);
    }
  }

  @Test
  void restartsOnEachChange() {
    var launcher = new Launcher();

    new RestartLoop(launcher, () -> true, message -> {}).run(new Changes(2));

    assertThat(launcher.events)
        .containsExactly("start1", "stop1", "start2", "stop2", "start3", "stop3");
  }

  @Test
  void startsAndStopsOnceWhenNothingChanges() {
    var launcher = new Launcher();

    new RestartLoop(launcher, () -> true, message -> {}).run(new Changes(0));

    assertThat(launcher.events).containsExactly("start1", "stop1");
  }

  @Test
  void failedBuildLeavesTheApplicationRunning() {
    var launcher = new Launcher();
    var changes = new Changes(2);

    new RestartLoop(launcher, () -> false, message -> {}).run(changes);

    assertThat(launcher.events).containsExactly("start1", "stop1");
    assertThat(changes.drained).isEqualTo(2);
  }

  @Test
  void buildRunsBeforeEachRestart() {
    var launcher = new Launcher();
    var builds = new AtomicInteger();

    new RestartLoop(launcher, () -> builds.incrementAndGet() > 0, message -> {})
        .run(new Changes(3));

    assertThat(builds).hasValue(3);
    assertThat(launcher.events).hasSize(8);
  }

  @Test
  void changesAreDrainedBeforeRestartingSoTheBuildDoesNotTriggerItself() {
    var launcher = new Launcher();
    var changes = new Changes(1);

    new RestartLoop(launcher, () -> true, message -> {}).run(changes);

    assertThat(changes.drained).isEqualTo(1);
  }

  @Test
  void stopEndsTheApplicationAndTheNextChangeDoesNotStartIt() {
    var launcher = new Launcher();
    var loop = new RestartLoop(launcher, () -> true, message -> {});
    var changes =
        new ChangeSource() {
          @Override
          public boolean awaitChange() {
            loop.stop();
            return true;
          }

          @Override
          public void drain() {}
        };

    loop.run(changes);

    assertThat(launcher.events).containsExactly("start1", "stop1");
  }
}
