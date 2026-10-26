/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 Jun Yang
 */
package com.liquidjs.tokens;

import com.liquidjs.LiquidOptions;
import com.liquidjs.parser.TokenKind;
import com.liquidjs.parser.Tokenizer;

/** A tag, written between the tag delimiters. */
public class TagToken extends DelimitedToken implements TopLevelToken {
  public final String name;
  public final Tokenizer tokenizer;
  private final String argsText;
  private final boolean argsFollowTheCursor;

  public TagToken (String input, int begin, int end, LiquidOptions options, String file) {
    this(input, begin + options.tagDelimiterLeft.length(),
        end - options.tagDelimiterRight.length(), input, begin, end, options,
        options.trimTagLeft, options.trimTagRight, file, false, "illegal tag syntax, tag name expected");
  }

  /**
   * A tag whose content range is given rather than taken from the delimiters, and whose arguments
   * are read from wherever the cursor stands rather than captured once.
   */
  protected TagToken (String input, int contentBegin, int contentEnd, String sameInput, int begin,
                      int end, LiquidOptions options, boolean trimLeft, boolean trimRight,
                      String file, boolean argsFollowTheCursor, String nameExpected) {
    super(TokenKind.Tag, contentBegin, contentEnd, input, begin, end, trimLeft, trimRight, file);
    this.tokenizer = new Tokenizer(input, options.operators, file, this.contentRange,
        options.groupedExpressions);
    this.name = this.tokenizer.readTagName();
    this.tokenizer.assertTrue(!this.name.isEmpty(), nameExpected);
    this.tokenizer.skipBlank();
    this.argsFollowTheCursor = argsFollowTheCursor;
    this.argsText = argsFollowTheCursor
        ? null
        : slice(this.tokenizer.input, this.tokenizer.p, this.contentRange[1]);
  }

  /** The arguments the tag was written with. */
  public String getArgs () {
    if (argsFollowTheCursor) {
      return slice(tokenizer.input, tokenizer.p, contentRange[1]);
    }
    return argsText;
  }
}
