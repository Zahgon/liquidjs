/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 Jun Yang
 */
package com.liquidjs.tokens;

import com.liquidjs.parser.TokenKind;

import java.util.List;

/** A path such as a.b[c].d, optionally rooted at a value rather than at the scope. */
public class PropertyAccessToken extends Token implements ValueToken {
  public final Token variable;
  public final List<Token> props;

  public PropertyAccessToken (Token variable, List<Token> props, String input, int begin, int end,
                              String file) {
    super(TokenKind.PropertyAccess, input, begin, end, file);
    this.variable = variable;
    this.props = props;
  }
}
