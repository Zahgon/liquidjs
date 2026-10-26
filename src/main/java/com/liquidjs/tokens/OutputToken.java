/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 Jun Yang
 */
package com.liquidjs.tokens;

import com.liquidjs.LiquidOptions;
import com.liquidjs.parser.TokenKind;

/** An output, written between the output delimiters. */
public class OutputToken extends DelimitedToken implements TopLevelToken {
  /** An output, written between the output delimiters. */
  public OutputToken (String input, int begin, int end, LiquidOptions options, String file) {
    super(TokenKind.Output, begin + options.outputDelimiterLeft.length(),
        end - options.outputDelimiterRight.length(), input, begin, end,
        options.trimOutputLeft, options.trimOutputRight, file);
  }
}
