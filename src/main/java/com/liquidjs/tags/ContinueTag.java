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

/** Skips to the next turn of the loop it is written in. */
public class ContinueTag extends Tag {
  /** How the parser builds this tag. */
  public static final TagClass CLASS = ContinueTag::new;

  /** The continue tag, read from its token and the tokens that follow it. */
  public ContinueTag (TagToken token, List<Token> remainTokens, Liquid liquid, Parser parser) {
    super(token, remainTokens, liquid);
  }

  @Override
  public Object render (Context ctx, Emitter emitter) {
    ctx.continueCalled = true;
    return null;
  }
}
