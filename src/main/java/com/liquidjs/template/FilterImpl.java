/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 Jun Yang
 */
package com.liquidjs.template;

import com.liquidjs.Liquid;
import com.liquidjs.context.Context;
import com.liquidjs.tokens.FilterToken;

/** What a filter implementation is given beside its arguments. */
public class FilterImpl {
  public final Context context;
  public final FilterToken token;
  public final Liquid liquid;

  public FilterImpl (Context context, FilterToken token, Liquid liquid) {
    this.context = context;
    this.token = token;
    this.liquid = liquid;
  }
}
