/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 Jun Yang
 */
package com.liquidjs.tokens;

import com.liquidjs.parser.TokenKind;

/** Template text outside any tag or output, with the blanks whitespace control takes off it. */
public class HTMLToken extends Token implements TopLevelToken {
  /** How many code units are trimmed from the start of this text. */
  public int trimLeft = 0;
  /** How many code units are trimmed from the end of this text. */
  public int trimRight = 0;

  /** A run of template text outside any tag or output. */
  public HTMLToken (String input, int begin, int end, String file) {
    super(TokenKind.HTML, input, begin, end, file);
  }

  /** The content of the token, without its delimiters or trim markers. */
  public String getContentText () {
    return slice(input, begin + trimLeft, end - trimRight);
  }
}
