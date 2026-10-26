/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 Jun Yang
 */
package com.liquidjs.filters;

import com.liquidjs.js.Js;

import java.util.Map;

/** Every filter the engine registers by default. */
public final class Filters {
  private Filters () {}

  /** Every one of them, under the names a template calls them by. */
  public static Map<String, Object> all () {
    Map<String, Object> filters = Js.newObject();
    HtmlFilters.register(filters);
    MathFilters.register(filters);
    UrlFilters.register(filters);
    ArrayFilters.register(filters);
    DateFilters.register(filters);
    StringFilters.register(filters);
    Base64Filters.register(filters);
    CryptoFilters.register(filters);
    MiscFilters.register(filters);
    return filters;
  }
}
