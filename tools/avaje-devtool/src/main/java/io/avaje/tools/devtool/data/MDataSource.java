package io.avaje.tools.devtool.data;

import io.avaje.jsonb.Json;

@Json
public record MDataSource(String name, String type, String path) {

}
