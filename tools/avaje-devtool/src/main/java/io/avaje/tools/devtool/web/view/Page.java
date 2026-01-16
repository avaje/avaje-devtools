package io.avaje.tools.devtool.web.view;

import io.jstach.jstache.JStache;

public class Page {

    @JStache(path = "index")
    public record Index(long kbaseCount, long taskCount, WorkingContext context){}

  public record WorkingContext(String path, String name) {}
}
