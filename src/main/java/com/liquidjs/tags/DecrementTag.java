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

/** Counts down from minus one in the scope the caller passed in. */
public class DecrementTag extends Tag {
  public static final TagClass CLASS = DecrementTag::new;

  private final IdentifierToken identifier;
  private final String variable;

  public DecrementTag (TagToken token, List<Token> remainTokens, Liquid liquid, Parser parser) {
    super(token, remainTokens, liquid);
    this.identifier = this.tokenizer.readIdentifier();
    this.variable = this.identifier.content;
  }

  @Override
  public Object render (Context context, Emitter emitter) {
    Map<String, Object> scope = Js.asObject(context.environments);
    if (!Js.isNumber(scope.get(variable))) scope.put(variable, 0.0);
    double next = (Double) scope.get(variable) - 1;
    scope.put(variable, next);
    emitter.write(Underscore.stringify(next));
    return null;
  }

  @Override
  public List<Token> localScope () {
    List<Token> scope = new ArrayList<>();
    scope.add(identifier);
    return scope;
  }
}
