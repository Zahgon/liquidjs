/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 Jun Yang
 */
package com.liquidjs.tokens;

import com.liquidjs.parser.TokenKind;

/** A bare word, used as a variable name, a tag name or a property. */
public class IdentifierToken extends Token implements ValueToken {
  /** The text the token stands for. */
  public final String content;

  /** A bare word, used as a name, a tag or a property. */
  public IdentifierToken (String input, int begin, int end, String file) {
    super(TokenKind.Word, input, begin, end, file);
    this.content = getText();
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
