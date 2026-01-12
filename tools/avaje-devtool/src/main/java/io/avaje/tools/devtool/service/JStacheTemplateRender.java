package io.avaje.tools.devtool.service;

import io.avaje.jex.htmx.TemplateRender;
import io.jstach.jstachio.JStachio;

public class JStacheTemplateRender implements TemplateRender {

  @Override
  public String render(Object viewModel) {
    return JStachio.render(viewModel);
  }
}
