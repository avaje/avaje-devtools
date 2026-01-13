package io.avaje.tools.devtool.data;

import io.avaje.jsonb.Json;

@Json
public record MDataSource(String name, String type, String path) {

  public boolean matchAll(String[] tokens) {
    for (String token : tokens) {
      if (!name.toLowerCase().contains(token) || !path.toLowerCase().contains(token)) {
        return false;
      }
    }
    return true;
  }
}
