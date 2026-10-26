/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 Jun Yang
 */
package com.liquidjs.render;

import com.liquidjs.context.Context;
import com.liquidjs.js.Js;

/** Truthiness: Liquid's own by default, the host language's when that option is set. */
public final class Booleans {
  private Booleans () {}

  /** Whether a value counts as true in a condition. */
  public static boolean isTruthy (Object value, Context ctx) {
    return !isFalsy(value, ctx);
  }

  /** Whether a value counts as false in a condition. */
  public static boolean isFalsy (Object input, Context ctx) {
    Object value = Js.toValue(input);
    if (ctx != null && ctx.opts.jsTruthy) {
      return !Js.truthy(value);
    }
    return Boolean.FALSE.equals(value) || Js.isNil(value);
  }
}
