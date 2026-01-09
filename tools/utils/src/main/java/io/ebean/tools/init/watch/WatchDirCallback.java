package io.ebean.tools.init.watch;

import java.nio.file.Path;
import java.nio.file.WatchEvent;

/**
 * Callback when a file event occurs.
 */
public interface WatchDirCallback {

  /**
   * Process the file event.
   */
  void event(WatchEvent<Path> event, Path child, String eventKind);
}
