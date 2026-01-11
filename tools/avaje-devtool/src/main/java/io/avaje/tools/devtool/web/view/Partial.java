package io.avaje.tools.devtool.web.view;

import io.avaje.tools.devtool.data.Task;
import io.jstach.jstache.JStache;

import java.util.List;

public class Partial {

    @JStache(path = "partial/search-tasks")
    public record SearchTasks(Task first, List<Task> tasks){}

    @JStache(path = "partial/sidebar")
    public static class Sidebar {
    }
}
