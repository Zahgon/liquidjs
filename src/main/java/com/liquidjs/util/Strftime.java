/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 Jun Yang
 */
package com.liquidjs.util;

import com.liquidjs.js.Js;
import com.liquidjs.js.JsString;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Date formatting by conversion code, with the flags, the widths and the padding of strftime. */
public final class Strftime {
  private Strftime () {}

  private static final Pattern FORMAT = Pattern.compile("%([-_0^#:]+)?(\\d+)?([EO])?(.)", Pattern.DOTALL);

  /** The width a conversion pads to when no width was given; the default is no padding. */
  private static final Map<Character, Integer> PAD_WIDTHS = new HashMap<>();
  private static final String PAD_SPACE_CHARS = "aAbBceklpP";

  static {
    PAD_WIDTHS.put('C', 2);
    PAD_WIDTHS.put('d', 2);
    PAD_WIDTHS.put('e', 2);
    PAD_WIDTHS.put('H', 2);
    PAD_WIDTHS.put('I', 2);
    PAD_WIDTHS.put('j', 3);
    PAD_WIDTHS.put('k', 2);
    PAD_WIDTHS.put('l', 2);
    PAD_WIDTHS.put('L', 3);
    PAD_WIDTHS.put('m', 2);
    PAD_WIDTHS.put('M', 2);
    PAD_WIDTHS.put('S', 2);
    PAD_WIDTHS.put('U', 2);
    PAD_WIDTHS.put('W', 2);
    PAD_WIDTHS.put('y', 2);
  }

  private static int[] daysInMonth (LiquidDate date) {
    int feb = isLeapYear(date) ? 29 : 28;
    return new int[] { 31, feb, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31 };
  }

  private static int getDayOfYear (LiquidDate date) {
    int num = 0;
    int[] months = daysInMonth(date);
    for (int i = 0; i < date.getMonth(); ++i) num += months[i];
    return num + date.getDate();
  }

  private static String getWeekOfYear (LiquidDate date, int startDay) {
    // days before the first startDay of the year belong to week zero
    int daysSinceStartDay = ((date.getDay() - startDay) % 7 + 7) % 7;
    return String.valueOf((int) Math.floor((getDayOfYear(date) - 1 - daysSinceStartDay + 7) / 7.0));
  }

  private static boolean isLeapYear (LiquidDate date) {
    int year = date.getFullYear();
    return (year & 3) == 0 && (year % 100 != 0 || (year % 400 == 0 && year != 0));
  }

  private static String ordinal (LiquidDate date) {
    int day = date.getDate();
    if (day == 11 || day == 12 || day == 13) return "th";
    switch (day % 10) {
      case 1: return "st";
      case 2: return "nd";
      case 3: return "rd";
      default: return "th";
    }
  }

  private static int century (LiquidDate date) {
    return (int) Math.floor(date.getFullYear() / 100.0);
  }

  private static String getTimezoneOffset (LiquidDate date, boolean colon) {
    int offset = date.getTimezoneOffset();
    int absolute = Math.abs(offset);
    int hours = absolute / 60;
    int minutes = absolute % 60;
    return (offset > 0 ? "-" : "+")
        + JsString.padStart(String.valueOf(hours), 2, "0")
        + (colon ? ":" : "")
        + JsString.padStart(String.valueOf(minutes), 2, "0");
  }

  public static String strftime (LiquidDate date, String formatStr) {
    return strftime(date, formatStr, null);
  }

  public static String strftime (LiquidDate date, String formatStr, Limiter memoryLimit) {
    StringBuilder output = new StringBuilder();
    String remaining = formatStr;
    while (true) {
      Matcher match = FORMAT.matcher(remaining);
      if (!match.find()) break;
      output.append(remaining, 0, match.start());
      remaining = remaining.substring(match.end());
      output.append(format(date, match, memoryLimit));
    }
    return output + remaining;
  }

  private static String format (LiquidDate date, Matcher match, Limiter memoryLimit) {
    String input = match.group(0);
    String flagStr = match.group(1) == null ? "" : match.group(1);
    String width = match.group(2);
    char conversion = match.group(4).charAt(0);

    Map<Character, Boolean> flags = new HashMap<>();
    for (int i = 0; i < flagStr.length(); i++) flags.put(flagStr.charAt(i), true);

    String ret = convert(date, conversion, width, flags, memoryLimit);
    if (ret == null) return input;

    String padChar = PAD_SPACE_CHARS.indexOf(conversion) >= 0 ? " " : "0";
    int padWidth = width == null || width.isEmpty() ? 0 : Integer.parseInt(width);
    if (padWidth == 0) padWidth = PAD_WIDTHS.getOrDefault(conversion, 0);
    if (flags.containsKey('^')) ret = JsString.toUpperCase(ret);
    else if (flags.containsKey('#')) ret = Underscore.changeCase(ret);
    if (flags.containsKey('_')) padChar = " ";
    else if (flags.containsKey('0')) padChar = "0";
    if (flags.containsKey('-')) padWidth = 0;
    if (memoryLimit != null) memoryLimit.use(padWidth - ret.length());
    return JsString.padStart(ret, padWidth, padChar);
  }

  private static String convert (LiquidDate d, char conversion, String width,
                                 Map<Character, Boolean> flags, Limiter memoryLimit) {
    switch (conversion) {
      case 'a': return d.getShortWeekdayName();
      case 'A': return d.getLongWeekdayName();
      case 'b': case 'h': return d.getShortMonthName();
      case 'B': return d.getLongMonthName();
      case 'c': return d.toLocaleString();
      case 'C': return String.valueOf(century(d));
      case 'd': case 'e': return String.valueOf(d.getDate());
      case 'H': case 'k': return String.valueOf(d.getHours());
      case 'I': case 'l': return String.valueOf(d.getHours() % 12 == 0 ? 12 : d.getHours() % 12);
      case 'j': return String.valueOf(getDayOfYear(d));
      case 'L': return String.valueOf(d.getMilliseconds());
      case 'm': return String.valueOf(d.getMonth() + 1);
      case 'M': return String.valueOf(d.getMinutes());
      case 'N': {
        int n = width == null || width.isEmpty() ? 9 : (int) Underscore.toNumber(Double.parseDouble(width));
        if (n == 0) n = 9;
        String str = JsString.padStart(String.valueOf(d.getMilliseconds()), 3, "0");
        str = str.length() > n ? str.substring(0, n) : str;
        if (memoryLimit != null) memoryLimit.use(n - str.length());
        return JsString.padEnd(str, n, "0");
      }
      case 'p': return d.getHours() < 12 ? "AM" : "PM";
      case 'P': return d.getHours() < 12 ? "am" : "pm";
      case 'q': return ordinal(d);
      case 's': return String.valueOf((long) Math.floor(d.dateValue() / 1000));
      case 'S': return String.valueOf(d.getSeconds());
      case 'u': return String.valueOf(d.getDay() == 0 ? 7 : d.getDay());
      case 'U': return getWeekOfYear(d, 0);
      case 'w': return String.valueOf(d.getDay());
      case 'W': return getWeekOfYear(d, 1);
      case 'x': return d.toLocaleDateString();
      case 'X': return d.toLocaleTimeString();
      case 'y': return String.valueOf((d.getFullYear() % 100 + 100) % 100);
      case 'Y': return String.valueOf(d.getFullYear());
      case 'z': return getTimezoneOffset(d, flags.containsKey(':'));
      case 'Z': {
        String name = d.getTimeZoneName();
        return name == null || name.isEmpty() ? getTimezoneOffset(d, flags.containsKey(':')) : name;
      }
      case 't': return "\t";
      case 'n': return "\n";
      case '%': return "%";
      default: return null;
    }
  }

  /** Not every value a conversion produces is a string in the source; this keeps them aligned. */
  static String stringOf (Object value) {
    return Js.toStringValue(value);
  }
}
