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

  public TablerowloopDrop (int length, int cols, String collection, String variable) {
    super(length, collection, variable);
    this.length = length;
    this.cols = cols;
  }

  public double row () {
    return Math.floor((double) this.i / this.cols) + 1;
  }

  public double col0 () {
    return (double) this.i % this.cols;
  }

  public double col () {
    return col0() + 1;
  }

  public boolean colFirst () {
    return col0() == 0;
  }

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
