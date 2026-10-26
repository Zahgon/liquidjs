/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 Jun Yang
 */
package com.liquidjs.drop;

import com.liquidjs.js.Js;
import com.liquidjs.js.JsJson;

import java.util.Map;

/** The loop state a for tag exposes as forloop. */
public class ForloopDrop extends Drop {
  protected int i = 0;
  /** How many elements the loop runs over. */
  public int length;
  /** The loop's name: the variable and the collection it reads. */
  public String name;

  /** The state of a loop over a collection, before its first turn. */
  public ForloopDrop (int length, String collection, String variable) {
    this.length = length;
    this.name = variable + "-" + collection;
  }

  /** Move to the next turn of the loop. */
  public void next () {
    this.i++;
  }

  /** The zero-based number of this turn. */
  public int index0 () {
    return this.i;
  }

  /** The one-based number of this turn. */
  public int index () {
    return this.i + 1;
  }

  /** Whether this is the first turn. */
  public boolean first () {
    return this.i == 0;
  }

  /** Whether this is the last turn. */
  public boolean last () {
    return this.i == this.length - 1;
  }

  /** How many turns are left, counting this one. */
  public int rindex () {
    return this.length - this.i;
  }

  /** How many turns are left after this one. */
  public int rindex0 () {
    return this.length - this.i - 1;
  }

  @Override
  public Object getProperty (String key) {
    switch (key) {
      case "i": return (double) i;
      case "length": return (double) length;
      case "name": return name;
      case "index0": return (double) index0();
      case "index": return (double) index();
      case "first": return first();
      case "last": return last();
      case "rindex": return (double) rindex();
      case "rindex0": return (double) rindex0();
      default: return Js.UNDEFINED;
    }
  }

  @Override
  public Map<String, Object> ownProperties () {
    Map<String, Object> props = Js.newObject();
    props.put("i", (double) i);
    props.put("length", (double) length);
    props.put("name", name);
    return props;
  }

  @Override
  public Object valueOf () {
    return JsJson.stringify(ownProperties());
  }
}
