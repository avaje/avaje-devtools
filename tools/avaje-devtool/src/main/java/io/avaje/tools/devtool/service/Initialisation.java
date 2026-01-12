package io.avaje.tools.devtool.service;

import io.avaje.inject.Bean;
import io.avaje.inject.Factory;
import io.avaje.jex.htmx.TemplateRender;
import io.avaje.jsonb.Jsonb;
import io.jstach.jstachio.JStachio;

@Factory
final class Initialisation {

    @Bean
    Jsonb jsonb() {
        return Jsonb.builder().build();
    }

    @Bean
    TemplateRender templateRender() {
        return new JStacheTemplateRender();
    }

}
