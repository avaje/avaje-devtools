package io.avaje.tools.devtool.service;

import io.avaje.inject.Component;
import io.avaje.inject.PostConstruct;
import io.avaje.jsonb.JsonType;
import io.avaje.jsonb.Jsonb;
import io.avaje.tools.devtool.data.*;

import java.io.*;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import static java.lang.System.Logger.Level.ERROR;
import static java.lang.System.Logger.Level.INFO;

@Component
public class ProjectsRepository {

  private static final System.Logger log = System.getLogger("app");

  private final List<String> errors = new ArrayList<>();
  private final Jsonb jsonb;
  private final JsonType<MProjects> projectsJsonType;
  private MProjects mProjects = new MProjects();
  private File projectsFile;
  private Data data = new Data(List.of(), List.of(), List.of());

  public ProjectsRepository(Jsonb jsonb) {
    this.jsonb = jsonb;
    this.projectsJsonType = jsonb.type(MProjects.class);
  }

  public Data data() {
    return data;
  }

  public void refreshData(Data data) {
    // TODO refresh logic
  }

  @PostConstruct
  void initialise() {
    var rootDir = System.getProperty("projects.dir", "~/.avaje-devtool");
    if (rootDir.startsWith("~/")) {
      rootDir = evalRootDir(rootDir);
    }
    File file = new File(rootDir);
    if (!file.exists() && !file.mkdirs()) {
      errors.add("Failed to create directory at " + file.getAbsolutePath());
    }

    projectsFile = new File(file, "projects.json");
    if (projectsFile.exists()) {
      try (var reader = new FileReader(projectsFile)) {
        mProjects = projectsJsonType.fromJson(reader);

        List<KBase> kbases = new ArrayList<>();
        for (MDataSource mDataSource : mProjects.dataSources()) {
          kbases.addAll(loadDataSource(mDataSource));
        }
        data = initialiseRepoData(mProjects, kbases);
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }
    }
  }

  private Data initialiseRepoData(MProjects mProjects, List<KBase> kbases) {

    var projects = mProjects.projects().stream()
        .peek(MProject::initialiseSearchText)
        .sorted()
        .toList();

    var sources = mProjects.dataSources().stream()
      .sorted()
      .toList();

    var tasks = kbases.stream()
      .flatMap(k -> k.tasks().stream())
      .sorted()
      .toList();

    return new Data(projects, tasks, sources);
  }

  void saveProjectsFile() {
    if (projectsFile != null) {
      try {
        String jsonPretty = projectsJsonType.toJsonPretty(mProjects);
        Files.writeString(projectsFile.toPath(), jsonPretty);
        log.log(INFO, "Saved projects to " + projectsFile.getAbsolutePath());

      } catch (IOException e) {
        log.log(ERROR, "Error saving projects to " + projectsFile.getAbsolutePath(), e);
      }
    }
  }

  private List<KBase> loadDataSource(MDataSource mDataSource) {
    return DataLoader.load(jsonb, mDataSource.path());
  }

  private String evalRootDir(String rootDir) {
    String userHome = System.getProperty("user.home");
    return userHome + rootDir.substring(1);
  }

  public long addSource(String path) {
    File file = new File(path);
    if (!file.exists()) {
      return 0;
    }

    List<KBase> load = DataLoader.load(jsonb, file);
    var newSource = new MDataSource(path, "directory", path);

    var newTasks = load.stream()
      .flatMap(kb -> kb.tasks().stream())
      .toList();

    var sourceCopy = new ArrayList<>(data.sources());
    sourceCopy.add(newSource);

    var tasksCopy = new ArrayList<>(data.tasks());
    tasksCopy.addAll(newTasks);

    data = new Data(data().projects(), tasksCopy, sourceCopy);

    mProjects.dataSources().add(newSource);

    saveProjectsFile();
    return newTasks.size();
  }
}
