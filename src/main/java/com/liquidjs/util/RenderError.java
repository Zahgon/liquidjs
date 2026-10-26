/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 Jun Yang
 */
package com.liquidjs.util;

import com.liquidjs.template.Template;

/** Raised while rendering one template node. */
public class RenderError extends LiquidError {
  private static final long serialVersionUID = 1L;

  /** An error raised while rendering one node. */
  public RenderError (Throwable error, Template template) {
    super(error, template.getToken());
    this.name = "RenderError";
    setMessage(error.getMessage());
    update();
  }

  /** Whether a value is one of this engine's errors. */
  public static boolean is (Object value) {
    return value instanceof LiquidError && "RenderError".equals(((LiquidError) value).name);
  }
}
