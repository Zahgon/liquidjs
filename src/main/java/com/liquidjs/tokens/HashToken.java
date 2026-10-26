/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 Jun Yang
 */
package com.liquidjs.tokens;

import com.liquidjs.parser.TokenKind;

/** One key and value pair of a tag's arguments. */
public class HashToken extends Token {
  /** The key the argument was written under. */
  public final IdentifierToken name;
  /** The value of the argument. */
  public final Token value;

  /** One key and value argument of a tag. */
  public HashToken (String input, int begin, int end, IdentifierToken name, Token value,
                    String file) {
    super(TokenKind.Hash, input, begin, end, file);
    this.name = name;
    this.value = value;
  }
}
