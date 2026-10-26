/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 Jun Yang
 */
package com.liquidjs.drop;

/** A value that compares itself, so the operators ask it rather than coerce it. */
public interface Comparable {
  /** Whether this value equals another. */
  boolean equalsValue (Object rhs);

  /** Whether this value is greater than another. */
  boolean gt (Object rhs);

  /** Whether this value is greater than or equal to another. */
  boolean geq (Object rhs);

  /** Whether this value is less than another. */
  boolean lt (Object rhs);

  /** Whether this value is less than or equal to another. */
  boolean leq (Object rhs);
}
