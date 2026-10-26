/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 Jun Yang
 */
package com.liquidjs.template;

import com.liquidjs.Liquid;
import com.liquidjs.context.Context;
import com.liquidjs.parser.Tokenizer;
import com.liquidjs.render.Expression;
import com.liquidjs.tokens.FilteredValueToken;
import com.liquidjs.tokens.FilterToken;
import com.liquidjs.util.Assertions;

import java.util.ArrayList;
import java.util.List;

/** A value expression with its filters, as in: "foobar" | truncate: 3 */
public class Value {
  public final List<Filter> filters = new ArrayList<>();
  public final Expression initial;

  public Value (String input, Liquid liquid) {
    this(new Tokenizer(input, liquid.options.operators, null, null,
        liquid.options.groupedExpressions).readFilteredValue(), liquid);
  }

  public Value (FilteredValueToken token, Liquid liquid) {
    this.initial = token.initial;
    for (FilterToken filterToken : token.filters) {
      this.filters.add(new Filter(filterToken, getFilter(liquid, filterToken.name), liquid));
    }
  }

  public Object value (Context ctx) {
    return value(ctx, false);
  }

  public Object value (Context ctx, boolean lenientIn) {
    boolean lenient = lenientIn || (ctx.opts.lenientIf && !filters.isEmpty()
        && "default".equals(filters.get(0).name));
    Object val = initial.evaluate(ctx, lenient);
    for (Filter filter : filters) {
      val = filter.render(val, ctx);
    }
    return val;
  }

  private static Object getFilter (Liquid liquid, String name) {
    Object impl = liquid.filters.get(name);
    Assertions.assertTrue(impl != null || !liquid.options.strictFilters,
        () -> "undefined filter: " + name);
    return impl;
  }
}
