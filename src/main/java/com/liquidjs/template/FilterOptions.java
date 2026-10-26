/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 Jun Yang
 */
package com.liquidjs.template;

/** A filter registered with options rather than as a plain handler. */
public class FilterOptions {
  /** The implementation the filter runs. */
  public final FilterHandler handler;
  /** True when the filter's value needs no output escape. */
  public final boolean raw;

  /** A filter registered with options rather than as a plain handler. */
  public FilterOptions (FilterHandler handler, boolean raw) {
    this.handler = handler;
    this.raw = raw;
  }
}
