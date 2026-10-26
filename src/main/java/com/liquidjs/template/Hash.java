/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 Jun Yang
 */
package com.liquidjs.template;

import com.liquidjs.context.Context;
import com.liquidjs.js.Js;
import com.liquidjs.parser.Tokenizer;
import com.liquidjs.render.Expression;
import com.liquidjs.tokens.HashToken;
import com.liquidjs.tokens.Token;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The key and value pairs a tag was given. For the markup {@code , foo:'bar', coo:2 reversed %}}
 * the hash holds foo as 'bar', coo as 2 and reversed as true.
 */
public class Hash {
  public final Map<String, Token> hash = new LinkedHashMap<>();

  /** The key and value arguments read from their source text. */
  public Hash (String input) {
    this(new Tokenizer(input, Js.newObject()), null);
  }

  /** The key and value arguments, with the separator between a key and its value. */
  public Hash (String input, Object jekyllStyle) {
    this(new Tokenizer(input, Js.newObject()), jekyllStyle);
  }

  /** The key and value arguments read from where a tokenizer stands. */
  public Hash (Tokenizer tokenizer, Object jekyllStyle) {
    for (HashToken token : tokenizer.readHashes(jekyllStyle)) {
      this.hash.put(token.name.content, token.value);
    }
  }

  /** Evaluate every argument; one written without a value is true. */
  public Map<String, Object> render (Context ctx) {
    Map<String, Object> rendered = Js.newObject();
    for (Map.Entry<String, Token> entry : hash.entrySet()) {
      rendered.put(entry.getKey(), entry.getValue() == null
          ? Boolean.TRUE
          : Expression.evalToken(entry.getValue(), ctx));
    }
    return rendered;
  }
}
