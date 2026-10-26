/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 Jun Yang
 */
package com.liquidjs.util;

/** Raised by an assertion that did not hold; it carries no token of its own. */
public class AssertionError extends RuntimeException {
  private static final long serialVersionUID = 1L;

  public final String name = "AssertionError";

  public AssertionError (String message) {
    super(message);
  }
}
