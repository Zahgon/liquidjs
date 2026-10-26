/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 Jun Yang
 */
package com.liquidjs.util;

import com.liquidjs.js.Js;
import com.liquidjs.js.JsJson;

import java.util.function.Supplier;

/** The assertions the engine states its preconditions with. */
public final class Assertions {
  private Assertions () {}

  public static void assertTrue (boolean predicate, String message) {
    if (!predicate) throw new AssertionError(message);
  }

  public static void assertTrue (boolean predicate, Supplier<String> message) {
    if (!predicate) throw new AssertionError(message.get());
  }

  public static void assertTruthy (Object value, String message) {
    if (!Js.truthy(value)) throw new AssertionError(message);
  }

  /** Assert that a tag took no arguments, naming what it was given when it did. */
  public static void assertEmpty (Object value) {
    String serialised = Js.isNil(value) ? "undefined" : JsJson.stringify(value);
    assertEmpty(value, "unexpected " + serialised);
  }

  public static void assertEmpty (Object value, String message) {
    assertTrue(!Js.truthy(value), message);
  }
}
