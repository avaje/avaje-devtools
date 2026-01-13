package io.avaje.tools.devtool.web;

import io.avaje.htmx.api.Html;
import io.avaje.htmx.api.HxRequest;
import io.avaje.http.api.*;
import io.avaje.jex.http.Context;
import io.avaje.tools.devtool.data.*;
import io.avaje.tools.devtool.service.ClipB;
import io.avaje.tools.devtool.service.DataService;
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
    Data data = dataService.data();
    return new Page.Index(data.tasks().size(), data.tasks().size());
  }

  @HxRequest
  @Get("initial")
  Object initial() {
    var sources = dataService.data().sources();
    if (sources.isEmpty()) {
      return new Partial.InitialEmpty();
    }
    return tasks();
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
    //String clipboardText = ClipB.getClipboardText();
    //System.out.println("clipboardText = " + clipboardText);
    List<Task> tasks = dataService.searchTasks(search, 10);
    var first = !tasks.isEmpty() ? tasks.getFirst() : null;
    return new Partial.SearchTasks(first, tasks);
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
  @Post("searchSources")
  Partial.SearchSources searchSources(String search) {
    var sources = dataService.searchSources(search, 10);
    return new Partial.SearchSources(sources);
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
    return new Partial.Tasks(first, tasks);
  }

  @HxRequest
  @Get("sources")
  Partial.Sources sources() {
    var sources = dataService.data().sources();
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
    long count = dataService.addSource(path);
    context.html("<p>Added source with " + count + " new tasks</p>");
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
