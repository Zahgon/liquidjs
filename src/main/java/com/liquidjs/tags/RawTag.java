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

import java.util.ArrayList;
import java.util.List;

/** Writes its body as it was written, without reading any tag in it. */
public class RawTag extends Tag {
  /** How the parser builds this tag. */
  public static final TagClass CLASS = RawTag::new;

  private final List<Token> tokens = new ArrayList<>();

  /** The raw tag, read from its token and the tokens that follow it. */
  public RawTag (TagToken tagToken, List<Token> remainTokens, Liquid liquid, Parser parser) {
    super(tagToken, remainTokens, liquid);
    while (!remainTokens.isEmpty()) {
      Token token = remainTokens.remove(0);
      if (token.kind == com.liquidjs.parser.TokenKind.Tag && "endraw".equals(((TagToken) token).name)) return;
      this.tokens.add(token);
    }
    throw new RuntimeException("tag " + tagToken.getText() + " not closed");
  }

  @Override
  public Object render (Context ctx, Emitter emitter) {
    StringBuilder out = new StringBuilder();
    for (Token token : tokens) out.append(token.getText());
    return out.toString();
  }
}
