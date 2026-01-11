package io.ebean.tools.init.watch;

import java.nio.file.Path;

/**
 * Provides support for skipping directory structures.
 */
public interface WatchDirSkip {

  /**
   * Return true if this path should be skipped/ignored.
   */
  boolean skip(Path path);
}
