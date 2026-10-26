/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 Jun Yang
 */
package com.liquidjs.parser;

import com.liquidjs.tokens.Token;

/** One argument of a filter: either a value, or a key and a value. */
public class FilterArg {
  /** The key the argument was written under, or null when it had none. */
  public final String key;
  /** The value of the argument. */
  public final Token value;

  private FilterArg (String key, Token value) {
    this.key = key;
    this.value = value;
  }

  /** An argument written as a value alone. */
  public static FilterArg of (Token value) {
    return new FilterArg(null, value);
  }

  /** An argument written as a key and a value. */
  public static FilterArg of (String key, Token value) {
    return new FilterArg(key, value);
  }

  /** True when the argument was written as a key and a value pair. */
  public boolean isKeyValuePair () {
    return key != null;
  }
}
