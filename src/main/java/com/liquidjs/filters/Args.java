/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 Jun Yang
 */
package com.liquidjs.filters;

import com.liquidjs.js.Js;
import com.liquidjs.util.Underscore;

import java.util.List;

/** Reading a filter's arguments, where an argument that was not written is absent. */
final class Args {
  private Args () {}

  static Object at (List<Object> args, int index) {
    return index < args.size() ? args.get(index) : Js.UNDEFINED;
  }

  /** An argument unwrapped from any drop, as the value filters receive is. */
  static Object value (List<Object> args, int index) {
    return Js.toValue(at(args, index));
  }

  static double number (List<Object> args, int index) {
    return Underscore.toNumber(at(args, index));
  }

  /**
   * An argument read as a number, where an argument that was not written stays not-a-number
   * rather than becoming zero: the arithmetic filters convert what they were given, no more.
   */
  static double numberOrNaN (List<Object> args, int index) {
    if (index >= args.size()) return Double.NaN;
    return Underscore.toNumber(args.get(index));
  }

  static double number (List<Object> args, int index, double fallback) {
    Object arg = at(args, index);
    return Js.isUndefined(arg) ? fallback : Underscore.toNumber(arg);
  }

  static String string (List<Object> args, int index) {
    return Underscore.stringify(at(args, index));
  }

  static String string (List<Object> args, int index, String fallback) {
    Object arg = at(args, index);
    return Js.isUndefined(arg) ? fallback : Underscore.stringify(arg);
  }

  /** The pairs a filter was given as key and value arguments. */
  static Object keyValue (List<Object> args, String key) {
    for (Object arg : args) {
      if (arg instanceof List) {
        List<Object> pair = Js.asArray(arg);
        if (pair.size() == 2 && key.equals(pair.get(0))) return pair.get(1);
      }
    }
    return Js.UNDEFINED;
  }
}
