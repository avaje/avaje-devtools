package io.avaje.tools.devtool.state;

import org.junit.jupiter.api.Test;

import java.io.File;

class PreviewContentTest {

  @Test
  void asd() {
    //File file = new File("/Users/robinbygrave/github/avaje/avaje-devtools/tools/avaje-devtool/data/kbase-avaje/pom-add-simplelogger");
    File file = new File("/Users/robinbygrave/github/avaje/avaje-devtools/tools/avaje-devtool/data/kbase-ebean/test-setup-docker-pg");
    String result = PreviewContent.read(file);

    System.out.println(result);
  }
}
