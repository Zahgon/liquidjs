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
import com.liquidjs.render.Expression;
import com.liquidjs.template.Tag;
import com.liquidjs.template.TagClass;
import com.liquidjs.tokens.TagToken;
import com.liquidjs.tokens.Token;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Writes the next of its candidates each time it is rendered. */
public class CycleTag extends Tag {
  /** How the parser builds this tag. */
  public static final TagClass CLASS = CycleTag::new;

  private final List<Token> candidates = new ArrayList<>();
  private Token group;

  /** The cycle tag, read from its token and the tokens that follow it. */
  public CycleTag (TagToken token, List<Token> remainTokens, Liquid liquid, Parser parser) {
    super(token, remainTokens, liquid);
    Token group = this.tokenizer.readValue();
    this.tokenizer.skipBlank();

    if (group != null) {
      if (":".equals(this.tokenizer.peek())) {
        this.group = group;
        this.tokenizer.advance();
      } else {
        this.candidates.add(group);
      }
    }

    while (!this.tokenizer.end()) {
      Token value = this.tokenizer.readValue();
      if (value != null) this.candidates.add(value);
      this.tokenizer.readTo(",");
    }
    this.tokenizer.assertTrue(!this.candidates.isEmpty(),
        () -> "empty candidates: \"" + token.getText() + "\"");
  }

  @Override
  public Object render (Context ctx, Emitter emitter) {
    Object group = Expression.evalToken(this.group, ctx);
    // the candidate tokens are objects, so they all read the same way: the fingerprint is made of
    // the group and of how many candidates there are
    StringBuilder joined = new StringBuilder();
    for (int i = 0; i < candidates.size(); i++) {
      if (i > 0) joined.append(',');
      joined.append("[object Object]");
    }
    String fingerprint = "cycle:" + Js.toStringValue(group) + ":" + joined;
    Map<String, Object> groups = ctx.getRegister("cycle", Js.newObject());
    Object stored = groups.get(fingerprint);
    int idx = stored == null ? 0 : (int) (double) (Double) stored;
    if (stored == null) groups.put(fingerprint, 0.0);

    Token candidate = this.candidates.get(idx);
    idx = (idx + 1) % this.candidates.size();
    groups.put(fingerprint, (double) idx);
    return Expression.evalToken(candidate, ctx);
  }

  @Override
  public List<Object> arguments () {
    List<Object> args = new ArrayList<>(candidates);
    if (group != null) args.add(group);
    return args;
  }
}
