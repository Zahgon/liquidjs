/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 Jun Yang
 */
package com.liquidjs.template;

import com.liquidjs.Liquid;
import com.liquidjs.context.Context;
import com.liquidjs.parser.FilterArg;
import com.liquidjs.render.Expression;
import com.liquidjs.tokens.FilterToken;

import java.util.ArrayList;
import java.util.List;

/** One filter of a filter chain, bound to the implementation registered under its name. */
public class Filter {
  /** The name the filter was called by. */
  public final String name;
  /** The arguments the filter was given, unevaluated. */
  public final List<FilterArg> args;
  /** True when the filter marks its value as needing no output escape. */
  public final boolean raw;
  private final FilterHandler handler;
  private final Liquid liquid;
  private final FilterToken token;

  /** One filter of a chain, bound to the implementation registered under its name. */
  public Filter (FilterToken token, Object options, Liquid liquid) {
    this.token = token;
    this.name = token.name;
    if (options instanceof FilterHandler) {
      this.handler = (FilterHandler) options;
      this.raw = false;
    } else if (options instanceof FilterOptions) {
      FilterOptions filterOptions = (FilterOptions) options;
      this.handler = filterOptions.handler;
      this.raw = filterOptions.raw;
    } else {
      this.handler = (impl, value, argv) -> value;
      this.raw = false;
    }
    this.args = token.args;
    this.liquid = liquid;
  }

  /** Apply the filter to a value, evaluating its arguments first. */
  public Object render (Object value, Context context) {
    List<Object> argv = new ArrayList<>();
    for (FilterArg arg : args) {
      if (arg.isKeyValuePair()) {
        List<Object> pair = new ArrayList<>();
        pair.add(arg.key);
        pair.add(Expression.evalToken(arg.value, context));
        argv.add(pair);
      } else {
        argv.add(Expression.evalToken(arg.value, context));
      }
    }
    return handler.apply(new FilterImpl(context, token, liquid), value, argv);
  }
}
