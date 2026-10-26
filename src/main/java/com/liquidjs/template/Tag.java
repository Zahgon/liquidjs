/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 Jun Yang
 */
package com.liquidjs.template;

import com.liquidjs.Liquid;
import com.liquidjs.parser.Tokenizer;
import com.liquidjs.tokens.TagToken;
import com.liquidjs.tokens.Token;

import java.util.List;

/** The base of every tag. */
public abstract class Tag extends TemplateImpl<TagToken> {
  /** The name the filter was called by. */
  public final String name;
  /** The engine the tag belongs to. */
  public final Liquid liquid;
  protected final Tokenizer tokenizer;

  protected Tag (TagToken token, List<Token> remainTokens, Liquid liquid) {
    super(token);
    this.name = token.name;
    this.liquid = liquid;
    this.tokenizer = token.tokenizer;
  }
}
