/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 Jun Yang
 */
package com.liquidjs.drop;

import com.liquidjs.context.Context;
import com.liquidjs.js.Js;

import java.util.Map;

/** A value with behaviour of its own, which a template reads as if it were a plain object. */
public abstract class Drop {
  /** Read a property the drop defines; an unknown property is absent. */
  public Object getProperty (String key) {
    return Js.UNDEFINED;
  }

  /** Called for a property the drop does not define. */
  public Object liquidMethodMissing (Object key, Context context) {
    return Js.UNDEFINED;
  }

  /** The value the drop stands for in a comparison or in output; by default the drop itself. */
  public Object valueOf () {
    return this;
  }

  /** The own enumerable properties the drop serialises as. */
  public Map<String, Object> ownProperties () {
    return Js.newObject();
  }
}
