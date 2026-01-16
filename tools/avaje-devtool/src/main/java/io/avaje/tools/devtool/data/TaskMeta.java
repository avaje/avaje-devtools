package io.avaje.tools.devtool.data;

import io.avaje.jsonb.Json;

import java.util.List;
import java.util.Map;

@Json
public record TaskMeta(
        String type,
        String displayName,
        String searchKeywords,
        int priority,
        String description,
        List<TaskAction> actions
) {
    public String allSearchKeywords() {
        return searchKeywords == null || searchKeywords.isBlank()
                ? displayName.toLowerCase()
                : (displayName + " " + searchKeywords).toLowerCase();
    }

    @Json
    public record TaskAction(
      String action,
      String source,
      String target,
      @Json.Unmapped
      Map<String,Object> unmapped) {}
}
