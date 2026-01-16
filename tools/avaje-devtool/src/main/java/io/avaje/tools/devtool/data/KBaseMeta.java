package io.avaje.tools.devtool.data;

import io.avaje.jsonb.Json;

@Json
public record KBaseMeta(
        String id,
        String name,
        String description,
        String type
) {
}
