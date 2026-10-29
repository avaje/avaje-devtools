package io.avaje.dev;

/** Starts the application, each start returns a handle used to stop that instance. */
interface AppLauncher {

  AppHandle start();

  /** A running application instance. */
  interface AppHandle {

    /** Stop the instance, returning once it has exited. */
    void stop();
  }
}
