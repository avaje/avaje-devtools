package io.avaje.tools.devtool.web;

import io.avaje.htmx.api.Html;
import io.avaje.htmx.api.HxRequest;
import io.avaje.http.api.*;
import io.avaje.jex.http.Context;
import io.avaje.tools.devtool.service.DataService;
import io.avaje.tools.devtool.service.ModelProjectMaven;
import io.avaje.tools.devtool.service.ProjectFileSearch;
import io.avaje.tools.devtool.state.AddTasksResult;
import io.avaje.tools.devtool.state.ApplicationState;
import io.avaje.tools.devtool.state.Task;
import io.avaje.tools.devtool.web.view.Page;
import io.avaje.tools.devtool.web.view.Partial;

import java.util.List;

@Html
@Controller
final class IndexController {

  private final DataService dataService;

  IndexController(DataService dataService) {
    this.dataService = dataService;
  }

  @Get
  Page.Index home() {
    ModelProjectMaven localPom = dataService.workingDirectoryPom();
    var context = localPom == null ? null : new Page.WorkingContext(localPom.projectFile().getAbsolutePath(), localPom.gav());

    ApplicationState data = dataService.data();
    return new Page.Index(data.tasks().size(), data.tasks().size(), context);
  }

  @HxRequest
  @Get("initial")
  Object initial() {
    var sources = dataService.data().dataSources();
    if (sources.isEmpty()) {
      return new Partial.InitialEmpty();
    }
    return tasks();
  }

  @HxRequest
  @Form
  @Post("search")
  Partial.SearchTasks searchTasks(String search) {
    List<Task> tasks = dataService.searchTasks(search, 10);
    if (tasks.isEmpty()) {
      return new Partial.SearchTasks(null, tasks, null);
    }
    var first = tasks.getFirst();
    if (!first.isAction() || !dataService.hasCurrentProject()) {
      return new Partial.SearchTasks(first, tasks, null);
    }
    var action = new Partial.TaskAction(first.uniqueTaskId());
    return new Partial.SearchTasks(first, tasks, action);
  }

  @HxRequest
  @Form
  @Post("searchProjects")
  Partial.SearchProjects searchProjects(String search) {
    var projects = dataService.searchProjects(search, 10);
    return new Partial.SearchProjects(projects);
  }

  @HxRequest
  @Form
  @Post("searchTaskSources")
  Partial.SearchTaskSources searchTaskSources(String search) {
    var sources = dataService.searchTaskSources(search, 10);
    return new Partial.SearchTaskSources(sources);
  }

  @HxRequest
  @Form
  @Post("searchProjectSources")
  Partial.SearchProjectSources searchProjectSources(String search) {
    var sources = dataService.searchProjectSources(search, 10);
    return new Partial.SearchProjectSources(sources);
  }

  @HxRequest
  @Get("projects")
  Partial.Projects projects() {
    var projects = dataService.data().projects();
    return new Partial.Projects(projects);
  }

  @HxRequest
  @Get("tasks")
  Partial.Tasks tasks() {
    var tasks = dataService.data().tasks();
    var first = !tasks.isEmpty() ? tasks.getFirst() : null;
    return new Partial.Tasks(first, tasks, null);
  }

  @HxRequest
  @Get("sources")
  Partial.Sources sources() {
    var taskSources = dataService.data().dataSources();
    var projectsSources = dataService.data().projectSources();
    return new Partial.Sources(taskSources, projectsSources);
  }

  @HxRequest
  @Get("toggle/sidebar")
  Partial.Sidebar toggleSidebar() {
    return new Partial.Sidebar();
  }

  @HxRequest
  @Form
  @Post("addSource")
  void addSource(String path, Context context) {
    var result = dataService.addSource(path);
    context.html("<p>Added source with " + result.addedCount() + " new tasks</p>");
  }

  @Produces(MediaType.TEXT_HTML)
  @Post("upload")
  void upload(Context ctx) {
    String body = ctx.body();
    System.out.println("body = " + body);
    ctx.html("upload");
  }

  @Produces(MediaType.TEXT_HTML)
  @Post("upload2")
  void upload2(Context ctx) {
    String body = ctx.body();
    System.out.println("body = " + body);
    ctx.html("upload2");
  }

  @HxRequest
  @Get("viewNewProjectForm")
  Partial.ViewNewProjectForm viewNewProjectForm() {
    return new Partial.ViewNewProjectForm();
  }

  @HxRequest
  @Get("viewNewTasksForm")
  Partial.ViewNewTasksForm viewNewTasksForm() {
    return new Partial.ViewNewTasksForm();
  }

  @HxRequest
  @Get("viewProjects")
  Partial.ViewProjects viewProjects() {
    return new Partial.ViewProjects();
  }

  @HxRequest
  @Form
  @Post("scanPathForProjects")
  Partial.ScanProjectsResult scanPathForProjects(String path) {
    ProjectFileSearch scan = dataService.scanPathForProjects(path);
    List<ModelProjectMaven> projects = scan.topLevel()
      .stream()
      .sorted()
      .toList();

    int directoriesSearched = scan.totalDirectoriesSearched();
    long allCount = scan.all().count();

    return new Partial.ScanProjectsResult(projects, allCount, directoriesSearched);
  }

  @HxRequest
  @Form
  @Post("addTaskSource")
  Partial.AddTaskSourceResult addTaskSource(String path) {
    AddTasksResult result = dataService.addTaskSource(path);
    return new Partial.AddTaskSourceResult(result);
  }

  @HxRequest
  @Form
  @Post("addScannedProjects")
  void addScannedProjects(Context context) {
    dataService.addScannedProjects();
    context.html("<p>Scanned projects added</p>");
  }

  @HxRequest
  @Post("task/run/{taskId}")
  Partial.RunTaskResult taskRun(String taskId) {
    List<String> output = dataService.taskRun(taskId);
    return new Partial.RunTaskResult(output);
  }

  @HxRequest
  @Get("task/show/{taskId}")
  Partial.ShowTask showTask(String taskId) {
    Task task = dataService.findTask(taskId);
    if (task == null || !task.isAction() || !dataService.hasCurrentProject()) {
      return new Partial.ShowTask(task, null);
    }
    var action = new Partial.TaskAction(task.uniqueTaskId());
    return new Partial.ShowTask(task, action);
  }

}
