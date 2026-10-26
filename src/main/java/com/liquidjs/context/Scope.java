/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 Jun Yang
 */
package com.liquidjs.context;

import com.liquidjs.js.Js;

import java.util.Map;

/** A scope: a mapping from a name to a value, with no inherited members of its own. */
public final class Scope {
  private Scope () {}

  public static Map<String, Object> createScope () {
    return Js.newObject();
  }

  public static Map<String, Object> createScope (Map<String, Object> from) {
    Map<String, Object> scope = Js.newObject();
    if (from != null) scope.putAll(from);
    return scope;
  }

  /** A scope holding one name. */
  public static Map<String, Object> of (String key, Object value) {
    Map<String, Object> scope = Js.newObject();
    scope.put(key, value);
    return scope;
  }
}
