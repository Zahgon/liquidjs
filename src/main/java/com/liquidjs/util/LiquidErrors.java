/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 Jun Yang
 */
package com.liquidjs.util;

import java.util.List;

/** Every error collected in one parse or render call, when errors are caught instead of thrown. */
public class LiquidErrors extends LiquidError {
  private static final long serialVersionUID = 1L;

  /** The errors this one stands for, in the order they were raised. */
  public final List<LiquidError> errors;

  public LiquidErrors (List<LiquidError> errors) {
    super(errors.get(0), errors.get(0).getToken());
    this.errors = errors;
    this.name = "LiquidErrors";
    String plural = errors.size() > 1 ? "s" : "";
    setMessage(errors.size() + " error" + plural + " found");
    update();
  }

  public static boolean is (Object value) {
    return value instanceof LiquidError && "LiquidErrors".equals(((LiquidError) value).name);
  }
}
