/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 Jun Yang
 */
package com.liquidjs.util;

import com.liquidjs.js.Js;
import com.liquidjs.js.JsDate;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A date with timezone information, as a Ruby date has.
 *
 * It keeps the instant it was built from and a second, shifted instant whose UTC fields read as
 * the wall clock of the display timezone, so every field accessor stays a plain lookup.
 */
public class LiquidDate {
  private static final long ONE_MINUTE = 60000L;

  /**
   * Both the ISO 8601 and the RFC 2822 forms have to be supported, as they are by the runtimes
   * this engine runs on.
   */
  private static final Pattern TIMEZONE_PATTERN = Pattern.compile("([zZ]|([+-])(\\d{2}):?(\\d{2}))$");

  private final int timezoneOffset;
  private final String timezoneName;
  private final double dateMillis;
  private final double displayMillis;
  private final String locale;
  /** True when the display timezone was given rather than taken from the host. */
  public final boolean timezoneFixed;

  private final JsDate.Fields fields;

  /** A date from a string, a timestamp or another date, displayed in a timezone. */
  public LiquidDate (Object init, String locale, Object timezone) {
    this.locale = locale;
    this.dateMillis = toMillis(init);
    this.timezoneFixed = !Js.isNil(timezone);
    Object zone = timezone;
    if (Js.isNil(zone)) {
      zone = (double) JsDate.localOffsetMinutes(this.dateMillis);
    }
    if (Js.isString(zone)) {
      this.timezoneName = (String) zone;
      this.timezoneOffset = JsDate.namedOffsetMinutes(this.timezoneName, this.dateMillis);
    } else {
      this.timezoneName = "";
      this.timezoneOffset = (int) Js.toNumberValue(zone);
    }
    double diff = (JsDate.localOffsetMinutes(this.dateMillis) - this.timezoneOffset) * (double) ONE_MINUTE;
    this.displayMillis = this.dateMillis + diff;
    this.fields = Double.isNaN(this.displayMillis) ? null : new JsDate.Fields(this.displayMillis);
  }

  private static double toMillis (Object init) {
    if (Js.isNumber(init)) return (Double) init;
    if (Js.isString(init)) return JsDate.parse((String) init);
    if (init instanceof LiquidDate) return ((LiquidDate) init).dateValue();
    if (init instanceof java.util.Date) return ((java.util.Date) init).getTime();
    return Double.NaN;
  }

  /** The display timestamp in milliseconds, shifted into the display timezone. */
  public double getTime () {
    return displayMillis;
  }

  /**
   * The underlying timestamp in milliseconds, unaffected by the display timezone. Use this, and
   * not getTime(), for a timezone-invariant value.
   */
  public double dateValue () {
    return dateMillis;
  }

  /** The milliseconds of the display wall clock. */
  public int getMilliseconds () {
    return fields.millisecond;
  }

  /** The seconds of the display wall clock. */
  public int getSeconds () {
    return fields.second;
  }

  /** The minutes of the display wall clock. */
  public int getMinutes () {
    return fields.minute;
  }

  /** The hours of the display wall clock. */
  public int getHours () {
    return fields.hour;
  }

  /** The day of the week of the display wall clock, Sunday being zero. */
  public int getDay () {
    return fields.weekday;
  }

  /** The day of the month of the display wall clock. */
  public int getDate () {
    return fields.day;
  }

  /** The month of the display wall clock, January being zero. */
  public int getMonth () {
    return fields.month;
  }

  /** The year of the display wall clock. */
  public int getFullYear () {
    return fields.year;
  }

  /** The date and time as the locale writes them. */
  public String toLocaleString () {
    return JsDate.toLocaleString(fields);
  }

  /** The time as the locale writes it. */
  public String toLocaleTimeString () {
    return JsDate.toLocaleTimeString(fields);
  }

  /** The date as the locale writes it. */
  public String toLocaleDateString () {
    return JsDate.toLocaleDateString(fields);
  }

  /** The display timezone offset in minutes west of UTC. */
  public int getTimezoneOffset () {
    return timezoneOffset;
  }

  /** The name of the display timezone. */
  public String getTimeZoneName () {
    if (timezoneFixed) return timezoneName;
    return JsDate.localTimezoneName();
  }

  /** The full name of the month. */
  public String getLongMonthName () {
    return JsDate.monthName(getMonth());
  }

  /** The short name of the month. */
  public String getShortMonthName () {
    return JsDate.shortMonthName(getMonth());
  }

  /** The full name of the weekday. */
  public String getLongWeekdayName () {
    return JsDate.weekdayName(getDay());
  }

  /** The short name of the weekday. */
  public String getShortWeekdayName () {
    return JsDate.shortWeekdayName(getDay());
  }

  /** The locale the names are written in. */
  public String getLocale () {
    return locale;
  }

  /** Whether the date was read successfully. */
  public boolean valid () {
    return !Double.isNaN(getTime());
  }

  /**
   * A date fixed to the timezone its text declares, so both 2021-08-06T02:29:00.000Z and
   * 2021-08-06T02:29:00.000+08:00 always display as 2021-08-06 02:29:00 whatever the host
   * timezone is.
   */
  public static LiquidDate createDateFixedToTimezone (String dateString, String locale) {
    Matcher m = TIMEZONE_PATTERN.matcher(dateString);
    if (m.find()) {
      String zone = m.group(1);
      if ("Z".equals(zone) || "z".equals(zone)) {
        return new LiquidDate(JsDate.parse(dateString), locale, 0.0);
      }
      if (m.group(2) != null && m.group(3) != null && m.group(4) != null) {
        int sign = "+".equals(m.group(2)) ? -1 : 1;
        double offset = sign * (Integer.parseInt(m.group(3)) * 60 + Integer.parseInt(m.group(4)));
        return new LiquidDate(JsDate.parse(dateString), locale, offset);
      }
    }
    return new LiquidDate(dateString, locale, null);
  }
}
