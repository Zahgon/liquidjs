/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 Jun Yang
 */
package com.liquidjs.template;

/** How a template is analysed. */
public class StaticAnalysisOptions {
  /** When true, the default, partial templates are loaded and analysed too. */
  public boolean partials = true;

  public StaticAnalysisOptions () {
  }

  public StaticAnalysisOptions (boolean partials) {
    this.partials = partials;
  }
}
