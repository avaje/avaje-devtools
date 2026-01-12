package io.avaje.tools.devtool.web;

import io.avaje.htmx.api.Html;
import io.avaje.htmx.api.HxRequest;
import io.avaje.http.api.*;
import io.avaje.jex.http.Context;
import io.avaje.tools.devtool.data.Data;
import io.avaje.tools.devtool.data.MDataSource;
import io.avaje.tools.devtool.data.MProjects;
import io.avaje.tools.devtool.data.Task;
import io.avaje.tools.devtool.service.ClipB;
import io.avaje.tools.devtool.service.DataService;
import io.avaje.tools.devtool.web.view.Page;
import io.avaje.tools.devtool.web.view.Partial;

import java.util.List;
import java.util.Optional;

@Html
@Controller
final class IndexController {

  private final DataService dataService;

  IndexController(DataService dataService) {
    this.dataService = dataService;
  }

  @Get
  Page.Index home() {
    Data data = dataService.data();
    return new Page.Index(data.kbaseCount(), data.taskCount());
  }

  @HxRequest
  @Get("initial")
  Object initial() {
    MProjects projects = dataService.projects();
    List<MDataSource> mDataSources = projects.dataSources();
    if (mDataSources.isEmpty()) {
      return new Partial.InitialEmpty();
    }
    return searchTasks("docs");
  }


  @HxRequest
  @Get("cb")
  void getClipboard(Context context) {
    String clipboardText = ClipB.getClipboardText();
    System.out.println("cb: " + clipboardText);
//    String c = """
//      <input id="task-search-input" type="text" value="{}" hx-swap-oob="innerHTML">
//      """;
//    var r = c.replace("{}", clipboardText);
    context.text(clipboardText);
  }

  @HxRequest
  @Form
  @Post("copyToClipboard")
  Partial.Toast copyToClipboard(String clip) {
    ClipB.setClipboardText(clip);
    return new Partial.Toast("Copied to clipboard");
  }

  @HxRequest
  @Form
  @Post("search")
  Partial.SearchTasks searchTasks(String search) {
    String clipboardText = ClipB.getClipboardText();
    System.out.println("clipboardText = " + clipboardText);
    List<Task> tasks = dataService.searchTasks(search, 10);
    var first = !tasks.isEmpty() ? tasks.getFirst() : null;
    return new Partial.SearchTasks(first, tasks);
  }

  @HxRequest
  @Form
  @Post("searchProjects")
  Partial.SearchTasks searchProjects(String search) {
    //TODO searchProjects
    List<Task> tasks = dataService.searchTasks(search, 10);
    var first = !tasks.isEmpty() ? tasks.getFirst() : null;
    return new Partial.SearchTasks(first, tasks);
  }

  @HxRequest
  @Form
  @Post("searchSources")
  Partial.SearchTasks searchSources(String search) {
    // TODO searchSources
    List<Task> tasks = dataService.searchTasks(search, 10);
    var first = !tasks.isEmpty() ? tasks.getFirst() : null;
    return new Partial.SearchTasks(first, tasks);
  }

  @HxRequest
  @Get("projects")
  Partial.Projects projects() {
    MProjects projects = dataService.projects();
    return new Partial.Projects(projects.projects());
  }

  @HxRequest
  @Get("tasks")
  Partial.Tasks tasks() {
    MProjects projects = dataService.projects();
    return new Partial.Tasks(null, List.of());
  }

  @HxRequest
  @Get("sources")
  Partial.Sources sources() {
    var sources = dataService.projects().dataSources();
    return new Partial.Sources(sources);
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
    Optional<Data> data = dataService.addSource(path);
    Long count = data.map(Data::kbaseCount).orElse(0L);
    context.html("<p>Added source with " + count + " kbases</p>");
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
}
