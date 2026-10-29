# avaje-dev-maven-plugin

[![Build](https://github.com/avaje/avaje-dev-plugin/actions/workflows/build.yml/badge.svg)](https://github.com/avaje/avaje-dev-plugin/actions/workflows/build.yml)
[![Maven Central](https://img.shields.io/maven-central/v/io.avaje/avaje-dev-maven-plugin.svg?label=Maven%20Central)](https://central.sonatype.com/artifact/io.avaje/avaje-dev-maven-plugin)

Development mode for any java application. Runs your main class in a child JVM and restarts it when
files change, so an edit is live without leaving the terminal.

## Use

```xml
<plugin>
  <groupId>io.avaje</groupId>
  <artifactId>avaje-dev-maven-plugin</artifactId>
  <version>${avaje.dev.version}</version>
</plugin>
```

```
mvn avaje-dev:dev
```

The project is compiled first, then the application starts and the goal blocks until you stop it.

## The main class

Nothing above names the main class because the project usually names it already. It is read from
the manifest the jar, shade or assembly plugin writes, from the `mainClass` of the exec or spring
boot plugin, or from the `exec.mainClass`, `start-class`, `mainClass` or `main.class` property.
The one it settles on is logged as it starts.

Name it yourself when the project names none, or names one you would rather not run in dev mode.

```xml
<configuration>
  <mainClass>org.example.Main</mainClass>
</configuration>
```

## Watching

By default it watches the build output and the resource directories, so whatever compiles your
code (the IDE, or `mvn compile` in another terminal) drives the restart.

To have it run custom goals on restart, or add other configuration, give it a build command.

```xml
<configuration>
  <build>mvn -q compile</build>
</configuration>
```

When a build fails the running application is left in place.

The build runs in the project directory rather than wherever maven was invoked, so a module built
with `-pl` from the root still builds itself. The exception is a multi module build where the
module depends on others of it, covered below: there it runs from the root instead. Set `buildDir`
to run it somewhere else.

## Multi module

The modules of the build this one depends on are watched as well, along with their build files, so
editing a module the application uses restarts it as an edit of the application itself does.
Nothing needs configuring for that.

A build command in that case runs from the root of the build rather than from the module, since a
build run in the module would never rebuild the modules it depends on. Plain `mvn -q compile` is
therefore enough, and `-pl :app -am` narrows it to what the application needs.

```xml
<configuration>
  <build>mvn -q -pl :app -am compile</build>
</configuration>
```

Run the goal with the module in the build for any of this to apply, for example
`mvn -pl :app -am avaje-dev:dev` from the root. Run from the module directory alone and the
modules it depends on are the jars maven resolved, which is what building the module by itself
means.

## Serving a port

Given a port, the plugin serves it and forwards to the application, which listens on the port.
A change then costs nothing until the next request: the request that follows an edit rebuilds,
restarts, and is answered by the new application.

```xml
<configuration>
  <build>mvn -q compile</build>
  <port>8080</port>
</configuration>
```

The application port is passed to the child JVM as `-Dserver.port`, which is what avaje-config reads
and so what `AvajeJex.start()` and `Nima.start()` listen on. Set `portProperty` when your framework
reads a different property, and `appPort` when it decides its port some other way entirely. The
plugin only needs to know which port to forward to.

A request arriving while the build fails is answered with a 500 and the console has the compiler
output. The build is not attempted again until something changes, so a broken source file is
reported rather than recompiled per request.

Without a port the application owns the port itself and restarts as changes arrive.

Give `quietPeriod` explicitly alongside `port` and the application restarts as soon as a change
settles instead of waiting for the next request. A request arriving during that restart gets a 503
rather than the stale application, same as one arriving before the application first starts.

```xml
<configuration>
  <build>mvn -q compile</build>
  <port>8080</port>
  <quietPeriod>300</quietPeriod>
</configuration>
```

Requests are forwarded as HTTP/1.1, including streamed responses such as server sent events.

## Configuration

| parameter      | property           | description                                                      |
|----------------|--------------------|------------------------------------------------------------------|
| `mainClass`    | `dev.mainClass`    | the application main class, read from the project when unset     |
| `args`         | `dev.args`         | arguments passed to the application main                         |
| `jvmArgs`      | `dev.jvmArgs`      | extra arguments for the child JVM                                |
| `watch`        | `dev.watch`        | directories to watch, defaults as described above                |
| `build`        | `dev.build`        | command run before each restart                                  |
| `skipTests`    | `dev.skipTests`    | append `-DskipTests` to the build command, default off            |
| `buildDir`     | `dev.buildDir`     | directory the build runs in, the project or multi module root    |
| `port`         | `dev.port`         | port served by the plugin, restart happens on the next request   |
| `appPort`      | `dev.appPort`      | port the application binds behind `port`, defaults to `port` + 1 |
| `portProperty` | `dev.portProperty` | system property the application port is given as, `server.port`  |
| `quietPeriod`  | `dev.quietPeriod`  | milliseconds changes must settle before restarting, default 300. Given explicitly with `port`, restarts eagerly instead of on the next request |
| `quickStart`   | `dev.quickStart`   | configure the child JVM for starting rather than running, default on |
| `skip`         | `dev.skip`         | skip the goal                                                    |

```
mvn avaje-dev:dev -Ddev.build="mvn -q compile" -Ddev.port=8080
```
