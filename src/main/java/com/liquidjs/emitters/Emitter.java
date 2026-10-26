/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 Jun Yang
 */
package com.liquidjs.emitters;

/** Where a template writes its output. */
public interface Emitter {
  /** Write a value into the emitter. */
  void write (Object html);

  /** The output written so far. */
  Object getBuffer ();
}
