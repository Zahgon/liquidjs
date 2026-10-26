/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 Jun Yang
 */
package com.liquidjs.emitters;

import com.liquidjs.util.Underscore;

/** Collects the output as one string. */
public class SimpleEmitter implements Emitter {
  /** The output written so far. */
  public String buffer = "";

  @Override
  public void write (Object html) {
    this.buffer += Underscore.stringify(html);
  }

  @Override
  public Object getBuffer () {
    return buffer;
  }
}
