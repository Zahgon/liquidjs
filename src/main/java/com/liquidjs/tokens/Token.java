/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 Jun Yang
 */
package com.liquidjs.tokens;

/** A range of the template input, with the kind of syntax it holds. */
public abstract class Token {
  public final int kind;
  public final String input;
  public int begin;
  public int end;
  public final String file;

  protected Token (int kind, String input, int begin, int end, String file) {
    this.kind = kind;
    this.input = input;
    this.begin = begin;
    this.end = end;
    this.file = file;
  }

  public String getText () {
    return slice(input, begin, end);
  }

  /** A slice of the input, with bounds clamped as the source language clamps them. */
  protected static String slice (String input, int from, int to) {
    int begin = Math.min(Math.max(from, 0), input.length());
    int end = Math.min(Math.max(to, 0), input.length());
    return end <= begin ? "" : input.substring(begin, end);
  }

  /** The one-based row and column the token starts at. */
  public int[] getPosition () {
    int row = 1;
    int col = 1;
    for (int i = 0; i < begin; i++) {
      if (input.charAt(i) == '\n') {
        row++;
        col = 1;
      } else {
        col++;
      }
    }
    return new int[] { row, col };
  }

  public int size () {
    return end - begin;
  }

  /** True when the token carries a value of its own, as a literal or a number does. */
  public boolean hasContent () {
    return false;
  }

  public Object getContent () {
    return null;
  }
}
