/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 Jun Yang
 */
package com.liquidjs.filters;

import com.liquidjs.js.Js;
import com.liquidjs.js.JsNumber;
import com.liquidjs.template.FilterHandler;
import com.liquidjs.util.Underscore;

import java.util.Map;

/** The arithmetic filters, which read every argument as a number. */
public final class MathFilters {
  private MathFilters () {}

  /** Register these filters under the names a template calls them by. */
  public static void register (Map<String, Object> filters) {
    filters.put("abs", (FilterHandler) (impl, v, args) -> Math.abs(Underscore.toNumber(v)));
    filters.put("at_least", (FilterHandler) (impl, v, args) -> args.isEmpty()
        ? Underscore.toNumber(v)
        : maximum(Underscore.toNumber(v), Args.number(args, 0)));
    filters.put("at_most", (FilterHandler) (impl, v, args) -> args.isEmpty()
        ? Underscore.toNumber(v)
        : minimum(Underscore.toNumber(v), Args.number(args, 0)));
    filters.put("ceil", (FilterHandler) (impl, v, args) -> Math.ceil(Underscore.toNumber(v)));
    filters.put("floor", (FilterHandler) (impl, v, args) -> Math.floor(Underscore.toNumber(v)));
    filters.put("divided_by", (FilterHandler) (impl, v, args) -> {
      double dividend = Underscore.toNumber(v);
      double divisor = Args.numberOrNaN(args, 0);
      boolean integerArithmetic = args.size() > 1 && Args.number(args, 1) != 0;
      return integerArithmetic ? Math.floor(dividend / divisor) : dividend / divisor;
    });
    filters.put("minus", (FilterHandler) (impl, v, args) ->
        Underscore.toNumber(v) - Args.numberOrNaN(args, 0));
    filters.put("plus", (FilterHandler) (impl, v, args) ->
        Underscore.toNumber(v) + Args.numberOrNaN(args, 0));
    filters.put("modulo", (FilterHandler) (impl, v, args) -> {
      double value = Underscore.toNumber(v);
      double arg = Args.numberOrNaN(args, 0);
      return ((value % arg) + arg) % arg;
    });
    filters.put("times", (FilterHandler) (impl, v, args) ->
        Underscore.toNumber(v) * Args.numberOrNaN(args, 0));
    filters.put("round", (FilterHandler) (impl, v, args) -> {
      double value = Underscore.toNumber(v);
      double arg = Args.number(args, 0);
      double amp = Math.pow(10, arg);
      double scaled = (value * amp) * (1 + Math.ulp(1.0));
      return JsNumber.round(scaled) / amp;
    });
  }

  /** The larger of two numbers, which is not a number when either of them is. */
  private static double maximum (double lhs, double rhs) {
    if (Double.isNaN(lhs) || Double.isNaN(rhs)) return Double.NaN;
    return Math.max(lhs, rhs);
  }

  /** The smaller of two numbers, which is not a number when either of them is. */
  private static double minimum (double lhs, double rhs) {
    if (Double.isNaN(lhs) || Double.isNaN(rhs)) return Double.NaN;
    return Math.min(lhs, rhs);
  }

  /** The smallest difference between one and the next representable number. */
  static double epsilon () {
    return Math.ulp(1.0);
  }

  static Object number (double value) {
    return Js.number(value);
  }
}
