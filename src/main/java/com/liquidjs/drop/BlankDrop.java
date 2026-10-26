/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 Jun Yang
 */
package com.liquidjs.drop;

import com.liquidjs.js.Js;
import com.liquidjs.js.JsString;

/** The blank literal, which equals false, an absent value and any string of blanks. */
public class BlankDrop extends EmptyDrop {
  @Override
  public boolean equalsValue (Object input) {
    if (Boolean.FALSE.equals(input)) return true;
    if (Js.isNil(Js.toValue(input))) return true;
    if (Js.isString(input)) return JsString.trim((String) input).isEmpty();
    return super.equalsValue(input);
  }

  /** Whether a value is one of this engine's errors. */
  public static boolean is (Object value) {
    return value instanceof BlankDrop;
  }
}
