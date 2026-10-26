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
  public int length;
  public String name;

  public ForloopDrop (int length, String collection, String variable) {
    this.length = length;
    this.name = variable + "-" + collection;
  }

  public void next () {
    this.i++;
  }

  public int index0 () {
    return this.i;
  }

  public int index () {
    return this.i + 1;
  }

  public boolean first () {
    return this.i == 0;
  }

  public boolean last () {
    return this.i == this.length - 1;
  }

  public int rindex () {
    return this.length - this.i;
  }

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
