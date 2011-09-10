package com.jbrisbin.netmachine.http.util;

import static com.jbrisbin.netmachine.http.HttpHeader.*;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;


/**
 * @author Jon Brisbin <jon@jbrisbin.com>
 */
public class HttpHeaderUtils {

  private static final DateFormat RFC2616_DATE_FORMAT = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss z");

  public static Date parseDate(String s) {
    synchronized (RFC2616_DATE_FORMAT) {
      try {
        return RFC2616_DATE_FORMAT.parse(s);
      } catch (ParseException e) {
        return null;
      }
    }
  }

  public static String formatDate(Date d) {
    synchronized (RFC2616_DATE_FORMAT) {
      return RFC2616_DATE_FORMAT.format(d);
    }
  }

  public static SortedSet<MediaRange> parseAccept(String header) {
    List<MediaRange> ranges = new ArrayList<>();
    for (String range : header.split(",")) {
      ranges.add(new MediaRange(range.trim()));
    }
    return sortMediaRanges(ranges);
  }

  public static String negotiateContentType(final SortedSet<MediaRange> ranges, final Collection<String> types) {
    // Found types in the list that match a media range
    LinkedHashMap<MediaRange, String> foundRanges = new LinkedHashMap<>();
    for (MediaRange mr : ranges) {
      for (String type : types) {
        if (compareMediaRangeToType(mr.getType(), type) == 0) {
          foundRanges.put(mr, type);
        }
      }
    }

    if (foundRanges.isEmpty()) {
      return null;
    } else {
      return foundRanges.values().iterator().next();
    }
  }

  public static SortedSet<MediaRange> sortMediaRanges(Collection<MediaRange> ranges) {
    SortedSet<MediaRange> sortedRanges = new TreeSet<>(new Comparator<MediaRange>() {
      @Override public int compare(MediaRange mr1, MediaRange mr2) {
        return mr1.compareTo(mr2);
      }
    });
    sortedRanges.addAll(ranges);
    return sortedRanges;
  }

  private static int compareMediaRangeToType(String mr, String type) {
    if ("*/*".equals(mr)) {
      // This matches anything
      return 0;
    }

    String[] mrs = mr.split("/");
    String[] types = type.split("/");

    int i = mrs[0].compareTo(types[0]);
    if ("*".equals(mrs[1])) {
      return i;
    } else if (i == 0) {
      return types[1].compareTo(types[1]);
    }
    return i;
  }

}
