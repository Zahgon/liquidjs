/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 Jun Yang
 */
package com.liquidjs.cache;

import com.liquidjs.template.Template;

import java.util.List;

/** A cache of parsed templates. */
public interface LiquidCache {
  void write (String key, List<Template> value);

  List<Template> read (String key);

  void remove (String key);
}
