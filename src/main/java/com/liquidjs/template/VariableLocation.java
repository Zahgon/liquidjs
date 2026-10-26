/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 Jun Yang
 */
package com.liquidjs.template;

/** The row, the column and the file a variable was found at. */
public class VariableLocation {
  /** The one-based row. */
  public final int row;
  /** The one-based column. */
  public final int col;
  /** The file, or null for a template given as text. */
  public final String file;

  /** Where in a template something was read. */
  public VariableLocation (int row, int col, String file) {
    this.row = row;
    this.col = col;
    this.file = file;
  }

  /** The one-based row. */
  public int getRow () {
    return row;
  }

  /** The one-based column. */
  public int getCol () {
    return col;
  }

  /** The file, or null for a template given as text. */
  public String getFile () {
    return file;
  }
}
