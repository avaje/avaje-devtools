package io.avaje.tools.devtool.web;

import io.avaje.htmx.api.HxRequest;
import io.avaje.http.api.Controller;
import io.avaje.http.api.Form;
import io.avaje.http.api.Get;
import io.avaje.http.api.Post;
import io.avaje.tools.devtool.data.Data;
import io.avaje.tools.devtool.data.Task;
import io.avaje.tools.devtool.service.DataService;
import io.avaje.tools.devtool.web.view.Page;
import io.avaje.tools.devtool.web.view.Partial;

import java.util.List;

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
    @Form @Post("search")
    Partial.SearchTasks searchTasks(String search) {
        List<Task> tasks = dataService.searchTasks(search, 10);
        var first = !tasks.isEmpty() ? tasks.getFirst() : null;
        return new Partial.SearchTasks(first, tasks);
    }

    @HxRequest
    @Get("toggle/sidebar")
    Partial.Sidebar toggleSidebar() {
        return new Partial.Sidebar();
    }
}
