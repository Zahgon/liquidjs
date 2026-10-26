/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 Jun Yang
 */
package com.liquidjs.filters;

import com.liquidjs.LiquidOptions;
import com.liquidjs.js.Js;
import com.liquidjs.template.FilterHandler;
import com.liquidjs.template.FilterImpl;
import com.liquidjs.util.LiquidDate;
import com.liquidjs.util.Strftime;
import com.liquidjs.util.Underscore;

import java.util.Map;

/** The filters that format a date. */
public final class DateFilters {
  private DateFilters () {}

  /** Register these filters under the names a template calls them by. */
  public static void register (Map<String, Object> filters) {
    filters.put("date", (FilterHandler) (impl, v, args) ->
        date(impl, v, Args.at(args, 0), Args.at(args, 1)));
    filters.put("date_to_xmlschema", (FilterHandler) (impl, v, args) ->
        date(impl, v, "%Y-%m-%dT%H:%M:%S%:z", Js.UNDEFINED));
    filters.put("date_to_rfc822", (FilterHandler) (impl, v, args) ->
        date(impl, v, "%a, %d %b %Y %H:%M:%S %z", Js.UNDEFINED));
    filters.put("date_to_string", (FilterHandler) (impl, v, args) ->
        stringifyDate(impl, v, "%b", Args.at(args, 0), Args.at(args, 1)));
    filters.put("date_to_long_string", (FilterHandler) (impl, v, args) ->
        stringifyDate(impl, v, "%B", Args.at(args, 0), Args.at(args, 1)));
  }

  static Object date (FilterImpl impl, Object v, Object formatArg, Object timezoneOffset) {
    int size = (Js.isString(Js.toValue(v)) ? ((String) Js.toValue(v)).length() : 0)
        + (Js.isString(timezoneOffset) ? ((String) timezoneOffset).length() : 0);
    impl.context.memoryLimit.use(size);
    LiquidDate date = parseDate(v, impl.context.opts, timezoneOffset);
    if (date == null) return v;
    Object format = Js.toValue(formatArg);
    String formatStr = Js.isNil(format)
        ? impl.context.opts.dateFormat
        : Underscore.stringify(format);
    impl.context.memoryLimit.use(formatStr.length());
    return Strftime.strftime(date, formatStr, impl.context.memoryLimit);
  }

  private static Object stringifyDate (FilterImpl impl, Object v, String monthType, Object type,
                                       Object style) {
    LiquidDate date = parseDate(v, impl.context.opts, Js.UNDEFINED);
    if (date == null) return v;
    com.liquidjs.util.Limiter ml = impl.context.memoryLimit;
    if ("ordinal".equals(type)) {
      int d = date.getDate();
      return "US".equals(style)
          ? Strftime.strftime(date, monthType + " " + d + "%q, %Y", ml)
          : Strftime.strftime(date, d + "%q " + monthType + " %Y", ml);
    }
    return Strftime.strftime(date, "%d " + monthType + " %Y", ml);
  }

  private static LiquidDate parseDate (Object input, LiquidOptions opts, Object timezoneOffset) {
    LiquidDate date;
    Object defaultTimezoneOffset = Js.isUndefined(timezoneOffset) || timezoneOffset == null
        ? opts.timezoneOffset
        : timezoneOffset;
    String locale = opts.locale;
    Object v = Js.toValue(input);
    if (Js.isNil(v)) {
      return null;
    } else if ("now".equals(v) || "today".equals(v)) {
      date = new LiquidDate((double) System.currentTimeMillis(), locale, defaultTimezoneOffset);
    } else if (Js.isNumber(v)) {
      date = new LiquidDate((Double) v * 1000, locale, defaultTimezoneOffset);
    } else if (Js.isString(v)) {
      String str = (String) v;
      if (str.matches("\\d+")) {
        date = new LiquidDate(com.liquidjs.js.JsNumber.parse(str) * 1000, locale,
            defaultTimezoneOffset);
      } else if (opts.preserveTimezones && Js.isUndefined(timezoneOffset)) {
        date = LiquidDate.createDateFixedToTimezone(str, locale);
      } else {
        date = new LiquidDate(str, locale, defaultTimezoneOffset);
      }
    } else {
      date = new LiquidDate(v, locale, defaultTimezoneOffset);
    }
    return date.valid() ? date : null;
  }
}
