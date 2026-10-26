/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 Jun Yang
 */
package com.liquidjs.util;

import com.liquidjs.tokens.Token;

/** Raised while reading tokens out of a template. */
public class TokenizationError extends LiquidError {
  private static final long serialVersionUID = 1L;

  /** An error raised while reading tokens out of a template. */
  public TokenizationError (String message, Token token) {
    super(message, token);
    this.name = "TokenizationError";
    update();
  }
}
