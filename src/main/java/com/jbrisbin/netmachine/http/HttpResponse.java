package com.jbrisbin.netmachine.http;

/**
 * @author Jon Brisbin <jon@jbrisbin.com>
 */
public class HttpResponse extends HttpMessage<HttpResponse> {

  public static HttpResponse ok() {
    return new HttpResponse().status(200, "OK");
  }

  public static <T> HttpResponse ok(String contentType, T content) {
    return new HttpResponse()
        .status(200, "OK")
        .contentType(contentType)
        .write(content)
        .complete();
  }

  public static HttpResponse noContent() {
    return new HttpResponse()
        .status(204, "No Content")
        .contentLength(0);
  }

  public static HttpResponse notFound() {
    return new HttpResponse().status(404, "Resource Not Found");
  }

  public static HttpResponse error() {
    return new HttpResponse().status(500, "Internal Server Error");
  }

  private int statusCode;
  private String reasonPhrase;

  public HttpResponse status(int code, String reasonPhrase) {
    this.statusCode = code;
    this.reasonPhrase = reasonPhrase;
    return this;
  }

  public int statusCode() {
    return statusCode;
  }

  public HttpResponse statusCode(int statusCode) {
    this.statusCode = statusCode;
    return this;
  }

  public String reasonPhrase() {
    return reasonPhrase;
  }

  public HttpResponse reasonPhrase(String reasonPhrase) {
    this.reasonPhrase = reasonPhrase;
    return this;
  }

  @Override public String toString() {
    return "HttpResponse{" +
        "statusCode=" + statusCode +
        ", reasonPhrase='" + reasonPhrase + '\'' +
        super.toString() +
        '}';
  }
}
