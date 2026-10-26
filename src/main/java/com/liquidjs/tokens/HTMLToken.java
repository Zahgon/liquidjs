/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 Jun Yang
 */
package com.liquidjs.tokens;

import com.liquidjs.parser.TokenKind;

/** Template text outside any tag or output, with the blanks whitespace control takes off it. */
public class HTMLToken extends Token implements TopLevelToken {
  public int trimLeft = 0;
  public int trimRight = 0;

  public HTMLToken (String input, int begin, int end, String file) {
    super(TokenKind.HTML, input, begin, end, file);
  }

  public String getContentText () {
    return slice(input, begin + trimLeft, end - trimRight);
  }
}
