package io.avaje.dev;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Parsed command line for the dev launcher.
 *
 * @param mainClass the application main class run in the child JVM
 * @param appArgs arguments passed to the application main
 * @param jvmArgs extra arguments passed to the child JVM
 * @param classpath the application classpath used by the child JVM, not the launcher's own
 * @param watchDirs directories watched for changes
 * @param watchFiles single files watched for changes, the build files rather than a tree
 * @param buildCommand command run before each restart, empty when no build is configured
 * @param buildDir directory the build command runs in, null to inherit the current one
 * @param quickStart start the child JVM with the flags that favour starting over running long
 * @param quietPeriod time to wait for changes to stop arriving before restarting
 * @param quietPeriodGiven whether {@code --quiet-period} was given rather than defaulted, an
 *     explicit one opts a proxied port into restarting eagerly, see {@link #eagerRestart()}
 * @param port port the dev proxy listens on, 0 when no proxy is used
 * @param appPort port the application listens on, only used with a proxy
 * @param portProperty system property the application port is passed to the child JVM as
 */
record DevOptions(
    String mainClass,
    List<String> appArgs,
    List<String> jvmArgs,
    String classpath,
    List<Path> watchDirs,
    List<Path> watchFiles,
    List<String> buildCommand,
    Path buildDir,
    boolean quickStart,
    Duration quietPeriod,
    boolean quietPeriodGiven,
    int port,
    int appPort,
    String portProperty) {

  static final String DEFAULT_PORT_PROPERTY = "server.port";

  private static final List<String> BUILD_WATCH_DIRS =
      List.of("src/main/java", "src/main/resources");
  private static final List<String> CLASS_WATCH_DIRS =
      List.of("target/classes", "src/main/resources");

  static final String USAGE =
      """
      Usage: java -cp <launcher classpath> io.avaje.dev.DevRun [options] <mainClass> [appArgs...]

      <launcher classpath> is for DevRun itself, this jar and the dependencies it runs with.
      It is unrelated to the application, which gets its own classpath via --classpath below.

      Runs <mainClass> in a child JVM and restarts it when watched files change.

      Options:
        --watch <dir>        directory to watch, repeatable
                             (default: target/classes and src/main/resources, or
                              src/main/java and src/main/resources when --build is set)
        --watch-file <file>  single file to watch, repeatable, for the build files rather
                             than a tree
        --build <command>    command run before each restart, e.g. --build "mvn -q compile"
                             the application is left running when the build fails
        --build-dir <dir>    directory --build runs in (default: the current directory)
        --no-quick-start     start the child JVM as it would normally start, rather than with
                             the flags that favour starting over running long
        --jvm-arg <arg>      argument for the child JVM, repeatable
        --classpath <cp>     classpath the application runs on in the child JVM (default: the
                             launcher classpath above, which only works when the two happen
                             to be the same)
        --quiet-period <ms>  time changes must settle before restarting (default: 300). Given
                             together with --port, the application restarts as changes settle
                             rather than waiting for the next request
        --port <port>        serve on this port, restarting the application on the first
                             request that follows a change rather than as it arrives, unless
                             --quiet-period is given explicitly, see above
        --app-port <port>    port the application listens on behind --port (default: port + 1)
        --port-property <p>  system property the application port is given to the child JVM as
                             (default: server.port)
        --help               print this message
      """;

  /** Thrown when the command line cannot be used, the message is meant for the console. */
  static final class InvalidOptions extends RuntimeException {
    private static final long serialVersionUID = 1L;

    InvalidOptions(String message) {
      super(message);
    }
  }

  static DevOptions parse(String[] args) {
    var appArgs = new ArrayList<String>();
    var jvmArgs = new ArrayList<String>();
    var watchDirs = new ArrayList<Path>();
    var watchFiles = new ArrayList<Path>();
    var buildCommand = new ArrayList<String>();
    Path buildDir = null;
    var quickStart = true;
    var classpath = System.getProperty("java.class.path");
    var quietPeriod = Duration.ofMillis(300);
    var quietPeriodGiven = false;
    var port = 0;
    var appPort = 0;
    var portProperty = DEFAULT_PORT_PROPERTY;
    String mainClass = null;

    for (int i = 0; i < args.length; i++) {
      final var arg = args[i];
      if (mainClass != null) {
        // everything after the main class belongs to the application
        appArgs.add(arg);
        continue;
      }
      switch (arg) {
        case "--watch" -> watchDirs.add(Path.of(value(args, ++i, arg)));
        case "--watch-file" -> watchFiles.add(Path.of(value(args, ++i, arg)));
        case "--build" -> buildCommand.addAll(splitCommand(value(args, ++i, arg)));
        case "--build-dir" -> buildDir = directory(value(args, ++i, arg));
        case "--no-quick-start" -> quickStart = false;
        case "--jvm-arg" -> jvmArgs.add(value(args, ++i, arg));
        case "--classpath", "-cp" -> classpath = value(args, ++i, arg);
        case "--quiet-period" -> {
          quietPeriod = Duration.ofMillis(millis(value(args, ++i, arg)));
          quietPeriodGiven = true;
        }
        case "--port" -> port = port(value(args, ++i, arg), arg);
        case "--app-port" -> appPort = port(value(args, ++i, arg), arg);
        case "--port-property" -> portProperty = portProperty(value(args, ++i, arg));
        case "--" -> {
          // explicit end of options, the next argument is the main class
        }
        default -> {
          if (arg.startsWith("-")) {
            throw new InvalidOptions("Unknown option " + arg);
          }
          mainClass = arg;
        }
      }
    }
    if (mainClass == null) {
      throw new InvalidOptions("No main class given");
    }
    if (buildDir != null && buildCommand.isEmpty()) {
      throw new InvalidOptions("--build-dir is only used with --build");
    }
    if (watchDirs.isEmpty()) {
      watchDirs.addAll(defaultWatchDirs(!buildCommand.isEmpty()));
      if (watchDirs.isEmpty()) {
        throw new InvalidOptions(
            "None of the default watch directories exist, use --watch to name one");
      }
    }
    return new DevOptions(
        mainClass,
        List.copyOf(appArgs),
        List.copyOf(jvmArgs),
        classpath,
        List.copyOf(watchDirs),
        List.copyOf(watchFiles),
        List.copyOf(buildCommand),
        buildDir,
        quickStart,
        quietPeriod,
        quietPeriodGiven,
        port,
        appPort(port, appPort),
        portProperty);
  }

  /** True when a proxy serves the port and the restart waits for the next request. */
  boolean proxied() {
    return port > 0;
  }

  /**
   * True when a proxied application restarts as soon as changes settle rather than waiting for the
   * next request, opted into by giving {@code --quiet-period} explicitly alongside a port.
   */
  boolean eagerRestart() {
    return proxied() && quietPeriodGiven;
  }

  /** Default the application port to the one above the proxy, they must not collide. */
  private static int appPort(int port, int appPort) {
    if (port == 0) {
      if (appPort != 0) {
        throw new InvalidOptions("--app-port is only used with --port");
      }
      return 0;
    }
    if (appPort == 0) {
      return port == 65535 ? port - 1 : port + 1;
    }
    if (appPort == port) {
      throw new InvalidOptions("--app-port must differ from --port");
    }
    return appPort;
  }

  /** Watch sources when a build runs, otherwise watch the compiled output. */
  private static List<Path> defaultWatchDirs(boolean building) {
    return (building ? BUILD_WATCH_DIRS : CLASS_WATCH_DIRS)
        .stream().map(Path::of).filter(Files::isDirectory).toList();
  }

  /** The directory a build runs in, which has to exist for the build to run at all. */
  private static Path directory(String value) {
    final var dir = Path.of(value);
    if (!Files.isDirectory(dir)) {
      throw new InvalidOptions("--build-dir is not a directory - " + value);
    }
    return dir;
  }

  /** Split on whitespace, honouring single and double quoted sections. */
  private static List<String> splitCommand(String command) {
    var parts = new ArrayList<String>();
    var current = new StringBuilder();
    char quote = 0;
    boolean quoted = false;
    for (int i = 0; i < command.length(); i++) {
      final char ch = command.charAt(i);
      if (quote != 0) {
        if (ch == quote) {
          quote = 0;
        } else {
          current.append(ch);
        }
      } else if (ch == '"' || ch == '\'') {
        quote = ch;
        quoted = true;
      } else if (Character.isWhitespace(ch)) {
        if (quoted || current.length() > 0) {
          parts.add(current.toString());
          current.setLength(0);
          quoted = false;
        }
      } else {
        current.append(ch);
      }
    }
    if (quoted || current.length() > 0) {
      parts.add(current.toString());
    }
    if (parts.isEmpty()) {
      throw new InvalidOptions("--build was given an empty command");
    }
    return parts;
  }

  private static long millis(String value) {
    try {
      return Long.parseLong(value);
    } catch (NumberFormatException e) {
      throw new InvalidOptions("--quiet-period expects milliseconds but got " + value);
    }
  }

  private static String portProperty(String value) {
    if (value.isBlank()) {
      throw new InvalidOptions("--port-property requires a system property name");
    }
    return value;
  }

  private static int port(String value, String option) {
    final int port;
    try {
      port = Integer.parseInt(value);
    } catch (NumberFormatException e) {
      throw new InvalidOptions(option + " expects a port number but got " + value);
    }
    if (port < 1 || port > 65535) {
      throw new InvalidOptions(option + " expects a port between 1 and 65535 but got " + value);
    }
    return port;
  }

  private static String value(String[] args, int index, String option) {
    if (index >= args.length) {
      throw new InvalidOptions(option + " requires a value");
    }
    return args[index];
  }
}
