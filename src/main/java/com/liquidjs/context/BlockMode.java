/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 Jun Yang
 */
package com.liquidjs.context;

/** Whether a block writes its output or stores it for a layout to place. */
public enum BlockMode {
  /** Write the rendered HTML directly. */
  OUTPUT,
  /** Store the rendered HTML into the blocks register. */
  STORE
}
