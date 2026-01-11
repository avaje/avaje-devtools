package io.avaje.tools.devtool.data;

import io.avaje.jsonb.Json;

@Json
public record KBaseMeta(
        String name,
        String description
) {
}
