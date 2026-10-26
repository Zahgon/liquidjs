/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 Jun Yang
 */
package com.liquidjs.tokens;

import com.liquidjs.parser.TokenKind;
import com.liquidjs.util.Literals;

/** One of the named literals: true, false, nil, null, empty or blank. */
public class LiteralToken extends Token implements ValueToken {
  /** The value the literal stands for. */
  public final Object content;
  /** The literal as it was written. */
  public final String literal;

  /** One of the named literals: true, false, nil, null, empty or blank. */
  public LiteralToken (String input, int begin, int end, String file) {
    super(TokenKind.Literal, input, begin, end, file);
    this.literal = getText();
    this.content = Literals.VALUES.get(this.literal);
  }

  @Override
  public boolean hasContent () {
    return true;
  }

  @Override
  public Object getContent () {
    return content;
  }
}
