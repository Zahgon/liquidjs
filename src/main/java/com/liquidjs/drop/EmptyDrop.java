/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 Jun Yang
 */
package com.liquidjs.drop;

import com.liquidjs.js.Js;

/** The empty literal, which equals any string, array or object with nothing in it. */
public class EmptyDrop extends Drop implements Comparable {
  @Override
  public boolean equalsValue (Object input) {
    if (input instanceof EmptyDrop) return false;
    Object value = Js.toValue(input);
    if (Js.isString(value)) return ((String) value).isEmpty();
    if (Js.isArray(value)) return Js.asArray(value).isEmpty();
    if (Js.isObject(value)) return Js.size(value) == 0;
    return false;
  }

  @Override
  public boolean gt (Object rhs) {
    return false;
  }

  @Override
  public boolean geq (Object rhs) {
    return false;
  }

  @Override
  public boolean lt (Object rhs) {
    return false;
  }

  @Override
  public boolean leq (Object rhs) {
    return false;
  }

  @Override
  public Object valueOf () {
    return "";
  }

  /** Whether a value is one of this engine's errors. */
  public static boolean is (Object value) {
    return value instanceof EmptyDrop;
  }
}
