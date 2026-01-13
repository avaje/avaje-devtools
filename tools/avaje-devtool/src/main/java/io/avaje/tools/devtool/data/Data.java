package io.avaje.tools.devtool.data;

import java.util.List;
import java.util.stream.Stream;

public record Data(

  List<MProject> projects,
  List<Task> tasks,
  List<MDataSource> sources
) {

}
