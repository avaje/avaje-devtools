package io.avaje.tools.devtool.web.view;

import io.avaje.tools.devtool.data.MDataSource;
import io.avaje.tools.devtool.data.MProject;
import io.avaje.tools.devtool.data.Task;
import io.jstach.jstache.JStache;

import java.util.List;

public class Partial {

  @JStache(path = "partial/initial-empty")
  public record InitialEmpty() {
  }
  @JStache(path = "partial/initial-tasks")
  public record InitialTasks(Task first, List<Task> tasks) {
  }

  @JStache(path = "partial/search-tasks")
  public record SearchTasks(Task first, List<Task> tasks) {
  }

  @JStache(path = "partial/projects")
  public record Projects(List<MProject> projects) {
  }

  @JStache(path = "partial/tasks")
  public record Tasks(Task first, List<Task> tasks) {
  }

  @JStache(path = "partial/sources")
  public record Sources(List<MDataSource> sources) {
  }

  @JStache(path = "partial/sidebar")
  public static class Sidebar {
  }

  @JStache(path = "partial/toast")
  public static record Toast(String message) {
  }
}
