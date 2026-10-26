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
import com.liquidjs.tokens.IdentifierToken;
import com.liquidjs.tokens.QuotedToken;
import com.liquidjs.tokens.TagToken;
import com.liquidjs.tokens.Token;
import com.liquidjs.util.Underscore;

import java.util.ArrayList;
import java.util.List;

/** Renders its body and keeps the output under a name. */
public class CaptureTag extends Tag {
  /** How the parser builds this tag. */
  public static final TagClass CLASS = CaptureTag::new;

  private final Token identifier;
  private final String variable;
  private final List<Template> templates = new ArrayList<>();

  /** The capture tag, read from its token and the tokens that follow it. */
  public CaptureTag (TagToken tagToken, List<Token> remainTokens, Liquid liquid, Parser parser) {
    super(tagToken, remainTokens, liquid);
    this.identifier = readVariable();
    this.variable = com.liquidjs.js.Js.toStringValue(this.identifier.getContent());

    while (!remainTokens.isEmpty()) {
      Token token = remainTokens.remove(0);
      if (token.kind == com.liquidjs.parser.TokenKind.Tag && "endcapture".equals(((TagToken) token).name)) return;
      this.templates.add(parser.parseToken(token, remainTokens));
    }
    throw new RuntimeException("tag " + tagToken.getText() + " not closed");
  }

  private Token readVariable () {
    IdentifierToken ident = this.tokenizer.readIdentifier();
    if (!ident.content.isEmpty()) return ident;
    QuotedToken quoted = this.tokenizer.readQuoted();
    if (quoted != null) return quoted;
    throw this.tokenizer.error("invalid capture name");
  }

  @Override
  public Object render (Context ctx, Emitter emitter) {
    Object html = this.liquid.renderer.renderTemplates(this.templates, ctx);
    ctx.bottom().put(this.variable, html);
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

  @Override
  public List<Token> localScope () {
    List<Token> scope = new ArrayList<>();
    scope.add(identifier);
    return scope;
  }
}
