package io.avaje.tools.devtool.web.view;

import io.avaje.tools.devtool.data.KBaseSource;
import io.avaje.tools.devtool.data.MProject;
import io.avaje.tools.devtool.data.ProjectsSource;
import io.avaje.tools.devtool.state.AddTasksResult;
import io.avaje.tools.devtool.state.Task;
import io.avaje.tools.devtool.service.ModelProjectMaven;
import io.jstach.jstache.JStache;

import java.util.Collection;
import java.util.List;

public class Partial {

  @JStache(path = "partial/initial-empty")
  public record InitialEmpty() {
  }
  @JStache(path = "partial/initial-tasks")
  public record InitialTasks(Task first, List<Task> tasks, TaskAction action) {
  }

  @JStache(path = "partial/search-tasks")
  public record SearchTasks(Task first, List<Task> tasks, TaskAction action) {
  }

  public record TaskAction(String id){}

  @JStache(path = "partial/search-projects")
  public record SearchProjects(List<MProject> projects) {
  }

  @JStache(path = "partial/search-task-sources")
  public record SearchTaskSources(List<KBaseSource> taskSources) {
  }

  @JStache(path = "partial/search-project-sources")
  public record SearchProjectSources(List<ProjectsSource> projectSources) {
  }

  @JStache(path = "partial/projects")
  public record Projects(Collection<MProject> projects) {
  }

  @JStache(path = "partial/tasks")
  public record Tasks(Task first, Collection<Task> tasks, TaskAction action) {
  }

  @JStache(path = "partial/sources")
  public record Sources(Collection<KBaseSource> taskSources, Collection<ProjectsSource> projectSources) {
  }

  @JStache(path = "partial/sidebar")
  public static class Sidebar {
  }

  @JStache(path = "partial/toast")
  public record Toast(String message) {
  }

  @JStache(path = "partial/view-new-project-form")
  public static class ViewNewProjectForm {
  }

  @JStache(path = "partial/view-new-tasks-form")
  public static class ViewNewTasksForm {
  }

  @JStache(path = "partial/view-projects")
  public static class ViewProjects {
  }

  @JStache(path = "partial/scan-projects-result")
  public record ScanProjectsResult(
    List<ModelProjectMaven> projects,
    long allCount,
    int directoriesSearched) {
  }

  @JStache(path = "partial/add-task-source-result")
  public record AddTaskSourceResult(AddTasksResult result) {
  }

  @JStache(path = "partial/run-task-result")
  public record RunTaskResult(List<String> output) {
  }
}
