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
import com.liquidjs.template.Value;
import com.liquidjs.tokens.TagToken;
import com.liquidjs.tokens.Token;

import java.util.ArrayList;
import java.util.List;

/** Writes a value, as an output does. */
public class EchoTag extends Tag {
  /** How the parser builds this tag. */
  public static final TagClass CLASS = EchoTag::new;

  private Value value;

  /** The echo tag, read from its token and the tokens that follow it. */
  public EchoTag (TagToken token, List<Token> remainTokens, Liquid liquid, Parser parser) {
    super(token, remainTokens, liquid);
    this.tokenizer.skipBlank();
    if (!this.tokenizer.end()) {
      this.value = new Value(this.tokenizer.readFilteredValue(), this.liquid);
    }
  }

  @Override
  public Object render (Context ctx, Emitter emitter) {
    if (value == null) return null;
    Object val = Js.toValue(value.value(ctx, false));
    emitter.write(val);
    return null;
  }

  @Override
  public List<Object> arguments () {
    List<Object> args = new ArrayList<>();
    if (value != null) args.add(value);
    return args;
  }
}
