package io.avaje.dev;

/** Source of file change notifications driving the restart loop. */
interface ChangeSource {

  /**
   * Block until a change has been seen and no further change arrives for the quiet period.
   *
   * @return true when a change was seen, false when the source is closed
   */
  boolean awaitChange() throws InterruptedException;

  /** Discard any changes seen so far, used so a build does not trigger the next restart. */
  void drain();
}
