/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 Jun Yang
 */
package com.liquidjs.util;

/** A counter that raises once the resource it tracks passes its limit. */
public class Limiter {
  private final String message;
  private final double limit;
  private double base = 0;

  /** A counter over one resource, raising once it passes its limit. */
  public Limiter (String resource, double limit) {
    this.message = resource + " limit exceeded";
    this.limit = limit;
  }

  /** Charge an amount, raising when the total passes the limit. */
  public void use (double count) {
    if (count > 0) {
      Assertions.assertTrue(this.base + count <= this.limit, this.message);
      this.base += count;
    }
  }

  /** Raise when one amount alone passes the limit. */
  public void check (double count) {
    if (count > 0) {
      Assertions.assertTrue(count <= this.limit, this.message);
    }
  }
}
