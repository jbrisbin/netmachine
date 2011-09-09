package com.jbrisbin.netmachine.http;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * @author Jon Brisbin <jon@jbrisbin.com>
 */
public abstract class HttpHeaders {

  public static final String ACCEPT = "Accept";
  public static final String ACCEPT_CHARSET = "Accept-Charset";
  public static final String ACCEPT_ENCODING = "Accept-Encoding";
  public static final String ACCEPT_LANGUAGE = "Accept-Language";
  public static final String ACCEPT_RANGES = "Accept-Ranges";
  public static final String AGE = "Age";
  public static final String ALLOW = "Allow";
  public static final String AUTHORIZATION = "Authorization";
  public static final String CACHE_CONTROL = "Cache-Control";
  public static final String CONNECTION = "Connection";
  public static final String CONTENT_ENCODING = "Content-Encoding";
  public static final String CONTENT_LANGUAGE = "Content-Language";
  public static final String CONTENT_LENGTH = "Content-Length";
  public static final String CONTENT_LOCATION = "Content-Location";
  public static final String CONTENT_MD5 = "Content-MD5";
  public static final String CONTENT_RANGE = "Content-Range";
  public static final String CONTENT_TYPE = "Content-Type";
  public static final String DATE = "Date";
  public static final String ETAG = "ETag";
  public static final String EXPECT = "Expect";
  public static final String EXPIRES = "Expires";
  public static final String FROM = "From";
  public static final String HOST = "Host";
  public static final String IF_MATCH = "If-Match";
  public static final String IF_MODIFIED_SINCE = "If-Modified-Since";
  public static final String IF_NONE_MATCH = "If-None-Match";
  public static final String IF_RANGE = "If-Range";
  public static final String IF_UNMODIFIED_SINCE = "If-Unmodified-Since";
  public static final String LAST_MODIFIED = "Last-Modified";
  public static final String LOCATION = "Location";
  public static final String MAX_FORWARDS = "Max-Forwards";
  public static final String PRAGMA = "Pragma";
  public static final String PROXY_AUTHENTICATE = "Proxy-Authenticate";
  public static final String PROXY_AUTHORIZATION = "Proxy-Authorization";
  public static final String RANGE = "Range";
  public static final String REFERER = "Referer";
  public static final String RETRY_AFTER = "Retry-After";
  public static final String SERVER = "Server";
  public static final String TE = "TE";
  public static final String TRAILER = "Trailer";
  public static final String TRANSFER_ENCODING = "Transfer-Encoding";
  public static final String UPGRADE = "Upgrade";
  public static final String USER_AGENT = "User-Agent";
  public static final String VARY = "Vary";
  public static final String VIA = "Via";
  public static final String WARNING = "Warning";
  public static final String WWW_AUTHENTICATE = "WWW-Authenticate";

  public static final String[] HTTP_1_1 = new String[]{
      ACCEPT,
      ACCEPT_CHARSET,
      ACCEPT_ENCODING,
      ACCEPT_LANGUAGE,
      ACCEPT_RANGES,
      AGE,
      ALLOW,
      AUTHORIZATION,
      CACHE_CONTROL,
      CONNECTION,
      CONTENT_ENCODING,
      CONTENT_LANGUAGE,
      CONTENT_LENGTH,
      CONTENT_LOCATION,
      CONTENT_MD5,
      CONTENT_RANGE,
      CONTENT_TYPE,
      DATE,
      ETAG,
      EXPECT,
      EXPIRES,
      FROM,
      HOST,
      IF_MATCH,
      IF_MODIFIED_SINCE,
      IF_NONE_MATCH,
      IF_RANGE,
      IF_UNMODIFIED_SINCE,
      LAST_MODIFIED,
      LOCATION,
      MAX_FORWARDS,
      PRAGMA,
      PROXY_AUTHENTICATE,
      PROXY_AUTHORIZATION,
      RANGE,
      REFERER,
      RETRY_AFTER,
      SERVER,
      TE,
      TRAILER,
      TRANSFER_ENCODING,
      UPGRADE,
      USER_AGENT,
      VARY,
      VIA,
      WARNING,
      WWW_AUTHENTICATE
  };

  public static boolean matches(String s, String expected) {
    return (null != s ? s.toLowerCase().equals(expected.toLowerCase()) : false);
  }

  public static List<MediaRange> parseAccept(String header) {
    String[] parts = header.split(",");
    List<MediaRange> ranges = new ArrayList<>(parts.length);
    for (String s : parts) {
      ranges.add(new MediaRange(s));
    }
    return ranges;
  }

  public static ContentRange parseContentRange(String header) {
    return null;
  }

  public static class MediaRange implements Comparable<MediaRange> {

    private String type;
    private Float qualityFactor = 1F;
    private SortedMap<String, String> parameters = new TreeMap<>();

    public MediaRange(String s) {
      String[] parts = s.split(";");
      type = parts[0];
      for (int i = 1; i < parts.length; i++) {
        String[] paramParts = parts[i].split("=");
        String name = paramParts[0].trim();
        String value = paramParts[1].trim();
        if ("q".equals(name)) {
          qualityFactor = Float.parseFloat(value);
        } else {
          parameters.put(name, value);
        }
      }
    }

    public String getType() {
      return type;
    }

    public void setType(String type) {
      this.type = type;
    }

    public Float getQualityFactor() {
      return qualityFactor;
    }

    public void setQualityFactor(Float qualityFactor) {
      this.qualityFactor = qualityFactor;
    }

    public void setParameter(String name, String value) {
      parameters.put(name, value);
    }

    public Map<String, String> getParameters() {
      return parameters;
    }

    private String getFullType() {
      StringBuffer buff = new StringBuffer(type);
      for (Map.Entry<String, String> entry : parameters.entrySet()) {
        buff.append(";").append(entry.getKey()).append("=").append(entry.getValue());
      }
      return buff.toString();
    }

    @Override public int compareTo(MediaRange mediaRange) {
      int i;
      if (null != type && null != mediaRange.getType()) {
        i = type.compareTo(mediaRange.getType());
        if (i != 0) {
          return i;
        }
      } else {
        return -1;
      }

      i = getFullType().compareTo(mediaRange.getFullType());
      if (i != 0) {
        return i;
      }

      i = qualityFactor.compareTo(mediaRange.getQualityFactor());
      if (i != 0) {
        return i;
      }

      return i;
    }

  }

  public class ContentRange {

    private String unit = "bytes";
    private Integer start;
    private Integer end;

    public ContentRange(String unit, Integer start, Integer end) {
      this.unit = unit;
      this.start = start;
      this.end = end;
    }

    public ContentRange(Integer start, Integer end) {
      this.start = start;
      this.end = end;
    }

    public String getUnit() {
      return unit;
    }

    public void setUnit(String unit) {
      this.unit = unit;
    }

    public Integer getStart() {
      return start;
    }

    public void setStart(Integer start) {
      this.start = start;
    }

    public Integer getEnd() {
      return end;
    }

    public void setEnd(Integer end) {
      this.end = end;
    }

    @Override public String toString() {
      return String.format("%s=%s-%s", unit, (null != start ? start : ""), (null != end ? end : ""));
    }

  }

}
