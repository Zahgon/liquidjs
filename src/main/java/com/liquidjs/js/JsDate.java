/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 Jun Yang
 */
package com.liquidjs.js;

import java.time.ZoneId;
import java.util.TimeZone;

/**
 * Date parsing and locale formatting as the source runtime performs them: the date constructor
 * accepts the extended ISO form and a lenient legacy form, and a date-only ISO string is read as
 * UTC while every other form is read in the host timezone.
 */
public final class JsDate {
  private JsDate () {}

  public static final double INVALID = Double.NaN;

  private static final String[] MONTHS = {
    "January", "February", "March", "April", "May", "June",
    "July", "August", "September", "October", "November", "December"
  };
  private static final String[] WEEKDAYS = {
    "Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"
  };

  public static String monthName (int month) {
    return MONTHS[month];
  }

  public static String shortMonthName (int month) {
    return MONTHS[month].substring(0, 3);
  }

  public static String weekdayName (int day) {
    return WEEKDAYS[day];
  }

  public static String shortWeekdayName (int day) {
    return WEEKDAYS[day].substring(0, 3);
  }

  /** The host timezone offset in minutes west of UTC, the sign the date getter reports. */
  public static int localOffsetMinutes (double epochMillis) {
    if (Double.isNaN(epochMillis)) return 0;
    return -TimeZone.getDefault().getOffset((long) epochMillis) / 60000;
  }

  /** The offset in minutes west of UTC of a named timezone at an instant. */
  public static int namedOffsetMinutes (String timezoneName, double epochMillis) {
    ZoneId zone;
    try {
      zone = ZoneId.of(timezoneName);
    } catch (RuntimeException err) {
      throw new IllegalArgumentException("Invalid time zone specified: " + timezoneName);
    }
    long millis = Double.isNaN(epochMillis) ? 0L : (long) epochMillis;
    return -zone.getRules().getOffset(java.time.Instant.ofEpochMilli(millis)).getTotalSeconds() / 60;
  }

  /** The IANA name of the host timezone. */
  public static String localTimezoneName () {
    return TimeZone.getDefault().getID();
  }

  /** Parse a date string, returning the epoch in milliseconds or not-a-number. */
  public static double parse (String input) {
    String str = input.trim();
    if (str.isEmpty()) return INVALID;
    double iso = parseIso(str);
    if (!Double.isNaN(iso)) return iso;
    return parseLegacy(str);
  }

  private static double parseIso (String str) {
    java.util.regex.Matcher m = java.util.regex.Pattern.compile(
        "^([+-]\\d{6}|\\d{4})(?:-(\\d{2})(?:-(\\d{2}))?)?"
        + "(?:[T](\\d{2}):(\\d{2})(?::(\\d{2})(?:\\.(\\d{1,9}))?)?"
        + "(Z|[+-]\\d{2}:\\d{2})?)?$").matcher(str);
    if (!m.matches()) return INVALID;
    int year = Integer.parseInt(m.group(1));
    int month = m.group(2) == null ? 1 : Integer.parseInt(m.group(2));
    int day = m.group(3) == null ? 1 : Integer.parseInt(m.group(3));
    boolean dateOnly = m.group(4) == null;
    int hour = dateOnly ? 0 : Integer.parseInt(m.group(4));
    int minute = dateOnly ? 0 : Integer.parseInt(m.group(5));
    int second = m.group(6) == null ? 0 : Integer.parseInt(m.group(6));
    int millis = 0;
    if (m.group(7) != null) {
      String fraction = (m.group(7) + "000").substring(0, 3);
      millis = Integer.parseInt(fraction);
    }
    if (month < 1 || month > 12 || day < 1 || day > 31 || hour > 24 || minute > 59 || second > 59) {
      return INVALID;
    }
    double utc = utcMillis(year, month - 1, day, hour, minute, second, millis);
    String zone = m.group(8);
    if (zone == null) {
      // a date-only form is UTC, a date and time without an offset is local
      if (dateOnly) return utc;
      return utc + localOffsetAt(utc) * 60000.0;
    }
    if ("Z".equals(zone)) return utc;
    int sign = zone.charAt(0) == '-' ? -1 : 1;
    int offset = Integer.parseInt(zone.substring(1, 3)) * 60 + Integer.parseInt(zone.substring(4, 6));
    return utc - sign * offset * 60000.0;
  }

  private static double parseLegacy (String str) {
    String text = str.replaceAll("\\([^)]*\\)", " ").trim();
    int month = -1;
    int[] numbers = new int[8];
    int count = 0;
    int hour = -1;
    int minute = 0;
    int second = 0;
    int millis = 0;
    Integer offsetMinutes = null;
    boolean utc = false;
    String meridiem = null;

    java.util.regex.Matcher token = java.util.regex.Pattern.compile(
        "([A-Za-z]+)|([+-]\\d{2}:?\\d{2})|(\\d{1,2}:\\d{2}(?::\\d{2})?(?:\\.\\d{1,3})?)|(\\d+)")
        .matcher(text);
    while (token.find()) {
      if (token.group(1) != null) {
        String word = token.group(1).toLowerCase(java.util.Locale.ROOT);
        if (word.equals("am") || word.equals("pm")) {
          meridiem = word;
          continue;
        }
        if (word.equals("gmt") || word.equals("utc") || word.equals("ut") || word.equals("z")) {
          utc = true;
          continue;
        }
        int named = monthIndex(word);
        if (named >= 0) {
          if (month >= 0) return INVALID;
          month = named;
          continue;
        }
        if (weekdayIndex(word) >= 0) continue;
        Integer abbreviation = zoneAbbreviation(word);
        if (abbreviation != null) {
          offsetMinutes = abbreviation;
          continue;
        }
        return INVALID;
      }
      if (token.group(2) != null) {
        String zone = token.group(2).replace(":", "");
        int sign = zone.charAt(0) == '-' ? -1 : 1;
        offsetMinutes = -sign * (Integer.parseInt(zone.substring(1, 3)) * 60
            + Integer.parseInt(zone.substring(3, 5)));
        utc = true;
        continue;
      }
      if (token.group(3) != null) {
        String[] parts = token.group(3).split("[:.]");
        hour = Integer.parseInt(parts[0]);
        minute = Integer.parseInt(parts[1]);
        if (parts.length > 2) second = Integer.parseInt(parts[2]);
        if (parts.length > 3) millis = Integer.parseInt((parts[3] + "000").substring(0, 3));
        continue;
      }
      if (count >= numbers.length) return INVALID;
      numbers[count++] = Integer.parseInt(token.group(4));
    }

    int year;
    int day;
    if (month >= 0) {
      if (count == 1) {
        day = numbers[0] > 31 ? 1 : numbers[0];
        year = numbers[0] > 31 ? numbers[0] : 2001;
        if (numbers[0] <= 31) return INVALID;
      } else if (count >= 2) {
        if (numbers[0] > 31) {
          year = numbers[0];
          day = numbers[1];
        } else {
          day = numbers[0];
          year = numbers[1];
        }
      } else {
        return INVALID;
      }
    } else {
      if (count < 3) return INVALID;
      if (numbers[0] > 31) {
        year = numbers[0];
        month = numbers[1] - 1;
        day = numbers[2];
      } else {
        month = numbers[0] - 1;
        day = numbers[1];
        year = numbers[2];
      }
    }
    if (year < 100 && year >= 50) year += 1900;
    else if (year < 50 && count >= 3 && String.valueOf(year).length() <= 2) year += 2000;
    if (month < 0 || month > 11 || day < 1 || day > 31) return INVALID;
    if (hour < 0) hour = 0;
    if ("pm".equals(meridiem) && hour < 12) hour += 12;
    if ("am".equals(meridiem) && hour == 12) hour = 0;
    if (hour > 24 || minute > 59 || second > 59) return INVALID;

    double value = utcMillis(year, month, day, hour, minute, second, millis);
    if (offsetMinutes != null) return value + offsetMinutes * 60000.0;
    if (utc) return value;
    return value + localOffsetAt(value) * 60000.0;
  }

  private static int localOffsetAt (double utcGuess) {
    long millis = Double.isNaN(utcGuess) ? 0L : (long) utcGuess;
    int offset = -TimeZone.getDefault().getOffset(millis) / 60000;
    // the guess was read as if it were UTC, so correct it with the offset at the local instant
    long corrected = millis + offset * 60000L;
    return -TimeZone.getDefault().getOffset(corrected) / 60000;
  }

  private static int monthIndex (String word) {
    for (int i = 0; i < MONTHS.length; i++) {
      String name = MONTHS[i].toLowerCase(java.util.Locale.ROOT);
      if (name.equals(word) || (word.length() >= 3 && name.startsWith(word))) return i;
    }
    return -1;
  }

  private static int weekdayIndex (String word) {
    for (int i = 0; i < WEEKDAYS.length; i++) {
      String name = WEEKDAYS[i].toLowerCase(java.util.Locale.ROOT);
      if (name.equals(word) || (word.length() >= 3 && name.startsWith(word))) return i;
    }
    return -1;
  }

  private static Integer zoneAbbreviation (String word) {
    switch (word) {
      case "est": return 300;
      case "edt": return 240;
      case "cst": return 360;
      case "cdt": return 300;
      case "mst": return 420;
      case "mdt": return 360;
      case "pst": return 480;
      case "pdt": return 420;
      default: return null;
    }
  }

  /** The epoch in milliseconds of a UTC wall clock, with the day and month overflow of the source. */
  public static double utcMillis (int year, int month, int day, int hour, int minute, int second,
                                  int millis) {
    long days = daysFromCivil(year + Math.floorDiv(month, 12), Math.floorMod(month, 12) + 1, 1)
        + (day - 1);
    return days * 86400000.0 + hour * 3600000.0 + minute * 60000.0 + second * 1000.0 + millis;
  }

  private static long daysFromCivil (int year, int month, int day) {
    long y = year;
    y -= month <= 2 ? 1 : 0;
    long era = Math.floorDiv(y, 400);
    long yoe = y - era * 400;
    long doy = (153 * (month + (month > 2 ? -3 : 9)) + 2) / 5 + day - 1;
    long doe = yoe * 365 + yoe / 4 - yoe / 100 + doy;
    return era * 146097 + doe - 719468;
  }

  /** Field accessors on a UTC wall clock derived from an epoch in milliseconds. */
  public static final class Fields {
    public final int year;
    public final int month;
    public final int day;
    public final int weekday;
    public final int hour;
    public final int minute;
    public final int second;
    public final int millisecond;

    public Fields (double epochMillis) {
      long millis = (long) Math.floor(epochMillis);
      long days = Math.floorDiv(millis, 86400000L);
      long rest = Math.floorMod(millis, 86400000L);
      this.millisecond = (int) (rest % 1000);
      this.second = (int) ((rest / 1000) % 60);
      this.minute = (int) ((rest / 60000) % 60);
      this.hour = (int) (rest / 3600000);
      this.weekday = (int) Math.floorMod(days + 4, 7);
      long z = days + 719468;
      long era = Math.floorDiv(z, 146097);
      long doe = z - era * 146097;
      long yoe = (doe - doe / 1460 + doe / 36524 - doe / 146096) / 365;
      long y = yoe + era * 400;
      long doy = doe - (365 * yoe + yoe / 4 - yoe / 100);
      long mp = (5 * doy + 2) / 153;
      this.day = (int) (doy - (153 * mp + 2) / 5 + 1);
      int m = (int) (mp + (mp < 10 ? 3 : -9));
      this.month = m - 1;
      this.year = (int) (y + (m <= 2 ? 1 : 0));
    }
  }

  /** The date and time string the en-US locale produces, as toLocaleString does. */
  public static String toLocaleString (Fields fields) {
    return toLocaleDateString(fields) + ", " + toLocaleTimeString(fields);
  }

  public static String toLocaleDateString (Fields fields) {
    return (fields.month + 1) + "/" + fields.day + "/" + fields.year;
  }

  public static String toLocaleTimeString (Fields fields) {
    int hour12 = fields.hour % 12 == 0 ? 12 : fields.hour % 12;
    return hour12 + ":" + JsString.padStart(String.valueOf(fields.minute), 2, "0")
        + ":" + JsString.padStart(String.valueOf(fields.second), 2, "0")
        + " " + (fields.hour < 12 ? "AM" : "PM");
  }
}
