package io.avaje.tools.devtool.state;

import io.avaje.jsonb.JsonType;
import io.avaje.jsonb.Jsonb;
import io.avaje.tools.devtool.data.KBaseMeta;
import io.avaje.tools.devtool.data.TaskMeta;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.util.List;
import java.util.stream.Stream;

final class TaskLoader {

  private final JsonType<TaskMeta> metaJsonType;
  private final JsonType<KBaseMeta> kBaseMetaJsonType;
  private final File dataDirectory;
  private String kbaseId = "";
  private String groupId = "";

  TaskLoader(Jsonb jsonb, File dataDirectory) {
    this.metaJsonType = jsonb.type(TaskMeta.class);
    this.kBaseMetaJsonType = jsonb.type(KBaseMeta.class);
    this.dataDirectory = dataDirectory;
  }

  static List<TaskGroup> load(Jsonb jsonb, String path) {
    return load(jsonb, new File(path));
  }

  static List<TaskGroup> load(Jsonb jsonb, File dataDirectory) {
    return new TaskLoader(jsonb, dataDirectory).load();
  }

  private List<TaskGroup> load() {
    if (!dataDirectory.exists()) {
      return List.of();
    }
    File rootBaseFile = new File(dataDirectory, "kbase.json");
    if (rootBaseFile.exists()) {
      KBaseMeta kBase = readKbase(dataDirectory);
      kbaseId = isBlank(kBase.id()) ? dataDirectory.getName() : kBase.id().trim();
    }
    return fileStream(dataDirectory)
      .filter(File::isDirectory)
      .map(this::loadKBase)
      .toList();
  }

  private TaskGroup loadKBase(File kbaseDir) {
    KBaseMeta meta = readKbase(kbaseDir);
    setCurrentGroupId(meta.id(), kbaseDir.getName());
    List<Task> list = fileStream(kbaseDir)
      .filter(File::isDirectory)
      .map(this::loadTask)
      .toList();

    return new TaskGroup(meta, list);
  }

  private void setCurrentGroupId(String id, String groupDirName) {
    groupId = isBlank(id) ? groupDirName : id.trim();
  }

  private static boolean isBlank(String value) {
    return value == null || value.isBlank();
  }

  private Task loadTask(File taskDir) {
    TaskMeta taskMeta = readTaskMeta(taskDir);
    String uniqueTaskId = uniqueTaskId(taskDir.getName());
    String previewContent = readPreview(taskDir);
    return new Task(uniqueTaskId, taskMeta, previewContent, taskMeta.allSearchKeywords(), taskMeta.displayName().toLowerCase());
  }

  private String uniqueTaskId(String taskDir) {
    return
      trimLower(kbaseId) + '$' +
      trimLower(groupId) + '$' +
      trimLower(taskDir);
  }

  private static String trimLower(String id) {
    return id == null ? "" : id.trim().toLowerCase();
  }

  private Stream<File> fileStream(File dir) {
    File[] files = dir.listFiles();
    return files != null ? Stream.of(files) : Stream.of();
  }

  private String readPreview(File taskDir) {
    var file = new File(taskDir, "preview.html");
    if (!file.exists()) {
      return "";
    }
    try {
      return Files.readString(file.toPath());
    } catch (IOException e) {
      return "";
    }
  }

  private TaskMeta readTaskMeta(File taskDir) {
    try (var is = new FileInputStream(new File(taskDir, "$meta.json"))) {
      return metaJsonType.fromJson(is);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private KBaseMeta readKbase(File kbaseDir) {
    try (var is = new FileInputStream(new File(kbaseDir, "kbase.json"))) {
      return kBaseMetaJsonType.fromJson(is);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
