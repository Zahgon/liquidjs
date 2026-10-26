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

  public int getMilliseconds () {
    return fields.millisecond;
  }

  public int getSeconds () {
    return fields.second;
  }

  public int getMinutes () {
    return fields.minute;
  }

  public int getHours () {
    return fields.hour;
  }

  public int getDay () {
    return fields.weekday;
  }

  public int getDate () {
    return fields.day;
  }

  public int getMonth () {
    return fields.month;
  }

  public int getFullYear () {
    return fields.year;
  }

  public String toLocaleString () {
    return JsDate.toLocaleString(fields);
  }

  public String toLocaleTimeString () {
    return JsDate.toLocaleTimeString(fields);
  }

  public String toLocaleDateString () {
    return JsDate.toLocaleDateString(fields);
  }

  public int getTimezoneOffset () {
    return timezoneOffset;
  }

  public String getTimeZoneName () {
    if (timezoneFixed) return timezoneName;
    return JsDate.localTimezoneName();
  }

  public String getLongMonthName () {
    return JsDate.monthName(getMonth());
  }

  public String getShortMonthName () {
    return JsDate.shortMonthName(getMonth());
  }

  public String getLongWeekdayName () {
    return JsDate.weekdayName(getDay());
  }

  public String getShortWeekdayName () {
    return JsDate.shortWeekdayName(getDay());
  }

  public String getLocale () {
    return locale;
  }

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
