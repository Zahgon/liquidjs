/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 Jun Yang
 */
package com.liquidjs.emitters;

import com.liquidjs.js.Js;
import com.liquidjs.util.Underscore;

/**
 * Keeps the type of a value that is written on its own, so {{ my-port }} stays a number while
 * {{ my-host }}:{{ my-port }} becomes a string.
 */
public class KeepingTypeEmitter implements Emitter {
  public Object buffer = "";

  @Override
  public void write (Object input) {
    Object html = Js.toValue(input);
    if (!Js.isString(html) && "".equals(this.buffer)) {
      this.buffer = html;
    } else {
      this.buffer = Underscore.stringify(this.buffer) + Underscore.stringify(html);
    }
  }

  @Override
  public Object getBuffer () {
    return buffer;
  }
}
