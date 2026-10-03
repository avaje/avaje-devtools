package io.avaje.dev;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class DeferredRestartTest {

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

  private static DeferredRestart restart(Launcher launcher) {
    return new DeferredRestart(launcher, () -> true, () -> true, message -> {});
  }

  @Test
  void withoutAChangeNothingIsRebuilt() {
    var launcher = new Launcher();
    var builds = new AtomicInteger();
    var restart =
        new DeferredRestart(
            launcher, () -> builds.incrementAndGet() > 0, () -> true, message -> {});

    restart.start();

    assertThat(restart.awaitCurrent()).isEqualTo(DeferredRestart.Result.READY);
    assertThat(restart.awaitCurrent()).isEqualTo(DeferredRestart.Result.READY);
    assertThat(builds).hasValue(0);
    assertThat(launcher.events).containsExactly("start1");
  }

  @Test
  void aChangeIsPickedUpByTheNextRequestOnly() {
    var launcher = new Launcher();
    var restart = restart(launcher);
    restart.start();

    restart.markStale();

    assertThat(launcher.events).containsExactly("start1");
    assertThat(restart.awaitCurrent()).isEqualTo(DeferredRestart.Result.READY);
    assertThat(launcher.events).containsExactly("start1", "stop1", "start2");
  }

  @Test
  void severalChangesCostOneRestart() {
    var launcher = new Launcher();
    var builds = new AtomicInteger();
    var restart =
        new DeferredRestart(
            launcher, () -> builds.incrementAndGet() > 0, () -> true, message -> {});
    restart.start();

    restart.markStale();
    restart.markStale();
    restart.awaitCurrent();
    restart.awaitCurrent();

    assertThat(builds).hasValue(1);
    assertThat(launcher.events).containsExactly("start1", "stop1", "start2");
  }

  @Test
  void aFailedBuildLeavesTheApplicationRunningAndIsNotRetriedUntilTheNextChange() {
    var launcher = new Launcher();
    var builds = new AtomicInteger();
    var restart =
        new DeferredRestart(
            launcher, () -> builds.incrementAndGet() < 0, () -> true, message -> {});
    restart.start();

    restart.markStale();

    assertThat(restart.awaitCurrent()).isEqualTo(DeferredRestart.Result.BUILD_FAILED);
    assertThat(restart.awaitCurrent()).isEqualTo(DeferredRestart.Result.BUILD_FAILED);
    assertThat(builds).hasValue(1);
    assertThat(launcher.events).containsExactly("start1");
  }

  @Test
  void anApplicationThatNeverListensIsReportedRatherThanForwardedTo() {
    var launcher = new Launcher();
    var restart = new DeferredRestart(launcher, () -> true, () -> false, message -> {});
    restart.start();

    assertThat(restart.awaitCurrent()).isEqualTo(DeferredRestart.Result.NOT_READY);
  }

  @Test
  void readinessIsCheckedOncePerStart() {
    var launcher = new Launcher();
    var checks = new AtomicInteger();
    var restart =
        new DeferredRestart(
            launcher, () -> true, () -> checks.incrementAndGet() > 0, message -> {});
    restart.start();

    restart.awaitCurrent();
    restart.awaitCurrent();
    restart.markStale();
    restart.awaitCurrent();

    assertThat(checks).hasValue(2);
  }

  @Test
  void aChangeArrivingDuringTheBuildIsLeftForTheNextRequest() {
    var launcher = new Launcher();
    var builds = new AtomicInteger();
    var deferred = new DeferredRestart[1];
    var editedAgain = new AtomicBoolean(true);
    deferred[0] =
        new DeferredRestart(
            launcher,
            () -> {
              builds.incrementAndGet();
              if (editedAgain.getAndSet(false)) {
                // the file was saved again while this build was running
                deferred[0].markStale();
              }
              return true;
            },
            () -> true,
            message -> {});
    deferred[0].start();
    deferred[0].markStale();

    deferred[0].awaitCurrent();
    deferred[0].awaitCurrent();

    assertThat(builds).hasValue(2);
    assertThat(launcher.events).containsExactly("start1", "stop1", "start2", "stop2", "start3");
  }

  @Test
  void stoppingEndsTheApplicationAndRequestsAreTurnedAway() {
    var launcher = new Launcher();
    var deferred = restart(launcher);
    deferred.start();

    deferred.stop();

    assertThat(deferred.awaitCurrent()).isEqualTo(DeferredRestart.Result.STOPPED);
    assertThat(launcher.events).containsExactly("start1", "stop1");
  }
}
