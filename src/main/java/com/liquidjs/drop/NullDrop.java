/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 Jun Yang
 */
package com.liquidjs.drop;

import com.liquidjs.js.Js;

/** The nil literal, which equals an absent value and null. */
public class NullDrop extends Drop implements Comparable {
  @Override
  public boolean equalsValue (Object value) {
    return Js.isNil(Js.toValue(value));
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
    return null;
  }
}
