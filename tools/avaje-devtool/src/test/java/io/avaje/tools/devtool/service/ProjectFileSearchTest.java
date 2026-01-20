package io.avaje.tools.devtool.service;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ProjectFileSearchTest {

  @Disabled
  @Test
  void search() {
    String path = "~/Documents/github/avaje";
    ProjectFileSearch search = ProjectFileSearch.matchProjectFiles(path);
    System.out.println("Directories searched: " + search.totalDirectoriesSearched());

    List<ModelProjectMaven> topLevel = search.topLevel();
    List<ModelProjectMaven> list1 = search.all().toList();
    System.out.println("Found top level projects: " + topLevel.size());
    System.out.println("Found all projects: " + list1.size());

    assertThat(search.totalDirectoriesSearched()).isGreaterThan(4);
    assertThat(topLevel).isNotEmpty();

    List<ModelProjectMaven> list = topLevel.stream()
      .filter(p -> p.searchText().contains(":consol"))
      .toList();

    System.out.println(list);
  }
}
