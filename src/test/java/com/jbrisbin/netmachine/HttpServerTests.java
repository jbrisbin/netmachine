package com.jbrisbin.netmachine;

import static com.jbrisbin.netmachine.http.HttpHeader.*;
import static com.jbrisbin.netmachine.util.HashUtils.*;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.HashSet;
import java.util.Set;

import com.jbrisbin.netmachine.file.File;
import com.jbrisbin.netmachine.http.HttpRequest;
import com.jbrisbin.netmachine.http.HttpResponse;
import com.jbrisbin.netmachine.http.HttpServer;
import com.jbrisbin.netmachine.io.Buffer;
import org.junit.Test;

/**
 * @author Jon Brisbin <jon@jbrisbin.com>
 */
public class HttpServerTests {

  private Set<OpenOption> putOptions = new HashSet<OpenOption>() {{
    add(StandardOpenOption.CREATE);
    add(StandardOpenOption.TRUNCATE_EXISTING);
    add(StandardOpenOption.WRITE);
  }};

  @Test
  public void testHttpServer() throws InterruptedException, IOException {
    Handler<HttpRequest> handler = new Handler<HttpRequest>() {
      @Override public void handle(HttpRequest request) {
        final String uri = request.uri().getPath();

        HttpResponse response;
        switch (request.method()) {
          case GET:
            Path path = Paths.get("src/test/upload/1mb.bytes");
            try {
              long size = Files.size(path);
              response = HttpResponse.ok("application/octet-stream", Paths.get("src/test/upload/1mb.bytes"))
                                     .contentLength(size);
            } catch (IOException e) {
              e.printStackTrace();
              response = failure(e);
            }
            request.reply(response);
            break;
          case PUT:
            String hash = new BigInteger(sha1(uri)).toString();
            try {
              // Shove everything into a file
              Path dataDir = Paths.get("data", hash.substring(0, 2));
              Files.createDirectories(dataDir);
              Path outPath = Paths.get(dataDir.toString(), hash);
              final File out = File.overwrite(outPath);
              final HttpResponse resp = HttpResponse.noContent()
                                                    .header(LOCATION, uri);
              request.reply(resp);
              request.readHandler(out.writeHandler());

            } catch (FileNotFoundException e) {
              e.printStackTrace();
              request.reply(failure(e));
            } catch (IOException e) {
              e.printStackTrace();
              request.reply(failure(e));
            }
            break;
          default:
            request.reply(HttpResponse.noContent()
                                      .header(LOCATION, uri));
        }

      }

      private HttpResponse failure(Throwable t) {
        return HttpResponse.error()
                           .contentType("text/plain")
                           .write(t.getMessage())
                           .complete();
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
