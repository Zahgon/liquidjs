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
  /** The context the filter runs in. */
  public final Context context;
  /** The token the filter was read from. */
  public final FilterToken token;
  /** The engine the tag belongs to. */
  public final Liquid liquid;

  /** What a filter implementation is given beside its arguments. */
  public FilterImpl (Context context, FilterToken token, Liquid liquid) {
    this.context = context;
    this.token = token;
    this.liquid = liquid;
  }
}
