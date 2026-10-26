/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 Jun Yang
 */
package com.liquidjs.util;

import com.liquidjs.js.JsString;
import com.liquidjs.tokens.Token;

/**
 * The base of every error that carries the token it was raised at, so its message ends with the
 * file, line and column, and its context shows the offending line with a caret under it.
 */
public abstract class LiquidError extends RuntimeException {
  private static final long serialVersionUID = 1L;

  /** The error class name, as the thrown value reports it. */
  public String name = "LiquidError";
  /** The lines around the token, with a caret under the column. */
  public String context = "";
  /** The error this one wraps, when it wraps one. */
  public Throwable originalError;

  private final Token token;
  private String message;

  protected LiquidError (String message, Token token) {
    super(message);
    this.message = message;
    this.token = token;
  }

  protected LiquidError (Throwable error, Token token) {
    super(error.getMessage());
    this.message = error.getMessage();
    this.originalError = error;
    this.token = token;
  }

  public Token getToken () {
    return token;
  }

  @Override
  public String getMessage () {
    return message;
  }

  protected void setMessage (String message) {
    this.message = message;
  }

  /** Fill in the context block and extend the message with the token's position. */
  protected void update () {
    this.context = makeContext(token);
    this.message = makeMessage(this.message, token);
  }

  public static boolean is (Object value) {
    return value instanceof LiquidError;
  }

  private static String makeContext (Token token) {
    int[] position = token.getPosition();
    int line = position[0];
    int col = position[1];
    String[] lines = JsString.split(token.input, "\n").toArray(new String[0]);
    int begin = Math.max(line - 2, 1);
    int end = Math.min(line + 3, lines.length);

    StringBuilder out = new StringBuilder();
    for (int lineNumber = begin; lineNumber <= end; lineNumber++) {
      if (lineNumber > begin) out.append('\n');
      String rowIndicator = lineNumber == line ? ">> " : "   ";
      String num = JsString.padStart(String.valueOf(lineNumber), String.valueOf(end).length(), " ");
      String text = rowIndicator + num + "| ";
      String colIndicator = lineNumber == line
          ? "\n" + JsString.padStart("^", col + text.length(), " ")
          : "";
      out.append(text).append(lines[lineNumber - 1]).append(colIndicator);
    }
    return out.toString();
  }

  private static String makeMessage (String message, Token token) {
    String out = message;
    if (token.file != null && !token.file.isEmpty()) out += ", file:" + token.file;
    int[] position = token.getPosition();
    out += ", line:" + position[0] + ", col:" + position[1];
    return out;
  }
}
