/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 Jun Yang
 */
package com.liquidjs;

import com.liquidjs.js.Js;

import java.util.Map;

/** The options one render call may override. */
public class RenderOptions {
  /** Whether the call is synchronous; the engine sets this itself. */
  public boolean sync = false;
  /** The scope this call adds, as the globals option does for every call. */
  public Map<String, Object> globals = null;
  /** Raise on an undefined variable for this call only; null leaves the option alone. */
  public Boolean strictVariables = null;
  /** Read only own properties for this call only; null leaves the option alone. */
  public Boolean ownPropertyOnly = null;
  /** Limit how many tags, outputs and HTML nodes one render call may write. */
  public Double templateLimit = null;
  /** The time this call may take, in milliseconds; null leaves the option alone. */
  public Double renderLimit = null;
  /** How much data this call may create; null leaves the option alone. */
  public Double memoryLimit = null;

  /** Options that change nothing, which is what a plain render call uses. */
  public RenderOptions () {
  }

  /** Options that only say whether the call is synchronous. */
  public static RenderOptions sync (boolean sync) {
    RenderOptions options = new RenderOptions();
    options.sync = sync;
    return options;
  }

  /** Read the overrides out of a mapping whose keys are the documented option names. */
  public static RenderOptions normalize (Map<String, Object> input) {
    RenderOptions options = new RenderOptions();
    if (input == null) return options;
    if (input.containsKey("sync")) options.sync = Js.truthy(input.get("sync"));
    if (input.get("globals") instanceof Map) options.globals = Js.asObject(input.get("globals"));
    if (input.containsKey("strictVariables")) options.strictVariables = Js.truthy(input.get("strictVariables"));
    if (input.containsKey("ownPropertyOnly")) options.ownPropertyOnly = Js.truthy(input.get("ownPropertyOnly"));
    if (input.containsKey("templateLimit")) options.templateLimit = Js.toNumberValue(input.get("templateLimit"));
    if (input.containsKey("renderLimit")) options.renderLimit = Js.toNumberValue(input.get("renderLimit"));
    if (input.containsKey("memoryLimit")) options.memoryLimit = Js.toNumberValue(input.get("memoryLimit"));
    return options;
  }
}
