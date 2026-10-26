/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 Jun Yang
 */
package com.liquidjs.drop;

/** A value that compares itself, so the operators ask it rather than coerce it. */
public interface Comparable {
  boolean equalsValue (Object rhs);

  boolean gt (Object rhs);

  boolean geq (Object rhs);

  boolean lt (Object rhs);

  boolean leq (Object rhs);
}
