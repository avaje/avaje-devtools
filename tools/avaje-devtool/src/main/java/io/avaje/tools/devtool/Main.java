package io.avaje.tools.devtool;

import io.avaje.inject.*;
import io.avaje.jex.Jex;
import io.avaje.webview.Webview;

import java.net.URISyntaxException;

import static java.lang.System.Logger.Level.DEBUG;

public class Main {

    static final System.Logger log = System.getLogger("app");

    static void main(String[] args) throws URISyntaxException {
        var server = Jex.create()
                .configureWith(BeanScope.builder().build())
                .port(Integer.getInteger("http.port", 8092)) // 8092
                .start();

        int port = server.port();

        Webview wv = Webview.builder()
                .enableDeveloperTools(true)
                .extractToUserHome(true)
                .title("avaje devtool")
                .width(1000)
                .height(800)
                .navigate("http://localhost:" + port)
                .build();

        wv.setIcon(Main.class.getResource("/static/favicon.ico").toURI());
        wv.setTitle("avaje devtool");
        wv.run();
        server.shutdown();
        log.log(DEBUG, "done");
    }
}
