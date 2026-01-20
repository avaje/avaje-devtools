package ${mainPackage}.config;

import io.avaje.inject.Bean;
import io.avaje.inject.Factory;
import io.avaje.inject.test.TestScope;
import io.ebean.Database;
import io.ebean.test.containers.PostgisContainer;

@TestScope
@Factory
class TestDatabaseSetup {

    @Bean
    PostgisContainer postgres() {
        return PostgisContainer.builder("17")
            .dbName("my_app")
            // .useLW(true)
            .build()
            .start();
    }

    @Bean
    Database database(PostgisContainer container) {
        return container.ebean().builder()
            .build();
    }

}
