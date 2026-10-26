/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 Jun Yang
 */
package com.liquidjs.tokens;

import com.liquidjs.LiquidOptions;

/** One line of a liquid tag, which is a tag written without its delimiters. */
public class LiquidTagToken extends TagToken {
  public LiquidTagToken (String input, int begin, int end, LiquidOptions options, String file) {
    super(input, begin, end, input, begin, end, options, false, false, file, true,
        "illegal liquid tag syntax");
  }
}
