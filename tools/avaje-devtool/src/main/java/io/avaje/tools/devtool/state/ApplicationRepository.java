package io.avaje.tools.devtool.state;

import io.avaje.inject.Component;
import io.avaje.inject.PostConstruct;
import io.avaje.jsonb.JsonType;
import io.avaje.jsonb.Jsonb;
import io.avaje.tools.devtool.data.*;
import io.avaje.tools.devtool.service.ModelProjectMaven;
import io.avaje.tools.devtool.service.ProjectFileSearch;

import java.io.*;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import static java.lang.System.Logger.Level.ERROR;
import static java.lang.System.Logger.Level.INFO;

@Component
public class ApplicationRepository {

  private static final System.Logger log = System.getLogger("app");

  private final List<String> errors = new ArrayList<>();
  private final Jsonb jsonb;
  private final JsonType<ApplicationModel> applicationModelJson;
  private final ApplicationState state = new ApplicationState();
  private File stateFile;

  public ApplicationRepository(Jsonb jsonb) {
    this.jsonb = jsonb;
    this.applicationModelJson = jsonb.type(ApplicationModel.class);
  }

  /**
   * Return the current application state.
   */
  public ApplicationState state() {
    return state;
  }

  public void refreshData(ApplicationState data) {
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

    stateFile = new File(file, "state.json");
    if (stateFile.exists()) {
      try (var reader = new FileReader(stateFile)) {
        var loadedModel = applicationModelJson.fromJson(reader);
        initialiseApplicationState(loadedModel);
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }
    }
  }

  private void initialiseApplicationState(ApplicationModel loadedModel) {

    var projects = loadedModel.projects().stream()
        .peek(MProject::initialiseSearchText)
        .distinct()
        .sorted()
        .toList();

    var dataSources = loadedModel.dataSources().stream()
      .distinct()
      .sorted()
      .toList();

    var projectSources = loadedModel.projectSources().stream()
      .distinct()
      .sorted()
      .toList();

    List<Task> tasks = loadedModel.dataSources().stream()
      .flatMap(s -> loadDataSource(s).stream())
      .flatMap(k -> k.tasks().stream())
      .distinct()
      .sorted()
      .toList();

    state.init(projects, dataSources, projectSources, tasks);
  }

  void saveProjectsFile() {
    if (stateFile != null) {
      try {
        var applicationModel = state.toApplicationModel();
        String jsonPretty = applicationModelJson.toJsonPretty(applicationModel);
        Files.writeString(stateFile.toPath(), jsonPretty);
        log.log(INFO, "Saved projects to {0}", stateFile.getAbsolutePath());

      } catch (IOException e) {
        log.log(ERROR, "Error saving projects to " + stateFile.getAbsolutePath(), e);
      }
    }
  }

  private List<TaskGroup> loadDataSource(KBaseSource mDataSource) {
    return TaskLoader.load(jsonb, mDataSource.path());
  }

  private String evalRootDir(String rootDir) {
    String userHome = System.getProperty("user.home");
    return userHome + rootDir.substring(1);
  }

  public long addTasksSource(String path) {
    File dir = new File(path);
    if (!dir.exists()) {
      return 0;
    }

    var newSource = new KBaseSource(path, "directory", path);
    List<TaskGroup> loadedGroups = TaskLoader.load(jsonb, dir);
    var tasks = loadedGroups.stream()
      .flatMap(kb -> kb.tasks().stream())
      .toList();

    state.addTasks(tasks);
    state.addDataSource(newSource);

    saveProjectsFile();
    return tasks.size();
  }

  /**
   * Add the scanned (found) projects (maven) to the repository.
   *
   * @param lastProjectScan The scanned projects
   */
  public void addScannedProjects(ProjectFileSearch lastProjectScan) {
    var newProjectSource = new ProjectsSource(lastProjectScan.path(), "directory", lastProjectScan.path());

    List<MProject> list = lastProjectScan.all()
      .map(ApplicationRepository::mapToMProject)
      .toList();

    state.addProjectSource(newProjectSource);
    state.addProjects(list);

    saveProjectsFile();
  }

  private static MProject mapToMProject(ModelProjectMaven loadedMavenPom) {
    var p = new MProject();
    p.setPath(loadedMavenPom.projectFile().getAbsolutePath());
    p.setName(loadedMavenPom.name());
    p.setType(MProject.Type.MAVEN);
    p.setGroupId(loadedMavenPom.groupId());
    p.setArtifactId(loadedMavenPom.artifactId());
    p.setDescription(loadedMavenPom.description());
    p.initialiseSearchText();
    return p;
  }
}
