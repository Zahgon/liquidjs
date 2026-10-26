/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 Jun Yang
 */
package com.liquidjs.drop;

import com.liquidjs.js.Js;

import java.util.Map;

/** The loop state a tablerow tag exposes as tablerowloop, which also tracks the column. */
public class TablerowloopDrop extends ForloopDrop {
  private final int cols;

  /** The state of a table row loop over a collection, laid out in columns. */
  public TablerowloopDrop (int length, int cols, String collection, String variable) {
    super(length, collection, variable);
    this.length = length;
    this.cols = cols;
  }

  /** The one-based row this turn writes into. */
  public double row () {
    return Math.floor((double) this.i / this.cols) + 1;
  }

  /** The zero-based column this turn writes into. */
  public double col0 () {
    return (double) this.i % this.cols;
  }

  /** The one-based column this turn writes into. */
  public double col () {
    return col0() + 1;
  }

  /** Whether this turn opens a row. */
  public boolean colFirst () {
    return col0() == 0;
  }

  /** Whether this turn closes a row. */
  public boolean colLast () {
    return col() == this.cols;
  }

  @Override
  public Object getProperty (String key) {
    switch (key) {
      case "cols": return (double) cols;
      case "row": return row();
      case "col0": return col0();
      case "col": return col();
      case "col_first": return colFirst();
      case "col_last": return colLast();
      default: return super.getProperty(key);
    }
  }

  @Override
  public Map<String, Object> ownProperties () {
    Map<String, Object> props = super.ownProperties();
    props.put("cols", (double) cols);
    return props;
  }
}
