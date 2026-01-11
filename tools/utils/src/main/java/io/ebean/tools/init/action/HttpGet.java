package io.ebean.tools.init.action;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Map;

public class HttpGet {

  private static final String host = "https://ebean.io/sample-source/";

  private static final String NEWLINE = "\n";

  private static final String USER_AGENT = "EbeanCLI";

  public static Response get(String url) throws Exception {

    HttpURLConnection con = (HttpURLConnection) new URL(host + url).openConnection();
    con.setRequestMethod("GET");
    con.setRequestProperty("User-Agent", USER_AGENT);

    int responseCode = con.getResponseCode();
    if (responseCode < 200 || responseCode >= 300) {
      return new Response(responseCode);
    }

    StringBuilder content = new StringBuilder();
    try (BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()))) {

      String inputLine;
      while ((inputLine = in.readLine()) != null) {
        content.append(inputLine).append(NEWLINE);
      }
    }

    return new Response(content.toString());
  }

  public static Map<String, String> asMap(Response response) {

    Map<String, String> map = new LinkedHashMap<>();

    final String content = response.getContent();
    final String[] lines = content.split("\n");
    for (String line : lines) {
      if (!line.isEmpty()) {
        final String[] split = line.split("=");
        map.put(split[0], split[1]);
      }
    }

    return map;
  }

  public static class Response {

    private final int status;
    private final String content;

    Response(int status) {
      this.status = status;
      this.content = null;
    }

    Response(String content) {
      this.status = 200;
      this.content = content;
    }

    public String getContent() {
      return content;
    }

    public boolean ok() {
      return status == 200;
    }
  }
}
