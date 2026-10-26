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

  /** The path that was read and not found. */
  public final String variableName;
  /** The error kind, as the thrown value reports it. */
  public final String name = "InternalUndefinedVariableError";

  /** A variable that was read and not found, with no token to name. */
  public InternalUndefinedVariableError (String variableName) {
    super("undefined variable: " + variableName);
    this.variableName = variableName;
  }
}
