/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 Jun Yang
 */
package com.liquidjs.util;

import com.liquidjs.tokens.Token;

/** Raised while building a template out of its tokens. */
public class ParseError extends LiquidError {
  private static final long serialVersionUID = 1L;

  /** An error raised while building a template out of its tokens. */
  public ParseError (Throwable error, Token token) {
    super(error, token);
    this.name = "ParseError";
    setMessage(error.getMessage());
    update();
  }
}
