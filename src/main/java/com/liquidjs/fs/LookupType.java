/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 Jun Yang
 */
package com.liquidjs.fs;

/** Which list of directories a lookup searches. */
public enum LookupType {
  Partials("partials"),
  Layouts("layouts"),
  Root("root");

  private final String key;

  LookupType (String key) {
    this.key = key;
  }

  /** The option name this lookup type reads its directories from. */
  public String key () {
    return key;
  }
}
