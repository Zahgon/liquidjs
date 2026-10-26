/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 Jun Yang
 */
package com.liquidjs.js;

import com.liquidjs.drop.Drop;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The value model of the source runtime: an absent value and a null value are distinct, every
 * number is a double, an object keeps the insertion order of its keys except for array indexes,
 * and comparison and conversion follow the abstract operations of the language specification.
 */
public final class Js {
  private Js () {}

  /** The absent value, distinct from {@code null}. */
  public static final Object UNDEFINED = new Object() {
    @Override public String toString () {
      return "undefined";
    }
  };

  /** Whether a value is the absent value. */
  public static boolean isUndefined (Object value) {
    return value == UNDEFINED;
  }

  /** True for the absent value and for null, as {@code value == null} is in the source. */
  public static boolean isNil (Object value) {
    return value == null || value == UNDEFINED;
  }

  /** Whether a value is a string. */
  public static boolean isString (Object value) {
    return value instanceof String;
  }

  /** Whether a value is a number. */
  public static boolean isNumber (Object value) {
    return value instanceof Double;
  }

  /** Whether a value is a boolean. */
  public static boolean isBoolean (Object value) {
    return value instanceof Boolean;
  }

  /** A value as the array it is known to be. */
  @SuppressWarnings("unchecked")
  public static List<Object> asArray (Object value) {
    return (List<Object>) value;
  }

  /** A value as the object it is known to be. */
  @SuppressWarnings("unchecked")
  public static Map<String, Object> asObject (Object value) {
    return (Map<String, Object>) value;
  }

  /** Whether a value is an array. */
  public static boolean isArray (Object value) {
    return value instanceof List;
  }

  /** True for arrays, plain objects, drops and every other non-primitive. */
  public static boolean isObject (Object value) {
    return value != null && value != UNDEFINED
        && !(value instanceof String) && !(value instanceof Double) && !(value instanceof Boolean);
  }

  /** True when the value has a numeric length, as an array or a string has. */
  public static boolean isArrayLike (Object value) {
    return value instanceof List || value instanceof String;
  }

  /** Unwrap a drop to the value it stands for. */
  public static Object toValue (Object value) {
    return value instanceof Drop ? ((Drop) value).valueOf() : value;
  }

  /** A fresh object that keeps the order its keys were inserted in. */
  public static Map<String, Object> newObject () {
    return new LinkedHashMap<>();
  }

  /**
   * Own enumerable keys in the order the language enumerates them: array indexes first, in
   * ascending numeric order, then the remaining keys in insertion order.
   */
  public static List<String> keys (Map<String, Object> object) {
    List<String> indexes = new ArrayList<>();
    List<String> rest = new ArrayList<>();
    for (String key : object.keySet()) {
      if (isArrayIndex(key)) indexes.add(key);
      else rest.add(key);
    }
    indexes.sort((a, b) -> Long.compare(Long.parseLong(a), Long.parseLong(b)));
    indexes.addAll(rest);
    return indexes;
  }

  /** True for the canonical decimal form of an integer below 2^32-1. */
  public static boolean isArrayIndex (String key) {
    if (key.isEmpty() || key.length() > 10) return false;
    if (key.charAt(0) == '0' && key.length() > 1) return false;
    for (int i = 0; i < key.length(); i++) {
      if (key.charAt(i) < '0' || key.charAt(i) > '9') return false;
    }
    long value = Long.parseLong(key);
    return value < 4294967295L;
  }

  /** ToString, the conversion an interpolated value goes through. */
  public static String toStringValue (Object value) {
    if (value == null) return "null";
    if (value == UNDEFINED) return "undefined";
    if (value instanceof String) return (String) value;
    if (value instanceof Double) return JsNumber.toString((Double) value);
    if (value instanceof Boolean) return value.toString();
    if (value instanceof Drop) {
      Object unwrapped = ((Drop) value).valueOf();
      return unwrapped == value ? "[object Object]" : toStringValue(unwrapped);
    }
    if (value instanceof List) {
      StringBuilder out = new StringBuilder();
      List<Object> array = asArray(value);
      for (int i = 0; i < array.size(); i++) {
        if (i > 0) out.append(',');
        Object item = array.get(i);
        if (!isNil(item)) out.append(toStringValue(item));
      }
      return out.toString();
    }
    return "[object Object]";
  }

  /** ToNumber. */
  public static double toNumberValue (Object value) {
    if (value == null) return 0;
    if (value == UNDEFINED) return Double.NaN;
    if (value instanceof Double) return (Double) value;
    if (value instanceof Boolean) return ((Boolean) value) ? 1 : 0;
    if (value instanceof String) return JsNumber.parse((String) value);
    Object primitive = toPrimitive(value);
    if (primitive == value) return Double.NaN;
    return toNumberValue(primitive);
  }

  /** ToPrimitive with no hint: a drop unwraps, an array and an object stringify. */
  public static Object toPrimitive (Object value) {
    if (!isObject(value)) return value;
    if (value instanceof Drop) {
      Object unwrapped = ((Drop) value).valueOf();
      if (unwrapped != value) return unwrapped;
    }
    if (value instanceof List) {
      List<Object> array = asArray(value);
      if (array.isEmpty()) return "";
      return toStringValue(value);
    }
    if (value instanceof Map) return "[object Object]";
    return value;
  }

  /** Strict equality. */
  public static boolean strictEquals (Object lhs, Object rhs) {
    if (lhs == null || rhs == null || lhs == UNDEFINED || rhs == UNDEFINED) return lhs == rhs;
    if (lhs instanceof Double && rhs instanceof Double) {
      double l = (Double) lhs;
      double r = (Double) rhs;
      return l == r;
    }
    if (lhs instanceof String && rhs instanceof String) return lhs.equals(rhs);
    if (lhs instanceof Boolean && rhs instanceof Boolean) return lhs.equals(rhs);
    return lhs == rhs;
  }

  /** SameValueZero, the equality a set uses to decide membership. */
  public static boolean sameValueZero (Object lhs, Object rhs) {
    if (lhs instanceof Double && rhs instanceof Double) {
      double l = (Double) lhs;
      double r = (Double) rhs;
      if (Double.isNaN(l) && Double.isNaN(r)) return true;
      return l == r;
    }
    return strictEquals(lhs, rhs);
  }

  /** Loose equality, which coerces across types. */
  public static boolean looseEquals (Object lhs, Object rhs) {
    if (isNil(lhs) || isNil(rhs)) return isNil(lhs) && isNil(rhs);
    boolean lhsPrimitive = !isObject(lhs);
    boolean rhsPrimitive = !isObject(rhs);
    if (lhsPrimitive && rhsPrimitive) {
      if (lhs instanceof String && rhs instanceof String) return lhs.equals(rhs);
      if (lhs instanceof Boolean || rhs instanceof Boolean
          || lhs instanceof Double || rhs instanceof Double) {
        double l = toNumberValue(lhs);
        double r = toNumberValue(rhs);
        return l == r;
      }
      return strictEquals(lhs, rhs);
    }
    if (!lhsPrimitive && !rhsPrimitive) return lhs == rhs;
    Object l = lhsPrimitive ? lhs : toPrimitive(lhs);
    Object r = rhsPrimitive ? rhs : toPrimitive(rhs);
    return looseEquals(l, r);
  }

  /**
   * The abstract relational comparison: two strings compare by their code units, anything else
   * compares as numbers, and a comparison with a not-a-number result is always false.
   */
  public static boolean lessThan (Object lhs, Object rhs) {
    Object l = toPrimitive(lhs);
    Object r = toPrimitive(rhs);
    if (l instanceof String && r instanceof String) return ((String) l).compareTo((String) r) < 0;
    double ln = toNumberValue(l);
    double rn = toNumberValue(r);
    if (Double.isNaN(ln) || Double.isNaN(rn)) return false;
    return ln < rn;
  }

  /** The abstract relational comparison, the other way round. */
  public static boolean greaterThan (Object lhs, Object rhs) {
    return lessThan(rhs, lhs);
  }

  /** The abstract relational comparison, allowing equality. */
  public static boolean lessOrEqual (Object lhs, Object rhs) {
    Object l = toPrimitive(lhs);
    Object r = toPrimitive(rhs);
    if (l instanceof String && r instanceof String) return ((String) l).compareTo((String) r) <= 0;
    double ln = toNumberValue(l);
    double rn = toNumberValue(r);
    if (Double.isNaN(ln) || Double.isNaN(rn)) return false;
    return ln <= rn;
  }

  /** The abstract relational comparison the other way round, allowing equality. */
  public static boolean greaterOrEqual (Object lhs, Object rhs) {
    return lessOrEqual(rhs, lhs);
  }

  /** ToBoolean. */
  public static boolean truthy (Object value) {
    if (value == null || value == UNDEFINED) return false;
    if (value instanceof Boolean) return (Boolean) value;
    if (value instanceof Double) {
      double number = (Double) value;
      return number != 0 && !Double.isNaN(number);
    }
    if (value instanceof String) return !((String) value).isEmpty();
    return true;
  }

  /** The number of elements a value reports, for an array, a string or an object. */
  public static int size (Object value) {
    if (value instanceof String) return ((String) value).length();
    if (value instanceof List) return asArray(value).size();
    if (value instanceof Map) return asObject(value).size();
    if (value instanceof Collection) return ((Collection<?>) value).size();
    return 0;
  }

  /** A number as the boxed double the value model uses. */
  public static Double number (double value) {
    return value;
  }
}
