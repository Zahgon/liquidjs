/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 Jun Yang
 */
package com.liquidjs.tokens;

import com.liquidjs.parser.FilterArg;
import com.liquidjs.parser.TokenKind;

import java.util.List;

/** One filter in a filter chain, with the arguments it was given. */
public class FilterToken extends Token {
  public final String name;
  public final List<FilterArg> args;

  public FilterToken (String name, List<FilterArg> args, String input, int begin, int end,
                      String file) {
    super(TokenKind.Filter, input, begin, end, file);
    this.name = name;
    this.args = args;
  }
}
