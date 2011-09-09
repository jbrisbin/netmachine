package com.jbrisbin.netmachine;

import java.io.IOException;

import com.jbrisbin.netmachine.http.HttpRequest;
import com.jbrisbin.netmachine.http.HttpResponse;
import com.jbrisbin.netmachine.http.HttpServer;
import org.junit.Test;

/**
 * @author Jon Brisbin <jon@jbrisbin.com>
 */
public class HttpServerTests {

  @Test
  public void testHttpServer() throws InterruptedException, IOException {
    Handler<HttpRequest> handler = new Handler<HttpRequest>() {
      @Override public void handle(HttpRequest request) {

        String hello = "Hello " + request.paramOr("name", "World") + "!";
        HttpResponse response = HttpResponse.ok("text/plain", hello);
        request.reply(response);

      }
    };

    new HttpServer()
        .route("/{bucket}/{key}**", handler)
        .start();

    while (true) {
      Thread.sleep(10000);
    }
  }

}
