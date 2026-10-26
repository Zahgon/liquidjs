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
import com.liquidjs.template.Template;
import com.liquidjs.tokens.LiquidTagToken;
import com.liquidjs.tokens.TagToken;
import com.liquidjs.tokens.Token;

import java.util.ArrayList;
import java.util.List;

/** Several tags written on their own lines, without delimiters of their own. */
public class LiquidTag extends Tag {
  public static final TagClass CLASS = LiquidTag::new;

  private final List<Template> templates;

  public LiquidTag (TagToken token, List<Token> remainTokens, Liquid liquid, Parser parser) {
    super(token, remainTokens, liquid);
    List<LiquidTagToken> read = this.tokenizer.readLiquidTagTokens(this.liquid.options);
    List<Token> tokens = new ArrayList<>(read);
    this.templates = parser.parseTokens(tokens);
  }

  @Override
  public Object render (Context ctx, Emitter emitter) {
    this.liquid.renderer.renderTemplates(this.templates, ctx, emitter);
    return null;
  }

  @Override
  public boolean hasChildren () {
    return true;
  }

  @Override
  public List<Template> children (boolean partials, boolean sync) {
    return templates;
  }
}
