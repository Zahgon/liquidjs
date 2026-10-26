/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 Jun Yang
 */
package com.liquidjs.util;

import com.liquidjs.drop.BlankDrop;
import com.liquidjs.drop.EmptyDrop;
import com.liquidjs.drop.NullDrop;
import com.liquidjs.js.Js;

import java.util.Map;

/** The literals an expression may name, and the value each one stands for. */
public final class Literals {
  private Literals () {}

  private static final NullDrop NIL = new NullDrop();

  /** The named literals, and the value each one stands for. */
  public static final Map<String, Object> VALUES = literalValues();

  private static Map<String, Object> literalValues () {
    Map<String, Object> values = Js.newObject();
    values.put("true", Boolean.TRUE);
    values.put("false", Boolean.FALSE);
    values.put("nil", NIL);
    values.put("null", NIL);
    values.put("empty", new EmptyDrop());
    values.put("blank", new BlankDrop());
    return values;
  }
}
