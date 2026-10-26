/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 Jun Yang
 */
package com.liquidjs.template;

import java.util.List;

/** The scope a partial template is analysed in. */
public class PartialScope {
  /** The partial's name, so each one is analysed once. */
  public final String name;
  /**
   * When true the names below go into a new scope of their own, without access to the parent
   * template's scope.
   */
  public final boolean isolated;
  /**
   * The names that will be in scope for the child template. An entry that is a pair of a name and
   * an argument makes the name an alias for that argument.
   */
  public final List<Object> scope;

  /** The scope a partial template is analysed in. */
  public PartialScope (String name, boolean isolated, List<Object> scope) {
    this.name = name;
    this.isolated = isolated;
    this.scope = scope;
  }
}
