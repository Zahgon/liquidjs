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

/** Drops everything up to its end tag. */
public class CommentTag extends Tag {
  public static final TagClass CLASS = CommentTag::new;

  public CommentTag (TagToken tagToken, List<Token> remainTokens, Liquid liquid, Parser parser) {
    super(tagToken, remainTokens, liquid);
    while (!remainTokens.isEmpty()) {
      Token token = remainTokens.remove(0);
      if (token.kind == com.liquidjs.parser.TokenKind.Tag && "endcomment".equals(((TagToken) token).name)) return;
    }
    throw new RuntimeException("tag " + tagToken.getText() + " not closed");
  }

  @Override
  public Object render (Context ctx, Emitter emitter) {
    return null;
  }
}
