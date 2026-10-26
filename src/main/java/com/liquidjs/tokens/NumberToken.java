/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 Jun Yang
 */
package com.liquidjs.tokens;

import com.liquidjs.js.JsNumber;
import com.liquidjs.parser.TokenKind;

/** A number literal. */
public class NumberToken extends Token implements ValueToken {
  public final Double content;

  public NumberToken (String input, int begin, int end, String file) {
    super(TokenKind.Number, input, begin, end, file);
    this.content = JsNumber.parse(getText());
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
