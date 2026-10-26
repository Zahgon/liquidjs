/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 Jun Yang
 */
package com.liquidjs.util;

/**
 * Used internally where no token is known, so it cannot carry a position; the caller wraps it in
 * an UndefinedVariableError once it knows the token.
 */
public class InternalUndefinedVariableError extends RuntimeException {
  private static final long serialVersionUID = 1L;

  public final String variableName;
  public final String name = "InternalUndefinedVariableError";

  public InternalUndefinedVariableError (String variableName) {
    super("undefined variable: " + variableName);
    this.variableName = variableName;
  }
}
