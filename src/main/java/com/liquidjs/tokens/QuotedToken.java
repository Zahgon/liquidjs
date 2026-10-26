/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 Jun Yang
 */
package com.liquidjs.tokens;

import com.liquidjs.parser.TokenKind;
import com.liquidjs.render.StringLiteral;

/** A quoted string, with its escape sequences already read. */
public class QuotedToken extends Token implements ValueToken {
  public final String content;

  public QuotedToken (String input, int begin, int end, String file) {
    super(TokenKind.Quoted, input, begin, end, file);
    this.content = StringLiteral.parseStringLiteral(getText());
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
