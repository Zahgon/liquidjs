/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 Jun Yang
 */
package com.liquidjs.tokens;

import com.liquidjs.parser.TokenKind;
import com.liquidjs.render.Expression;

import java.util.List;

/** A value expression with its filters, as in {% assign foo = "bar" | append: "coo" %} */
public class FilteredValueToken extends Token implements ValueToken {
  public final Expression initial;
  public final List<FilterToken> filters;

  public FilteredValueToken (Expression initial, List<FilterToken> filters, String input, int begin,
                             int end, String file) {
    super(TokenKind.FilteredValue, input, begin, end, file);
    this.initial = initial;
    this.filters = filters;
  }
}
