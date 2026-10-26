/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 Jun Yang
 */
package com.liquidjs.tokens;

import com.liquidjs.parser.TokenKind;

/** One key and value pair of a tag's arguments. */
public class HashToken extends Token {
  public final IdentifierToken name;
  public final Token value;

  public HashToken (String input, int begin, int end, IdentifierToken name, Token value,
                    String file) {
    super(TokenKind.Hash, input, begin, end, file);
    this.name = name;
    this.value = value;
  }
}
