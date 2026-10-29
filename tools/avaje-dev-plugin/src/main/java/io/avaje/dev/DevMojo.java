package io.avaje.dev;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

/**
 * Runs the application in a child JVM and restarts it when files change.
 *
 * <p>The project is compiled before the application starts, and the goal blocks until it is
 * interrupted.
 */
@Mojo(name = "dev", requiresDependencyResolution = ResolutionScope.RUNTIME, threadSafe = true)
@Execute(phase = LifecyclePhase.PROCESS_CLASSES)
public final class DevMojo extends AbstractMojo {

  /** The application main class. */
  @Parameter(property = "dev.mainClass")
  private String mainClass;

  /** Arguments passed to the application main. */
  @Parameter(property = "dev.args")
  private List<String> args;

  /** Extra arguments for the child JVM, for example {@code -Xmx256m}. */
  @Parameter(property = "dev.jvmArgs")
  private List<String> jvmArgs;

  /**
   * Directories watched for changes.
   *
   * <p>Defaults to the build output directory and the resource directories, or to the source
   * directories when a build command is configured. The same directories of the modules this one
   * depends on are watched as well when they are part of the same build.
   */
  @Parameter(property = "dev.watch")
  private List<File> watch;

  /**
   * Command run before each restart, for example {@code mvn -q compile}.
   *
   * <p>Without one the application is restarted whenever the build output changes, so compiling
   * from an IDE or another terminal drives the restart. The running application is left in place
   * when the build fails.
   */
  @Parameter(property = "dev.build")
  private String build;

  /**
   * Directory the build command runs in.
   *
   * <p>Defaults to the project directory, or to the root of a multi module build when this module
   * depends on other modules of it, so that a plain {@code mvn -q compile} rebuilds what the
   * application actually runs on. The goal itself runs from wherever maven was invoked, which for a
   * module built with {@code -pl} is neither of those.
   */
  @Parameter(property = "dev.buildDir")
  private File buildDir;

  /**
   * Port served by the plugin, forwarding to the application.
   *
   * <p>With one set the application is only rebuilt and restarted on the first request after a
   * change, so editing without hitting the application rebuilds nothing. The application itself
   * listens on {@code appPort}.
   */
  @Parameter(property = "dev.port")
  private int port;

  /**
   * Port the application listens on when {@code port} is set, defaults to the port above it.
   *
   * <p>It is passed to the child JVM as {@code -Dserver.port}, which avaje-config reads.
   */
  @Parameter(property = "dev.appPort")
  private int appPort;

  /**
   * System property the application port is passed to the child JVM as.
   *
   * <p>Defaults to {@code server.port}, which is what avaje-config reads. Give the property your
   * framework reads when it is not that one.
   */
  @Parameter(property = "dev.portProperty", defaultValue = "server.port")
  private String portProperty;

  /**
   * Start the child JVM with the flags that favour starting over running long.
   *
   * <p>The compiler stops at the first tier and the serial collector is used, unless the jvmArgs
   * name a collector themselves. Turn it off to run the application on the JVM it would normally
   * get, for example to measure something that only the full compiler shows.
   */
  @Parameter(property = "dev.quickStart", defaultValue = "true")
  private boolean quickStart;

  /**
   * Time in milliseconds that changes must settle before restarting, defaults to 300.
   *
   * <p>Given explicitly together with {@code port}, the application restarts as soon as changes
   * settle rather than waiting for the next request, which is what a {@code port} without one
   * otherwise does.
   */
  @Parameter(property = "dev.quietPeriod")
  private Long quietPeriod;

  /** Skip the goal. */
  @Parameter(property = "dev.skip", defaultValue = "false")
  private boolean skip;

  /**
   * Skip tests run by the build command.
   *
   * <p>Appended as {@code -DskipTests} to the build command, so a command that would otherwise run
   * tests, for example {@code mvn install}, skips them without editing the command itself.
   */
  @Parameter(property = "dev.skipTests", defaultValue = "false")
  private boolean skipTests;

  @Parameter(defaultValue = "${project}", readonly = true, required = true)
  private MavenProject project;

  @Parameter(defaultValue = "${session}", readonly = true, required = true)
  private MavenSession session;

  @Override
  public void execute() throws MojoExecutionException {
    if (skip) {
      getLog().info("avaje dev skipped");
      return;
    }
    final var log = (DevLog) message -> getLog().info(message);
    var buildCommand = build == null || build.isBlank() ? "" : build;
    final var building = !buildCommand.isEmpty();
    if (building && skipTests) {
      buildCommand = buildCommand + " -DskipTests";
    }
    if (buildDir != null && !building) {
      throw new MojoExecutionException("The buildDir parameter is only used with a build command");
    }
    final var reactorDependencies = reactorDependencies();
    final var watchDirs = watchDirs(building, reactorDependencies);
    if (watchDirs.isEmpty()) {
      throw new MojoExecutionException(
          "Nothing to watch, configure the watch parameter with a directory");
    }
    final var main = mainClass();
    final DevOptions options;
    try {
      options =
          toOptions(
              main,
              nonNull(args),
              nonNull(jvmArgs),
              watchDirs,
              watchFiles(reactorDependencies),
              buildCommand,
              building ? logged(buildDir(reactorDependencies)) : null,
              quickStart,
              quietPeriod,
              port,
              appPort,
              portProperty,
              classpath());
    } catch (DevOptions.InvalidOptions e) {
      throw new MojoExecutionException(e.getMessage(), e);
    }
    DevRun.run(options, log);
  }

  /** Log the directory the build runs in, it is not always the one the goal was run from. */
  private Path logged(Path dir) {
    getLog().info("building in " + dir);
    return dir;
  }

  /** The configured main class. */
  private String mainClass() throws MojoExecutionException {
    if (mainClass != null && !mainClass.isBlank()) {
      return mainClass.trim();
    }
    final var found =
        MainClassLookup.find(project)
            .orElseThrow(
                () ->
                    new MojoExecutionException(
                        "No main class found in the project, configure the mainClass parameter"));
    getLog().info("using main class " + found);
    return found;
  }

  /** Map the configuration onto the launcher command line so both entry points agree. */
  static DevOptions toOptions(
      String mainClass,
      List<String> args,
      List<String> jvmArgs,
      List<Path> watchDirs,
      List<Path> watchFiles,
      String buildCommand,
      Path buildDir,
      boolean quickStart,
      Long quietPeriod,
      int port,
      int appPort,
      String portProperty,
      String classpath) {
    final var command = new ArrayList<String>();
    command.add("--classpath");
    command.add(classpath);
    if (quietPeriod != null) {
      command.add("--quiet-period");
      command.add(String.valueOf(quietPeriod));
    }
    if (!quickStart) {
      command.add("--no-quick-start");
    }
    if (port > 0) {
      command.add("--port");
      command.add(String.valueOf(port));
      if (appPort > 0) {
        command.add("--app-port");
        command.add(String.valueOf(appPort));
      }
      if (portProperty != null && !portProperty.isBlank()) {
        command.add("--port-property");
        command.add(portProperty);
      }
    }
    if (!buildCommand.isEmpty()) {
      command.add("--build");
      command.add(buildCommand);
      if (buildDir != null) {
        command.add("--build-dir");
        command.add(buildDir.toString());
      }
    }
    for (var dir : watchDirs) {
      command.add("--watch");
      command.add(dir.toString());
    }
    for (var file : watchFiles) {
      command.add("--watch-file");
      command.add(file.toString());
    }
    for (var jvmArg : jvmArgs) {
      command.add("--jvm-arg");
      command.add(jvmArg);
    }
    command.add("--");
    command.add(mainClass);
    command.addAll(args);
    return DevOptions.parse(command.toArray(String[]::new));
  }

  /**
   * Watch the sources when a build runs, otherwise watch the build output. Keeping the two apart
   * means a build never triggers the restart that follows it.
   */
  List<Path> watchDirs(boolean building, List<MavenProject> reactorDependencies) {
    if (watch != null && !watch.isEmpty()) {
      return watch.stream().map(File::toPath).toList();
    }
    final var dirs = new LinkedHashSet<Path>();
    final var projects = new ArrayList<MavenProject>();
    projects.add(project);
    projects.addAll(reactorDependencies);
    for (var each : projects) {
      if (building) {
        each.getCompileSourceRoots().forEach(root -> dirs.add(Path.of(root)));
      } else {
        dirs.add(Path.of(each.getBuild().getOutputDirectory()));
      }
      each.getBuild()
          .getResources()
          .forEach(resource -> dirs.add(Path.of(resource.getDirectory())));
    }
    return dirs.stream().filter(dir -> dir.toFile().isDirectory()).toList();
  }

  /** Watch build files of this project, of the modules it depends on and of the parents of both. */
  List<Path> watchFiles(List<MavenProject> reactorDependencies) {
    final var files = new LinkedHashSet<Path>();
    final var projects = new ArrayList<MavenProject>();
    projects.add(project);
    projects.addAll(reactorDependencies);
    for (var each : projects) {
      for (var ancestor = each; ancestor != null; ancestor = ancestor.getParent()) {
        final var pom = ancestor.getFile();
        if (pom != null && pom.isFile()) {
          files.add(pom.toPath());
        }
      }
    }
    return List.copyOf(files);
  }

  /**
   * The modules of this build the project depends on, empty for a project built on its own.
   *
   * <p>They are matched by coordinates rather than by resolved file, so a module resolved to an
   * installed jar rather than to its output directory is still recognised as one of ours.
   */
  List<MavenProject> reactorDependencies() {
    if (session == null || session.getProjects() == null) {
      return List.of();
    }
    final var dependencies = new HashSet<String>();
    for (var artifact : project.getArtifacts()) {
      dependencies.add(artifact.getGroupId() + ":" + artifact.getArtifactId());
    }
    return session.getProjects().stream()
        .filter(each -> dependencies.contains(each.getGroupId() + ":" + each.getArtifactId()))
        .toList();
  }

  /**
   * The directory the build command runs in, the project directory unless it depends on modules
   * built alongside it, in which case the build has to run from the root to reach them.
   */
  Path buildDir(List<MavenProject> reactorDependencies) {
    if (buildDir != null) {
      return buildDir.toPath();
    }
    if (!reactorDependencies.isEmpty()) {
      final var root = session.getTopLevelProject();
      if (root != null && root.getBasedir() != null) {
        return root.getBasedir().toPath();
      }
    }
    return project.getBasedir().toPath();
  }

  private String classpath() throws MojoExecutionException {
    try {
      return String.join(File.pathSeparator, project.getRuntimeClasspathElements());
    } catch (Exception e) {
      throw new MojoExecutionException("Failed to resolve the runtime classpath", e);
    }
  }

  private static List<String> nonNull(List<String> values) {
    return values == null ? List.of() : values;
  }
}
