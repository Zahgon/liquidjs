/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 Jun Yang
 */
package com.liquidjs.parser;

import com.liquidjs.tokens.Token;

/** One argument of a filter: either a value, or a key and a value. */
public class FilterArg {
  public final String key;
  public final Token value;

  private FilterArg (String key, Token value) {
    this.key = key;
    this.value = value;
  }

  public static FilterArg of (Token value) {
    return new FilterArg(null, value);
  }

  public static FilterArg of (String key, Token value) {
    return new FilterArg(key, value);
  }

  /** True when the argument was written as a key and a value pair. */
  public boolean isKeyValuePair () {
    return key != null;
  }
}
