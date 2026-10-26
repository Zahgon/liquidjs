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
  /** Keep a parsed template under a key. */
  void write (String key, List<Template> value);

  /** The template kept under a key, or null when none is. */
  List<Template> read (String key);

  /** Forget the template kept under a key. */
  void remove (String key);
}
