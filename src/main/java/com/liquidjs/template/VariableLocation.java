/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 Jun Yang
 */
package com.liquidjs.template;

/** The row, the column and the file a variable was found at. */
public class VariableLocation {
  public final int row;
  public final int col;
  public final String file;

  public VariableLocation (int row, int col, String file) {
    this.row = row;
    this.col = col;
    this.file = file;
  }

  public int getRow () {
    return row;
  }

  public int getCol () {
    return col;
  }

  public String getFile () {
    return file;
  }
}
