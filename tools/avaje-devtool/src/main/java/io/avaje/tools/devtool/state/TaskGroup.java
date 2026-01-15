package io.avaje.tools.devtool.state;

import io.avaje.tools.devtool.data.KBaseMeta;

import java.util.List;

public record TaskGroup(
        KBaseMeta meta,
        List<Task> tasks
) {
    @Override
    public String toString() {
        return meta.name() + " tasks:" + tasks.size();
    }
}
