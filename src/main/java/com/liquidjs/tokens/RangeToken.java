/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 Jun Yang
 */
package com.liquidjs.tokens;

import com.liquidjs.parser.TokenKind;

/** A range such as (1..5), whose bounds are themselves values. */
public class RangeToken extends Token implements ValueToken {
  /** The lower bound of the range. */
  public final Token lhs;
  /** The upper bound of the range. */
  public final Token rhs;

  /** A range such as (1..5), whose bounds are themselves values. */
  public RangeToken (String input, int begin, int end, Token lhs, Token rhs, String file) {
    super(TokenKind.Range, input, begin, end, file);
    this.lhs = lhs;
    this.rhs = rhs;
  }
}
