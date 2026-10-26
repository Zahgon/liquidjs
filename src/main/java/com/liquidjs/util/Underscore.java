/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 Jun Yang
 */
package com.liquidjs.util;

import com.liquidjs.js.Js;
import com.liquidjs.js.JsJson;
import com.liquidjs.js.JsNumber;
import com.liquidjs.js.JsString;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/** The small helpers every layer of the engine shares. */
public final class Underscore {
  private Underscore () {}

  /** Render a value for output: a string stays, an absent value is empty, an array concatenates. */
  public static String stringify (Object input) {
    Object value = Js.toValue(input);
    if (Js.isString(value)) return (String) value;
    if (Js.isNil(value)) return "";
    if (Js.isArray(value)) {
      StringBuilder out = new StringBuilder();
      for (Object item : Js.asArray(value)) out.append(stringify(item));
      return out.toString();
    }
    return Js.toStringValue(value);
  }

  /** Read one element of an array, counting from the end for a negative index. */
  public static Object readArrayElement (List<Object> array, int index, boolean ownPropertyOnly) {
    int at = index < 0 ? array.size() + index : index;
    if (at < 0 || at >= array.size()) return Js.UNDEFINED;
    return array.get(at);
  }

  /** The elements a value iterates over: an object iterates as its key and value pairs. */
  public static List<Object> toEnumerable (Object input) {
    Object value = Js.toValue(input);
    if (Js.isArray(value)) return Js.asArray(value);
    if (Js.isString(value) && !((String) value).isEmpty()) {
      List<Object> single = new ArrayList<>();
      single.add(value);
      return single;
    }
    if (value instanceof Map) {
      Map<String, Object> object = Js.asObject(value);
      List<Object> pairs = new ArrayList<>();
      for (String key : Js.keys(object)) {
        List<Object> pair = new ArrayList<>();
        pair.add(key);
        pair.add(object.get(key));
        pairs.add(pair);
      }
      return pairs;
    }
    return new ArrayList<>();
  }

  /** A value as an array: an absent value is empty, anything else is wrapped. */
  public static List<Object> toArray (Object input) {
    Object value = Js.toValue(input);
    List<Object> out = new ArrayList<>();
    if (Js.isNil(value)) return out;
    if (Js.isArray(value)) return Js.asArray(value);
    out.add(value);
    return out;
  }

  /** A value as a number, where anything that is not a number counts as zero. */
  public static double toNumber (Object value) {
    double number = Js.toNumberValue(Js.toValue(value));
    return Double.isNaN(number) || number == 0 ? 0 : number;
  }

  /** The numbers from one bound up to another, the upper one excluded. */
  public static List<Object> range (double start, double stop) {
    return range(start, stop, 1);
  }

  /**
   * The numbers from one bound up to another, in steps. A bound that is not a number leaves the
   * list empty, because the loop that fills it never starts.
   */
  public static List<Object> range (double start, double stop, double step) {
    List<Object> out = new ArrayList<>();
    for (double i = start; i < stop; i += step) out.add(i);
    return out;
  }

  /** A value as a string, padded on the left to a length. */
  public static String padStart (Object value, int length, String pad) {
    return JsString.padStart(Js.toStringValue(value), length, pad);
  }

  /** A value as a string, padded on the right to a length. */
  public static String padEnd (Object value, int length, String pad) {
    return JsString.padEnd(Js.toStringValue(value), length, pad);
  }

  /** Upper case a string that has a lower case letter in it, lower case any other. */
  public static String changeCase (String str) {
    boolean hasLowerCase = false;
    for (int i = 0; i < str.length(); i++) {
      char ch = str.charAt(i);
      if (ch >= 'a' && ch <= 'z') {
        hasLowerCase = true;
        break;
      }
    }
    return hasLowerCase ? JsString.toUpperCase(str) : JsString.toLowerCase(str);
  }

  /** The string, shortened with three dots when it is longer than a maximum. */
  public static String ellipsis (String str, int max) {
    return str.length() > max ? str.substring(0, max - 3) + "..." : str;
  }

  /** Compare two values as the relational operators do, with absent values at the tail. */
  public static int orderedCompare (Object lhs, Object rhs) {
    if (Js.isNil(lhs) && Js.isNil(rhs)) return 0;
    if (Js.isNil(lhs)) return 1;
    if (Js.isNil(rhs)) return -1;
    if (Js.lessThan(lhs, rhs)) return -1;
    if (Js.greaterThan(lhs, rhs)) return 1;
    return 0;
  }

  /** Compare two values as lower case strings, with absent values at the tail. */
  public static int caseInsensitiveCompare (Object lhs, Object rhs) {
    if (Js.isNil(lhs) && Js.isNil(rhs)) return 0;
    if (Js.isNil(lhs)) return 1;
    if (Js.isNil(rhs)) return -1;
    String left = JsString.toLowerCase(Js.toStringValue(lhs));
    String right = JsString.toLowerCase(Js.toStringValue(rhs));
    int order = left.compareTo(right);
    return order < 0 ? -1 : order > 0 ? 1 : 0;
  }

  /** Unique elements, where two values are the same when they serialise the same. */
  public static List<Object> strictUniq (List<Object> array) {
    LinkedHashSet<String> seen = new LinkedHashSet<>();
    List<Object> out = new ArrayList<>();
    for (Object element : array) {
      String key = JsJson.stringify(element);
      if (seen.add(key == null ? "undefined" : key)) out.add(element);
    }
    return out;
  }

  /** Unique elements by the identity a set uses. */
  public static List<Object> uniq (List<Object> array) {
    List<Object> out = new ArrayList<>();
    for (Object element : array) {
      boolean found = false;
      for (Object kept : out) {
        if (Js.sameValueZero(kept, element)) {
          found = true;
          break;
        }
      }
      if (!found) out.add(element);
    }
    return out;
  }

  /** The last element of an array, or the absent value when it is empty. */
  public static Object last (List<Object> array) {
    return array.isEmpty() ? Js.UNDEFINED : array.get(array.size() - 1);
  }

  /** The number a template wrote, for a value the engine computed. */
  public static String numberToString (double value) {
    return JsNumber.toString(value);
  }
}
