package io.avaje.tools.devtool.service;

import io.avaje.inject.Component;
import io.avaje.inject.PostConstruct;
import io.avaje.jsonb.JsonType;
import io.avaje.jsonb.Jsonb;
import io.avaje.tools.devtool.data.Data;
import io.avaje.tools.devtool.data.MDataSource;
import io.avaje.tools.devtool.data.MProjects;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static java.lang.System.Logger.Level.ERROR;
import static java.lang.System.Logger.Level.INFO;

@Component
public class ProjectsRepository {

  private static final System.Logger log = System.getLogger("app");

  private final List<String> errors = new ArrayList<>();
  private final Jsonb jsonb;
  private final JsonType<MProjects> projectsJsonType;
  private MProjects mProjects = new MProjects();
  private final Data data = new Data(new ArrayList<>());
  private File projectsFile;

  public ProjectsRepository(Jsonb jsonb) {
    this.jsonb = jsonb;
    this.projectsJsonType = jsonb.type(MProjects.class);
  }

  public Data data() {
    return data;
  }

  public MProjects projects() {
    return mProjects;
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
      try {
        String asJson = Files.readString(projectsFile.toPath());
        var xp = projectsJsonType.fromJson(asJson);
        mProjects = xp;
        // mProjects = projectsJsonType.fromJson(new FileReader(projectsFile));
        for (MDataSource mDataSource : mProjects.dataSources()) {
          loadDataSource(mDataSource);
        }
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
  }

  boolean save() {
    if (projectsFile != null) {
      try {
        String jsonPretty = projectsJsonType.toJsonPretty(mProjects);
        Files.writeString(projectsFile.toPath(), jsonPretty);
        log.log(INFO, "Saved projects to " + projectsFile.getAbsolutePath());
        return true;
      } catch (IOException e) {
        log.log(ERROR, "Error saving projects to " + projectsFile.getAbsolutePath(), e);
        return false;
      }
    }
    return false;
  }

  private void loadDataSource(MDataSource mDataSource) {
    Data load = DataLoader.load(jsonb, mDataSource.path());
    data.kbases().addAll(load.kbases());
  }

  private String evalRootDir(String rootDir) {
    String userHome = System.getProperty("user.home");
    return userHome + rootDir.substring(1);
  }

  public Optional<Data> addSource(String path) {
    File file = new File(path);
    if (file.exists()) {
      Data load = DataLoader.load(jsonb, file);
      var newSource = new MDataSource(path, "directory", path);
      projects().dataSources().add(newSource);
      data.kbases().addAll(load.kbases());
      save();
      return Optional.of(data);
    }
    return Optional.empty();
  }
}
