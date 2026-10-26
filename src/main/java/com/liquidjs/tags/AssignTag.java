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
import com.liquidjs.template.Value;
import com.liquidjs.tokens.IdentifierToken;
import com.liquidjs.tokens.TagToken;
import com.liquidjs.tokens.Token;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Assigns a value to a name in the template's own scope. */
public class AssignTag extends Tag {
  public static final TagClass CLASS = AssignTag::new;

  private final String key;
  private final Value value;
  private final IdentifierToken identifier;

  public AssignTag (TagToken token, List<Token> remainTokens, Liquid liquid, Parser parser) {
    super(token, remainTokens, liquid);
    this.identifier = this.tokenizer.readIdentifier();
    this.key = this.identifier.content;
    this.tokenizer.assertTrue(!this.key.isEmpty(), "expected variable name");

    this.tokenizer.skipBlank();
    this.tokenizer.assertTrue("=".equals(this.tokenizer.peek()), "expected \"=\"");

    this.tokenizer.advance();
    this.value = new Value(this.tokenizer.readFilteredValue(), this.liquid);
  }

  @Override
  public Object render (Context ctx, Emitter emitter) {
    ctx.bottom().put(this.key, this.value.value(ctx, this.liquid.options.lenientIf));
    return null;
  }

  @Override
  public List<Object> arguments () {
    return Collections.singletonList(value);
  }

  @Override
  public List<Token> localScope () {
    List<Token> scope = new ArrayList<>();
    scope.add(identifier);
    return scope;
  }
}
