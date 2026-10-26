/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 Jun Yang
 */
package com.liquidjs.util;

import com.liquidjs.tokens.Token;

/** Raised when a variable is read that the scope does not define and strict variables are on. */
public class UndefinedVariableError extends LiquidError {
  private static final long serialVersionUID = 1L;

  public UndefinedVariableError (Throwable error, Token token) {
    super(error, token);
    this.name = "UndefinedVariableError";
    setMessage(error.getMessage());
    update();
  }
}
