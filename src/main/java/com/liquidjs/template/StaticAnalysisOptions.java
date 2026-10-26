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

  /** Analysis options that load and analyse partial templates. */
  public StaticAnalysisOptions () {
  }

  /** Analysis options that say whether partial templates are analysed too. */
  public StaticAnalysisOptions (boolean partials) {
    this.partials = partials;
  }
}
