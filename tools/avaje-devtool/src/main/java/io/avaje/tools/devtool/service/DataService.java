package io.avaje.tools.devtool.service;

import io.avaje.inject.Component;
import io.avaje.tools.devtool.data.Data;
import io.avaje.tools.devtool.data.MProjects;
import io.avaje.tools.devtool.data.Task;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

@Component
public final class DataService {

    private static final System.Logger log = System.getLogger("app");

    private final ProjectsRepository repository;

    DataService(ProjectsRepository repository) {
      this.repository = repository;
    }

    public Data data() {
        return repository.data();
    }

    void refreshData(Data data) {
      repository.refreshData(data);
    }

//    @PostConstruct
//    void initialLoad() {
//        data = loadPath(System.getProperty("data.dir", "data"))
//                .or(() -> loadPath("avaje-devtool/data"))
//                .orElse(new Data(List.of()));
//    }
//
//    private Optional<Data> loadPath(String path) {
//        var dataDir = new File(path);
//        if (!dataDir.exists()) {
//            return Optional.empty();
//        }
//        log.log(DEBUG, "Load data from {0}", dataDir.getAbsolutePath());
//        return Optional.of(DataLoader.load(jsonb, dataDir));
//    }

    public List<Task> searchTasks(String search, int limit) {
        if (search == null) return List.of();
        String[] tokens = asTokens(search);
        return searchTasks(tokens, limit);
    }

    static String[] asTokens(String search) {
        String[] tokens = search.split(" ");
        return Stream.of(tokens).map(String::toLowerCase).toList().toArray(new String[0]);
    }

    private List<Task> searchTasks(String[] tokens, int limit) {
        if (tokens == null || tokens.length == 0) {
            return List.of();
        }
        return data().kbases().stream()
                .flatMap(kb -> kb.tasks().stream())
                .filter(task -> task.matchAll(tokens))
                .sorted()
                .limit(limit)
                .toList();
    }

  public MProjects projects() {
    return repository.projects();
  }

  public Optional<Data> addSource(String path) {
    return repository.addSource(path);
  }
}
