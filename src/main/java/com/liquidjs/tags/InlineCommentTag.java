/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 Jun Yang
 */
package com.liquidjs.tags;

import com.liquidjs.Liquid;
import com.liquidjs.context.Context;
import com.liquidjs.emitters.Emitter;
import com.liquidjs.parser.Parser;
import com.liquidjs.template.Tag;
import com.liquidjs.template.TagClass;
import com.liquidjs.tokens.TagToken;
import com.liquidjs.tokens.Token;

import java.util.List;
import java.util.regex.Pattern;

import com.liquidjs.js.JsString;

/** A comment written on one line, or on several lines that each start with a hash. */
public class InlineCommentTag extends Tag {
  public static final TagClass CLASS = InlineCommentTag::new;

  private static final Pattern NOT_A_COMMENT_LINE = Pattern.compile(
      "\n[" + JsString.WHITESPACE_CLASS + "]*[^#" + JsString.WHITESPACE_CLASS + "]");

  public InlineCommentTag (TagToken tagToken, List<Token> remainTokens, Liquid liquid,
                           Parser parser) {
    super(tagToken, remainTokens, liquid);
    if (NOT_A_COMMENT_LINE.matcher(tagToken.getArgs()).find()) {
      throw new RuntimeException("every line of an inline comment must start with a '#' character");
    }
  }

  @Override
  public Object render (Context ctx, Emitter emitter) {
    return null;
  }
}
