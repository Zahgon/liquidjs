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

/** Leaves the loop it is written in. */
public class BreakTag extends Tag {
  public static final TagClass CLASS = BreakTag::new;

  public BreakTag (TagToken token, List<Token> remainTokens, Liquid liquid, Parser parser) {
    super(token, remainTokens, liquid);
  }

  @Override
  public Object render (Context ctx, Emitter emitter) {
    ctx.breakCalled = true;
    return null;
  }
}
