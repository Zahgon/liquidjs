/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 Jun Yang
 */
package com.liquidjs.tags;

import com.liquidjs.Liquid;
import com.liquidjs.context.Context;
import com.liquidjs.emitters.Emitter;
import com.liquidjs.js.Js;
import com.liquidjs.parser.Parser;
import com.liquidjs.template.Tag;
import com.liquidjs.template.TagClass;
import com.liquidjs.tokens.IdentifierToken;
import com.liquidjs.tokens.TagToken;
import com.liquidjs.tokens.Token;
import com.liquidjs.util.Underscore;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Counts up from zero in the scope the caller passed in. */
public class IncrementTag extends Tag {
  public static final TagClass CLASS = IncrementTag::new;

  private final IdentifierToken identifier;
  private final String variable;

  public IncrementTag (TagToken token, List<Token> remainTokens, Liquid liquid, Parser parser) {
    super(token, remainTokens, liquid);
    this.identifier = this.tokenizer.readIdentifier();
    this.variable = this.identifier.content;
  }

  @Override
  public Object render (Context context, Emitter emitter) {
    Map<String, Object> scope = Js.asObject(context.environments);
    if (!Js.isNumber(scope.get(variable))) scope.put(variable, 0.0);
    Object val = scope.get(variable);
    scope.put(variable, (Double) val + 1);
    emitter.write(Underscore.stringify(val));
    return null;
  }

  @Override
  public List<Token> localScope () {
    List<Token> scope = new ArrayList<>();
    scope.add(identifier);
    return scope;
  }
}
