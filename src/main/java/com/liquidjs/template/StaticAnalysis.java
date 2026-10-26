/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 Jun Yang
 */
package com.liquidjs.template;

import java.util.List;
import java.util.Map;

/** What analysing a template reports. */
public class StaticAnalysis {
  /**
   * Every variable, whether it is in scope or not, including names a tag introduces such as
   * forloop.
   */
  public final Map<String, List<Variable>> variables;
  /**
   * Variables that are not in scope: either globals the application is expected to provide, or
   * mistakes of the template's author.
   */
  public final Map<String, List<Variable>> globals;
  /** Variables a tag such as assign, capture or increment adds to the template's own scope. */
  public final Map<String, List<Variable>> locals;

  /** The three reports analysing a template produces. */
  public StaticAnalysis (Map<String, List<Variable>> variables,
                         Map<String, List<Variable>> globals,
                         Map<String, List<Variable>> locals) {
    this.variables = variables;
    this.globals = globals;
    this.locals = locals;
  }

  /** Every variable the template reads, grouped by its root segment. */
  public Map<String, List<Variable>> getVariables () {
    return variables;
  }

  /** Every variable the template expects to be given. */
  public Map<String, List<Variable>> getGlobals () {
    return globals;
  }

  /** Every variable the template adds to its own scope. */
  public Map<String, List<Variable>> getLocals () {
    return locals;
  }
}
