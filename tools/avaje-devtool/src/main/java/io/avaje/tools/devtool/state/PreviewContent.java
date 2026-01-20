package io.avaje.tools.devtool.state;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class PreviewContent {

  private final Pattern pattern = Pattern.compile("<meta\\s+resource=\"([^\"]+)\"\\s*/?>");

  private final File taskDir;
  private final StringBuilder out = new StringBuilder(200);
  private boolean useJavaEscape;

  private PreviewContent(File taskDir) {
    this.taskDir = taskDir;
  }

  static String read(File taskDir) {
    return new PreviewContent(taskDir).readPreview();
  }

  private String readPreview() {
    var file = new File(taskDir, "preview.html");
    if (!file.exists()) {
      return "";
    }
    try {
      for (String line : Files.readAllLines(file.toPath())) {
        parseLine(line);
      }
      return out.toString();
    } catch (IOException e) {
      return "";
    }
  }

  private void parseLine(String line) throws IOException {
    Matcher matcher = pattern.matcher(line);
    if (!matcher.find()) {
      out.append(line).append('\n');
    } else {
      // Group 1 captures the content inside the quotes
      String resourceValue = matcher.group(1);
      File res = new File(taskDir, resourceValue);
      if (res.exists()) {
        out.append("<div class=\"code-container\">\n");
        out.append("  <button class=\"copy-btn\" onclick=\"copyCode(this)\">copy</button>");
        out.append("  <pre><code");
        String language = language(res.getName().toLowerCase());
        if (language != null) {
          out.append(" class=\"").append(language).append('"');
        }
        out.append('>');
        for (String allFileLine : Files.readAllLines(res.toPath())) {
          out.append(escapeLine(allFileLine)).append('\n');
        }
        out.append("</code></pre>\n");
        out.append("</div>\n");
      }
    }
  }

  private String language(String lowerCase) {
    useJavaEscape = false;
    if (lowerCase.endsWith("xml")) {
      return "language-xml";
    }
    if (lowerCase.endsWith("java")) {
      useJavaEscape = true;
      return "language-java";
    }
    return null;
  }

  private String escapeLine(String line) {
    if (useJavaEscape) {
      line = line.replace("${mainPackage}", "example");
    }
    return line
      .replace("<", "&lt;")
      .replace(">", "&gt;");
  }
}
