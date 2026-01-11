package io.ebean.tools.init.action;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class HttpGetTest {

  @Test
  public void get() throws Exception {

    final HttpGet.Response response = HttpGet.get("Customer.java");
    assertThat(response.ok()).isTrue();
  }
}
