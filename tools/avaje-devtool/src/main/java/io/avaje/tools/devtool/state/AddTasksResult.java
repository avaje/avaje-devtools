package io.avaje.tools.devtool.state;

import io.avaje.tools.devtool.data.KBaseSource;

public record AddTasksResult(long addedCount, KBaseSource newSource, int totalTaskCount) {
}
