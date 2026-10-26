/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 Jun Yang
 */
package com.liquidjs.template;

import com.liquidjs.js.Js;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/** A variable's segments and the place it was found, which reads as the path it was written as. */
public class Variable {
  /** The property names and array indexes that make up the path to this variable. */
  public final List<Object> segments;
  public final VariableLocation location;

  public Variable (List<Object> segments, VariableLocation location) {
    this.segments = segments;
    this.location = location;
  }

  public int getRow () {
    return location.row;
  }

  public int getCol () {
    return location.col;
  }

  public String getFile () {
    return location.file;
  }

  @Override
  public String toString () {
    return segmentsString(segments, true);
  }

  /** This variable's segments as a list, with a nested list for a nested path. */
  public List<Object> toArray () {
    List<Object> out = new ArrayList<>();
    for (Object segment : segments) {
      if (segment instanceof Variable) {
        out.add(((Variable) segment).toArray());
      } else {
        out.add(segment);
      }
    }
    return out;
  }

  /** A segment that may be written with dot notation rather than in brackets. */
  private static final Pattern PROPERTY =
      Pattern.compile("^[\\u0080-\\uFFFFa-zA-Z_][\\u0080-\\uFFFFa-zA-Z0-9_-]*$");

  /** The path as a string, using dot notation where a segment allows it. */
  public static String segmentsString (List<Object> segments, boolean bracketedRoot) {
    StringBuilder buf = new StringBuilder();
    Object root = segments.isEmpty() ? null : segments.get(0);
    if (Js.isString(root)) {
      String name = (String) root;
      if (!bracketedRoot || PROPERTY.matcher(name).matches()) buf.append(name);
      else buf.append("['").append(name).append("']");
    }
    for (int i = 1; i < segments.size(); i++) {
      Object segment = segments.get(i);
      if (segment instanceof Variable) {
        buf.append('[').append(segmentsString(((Variable) segment).segments, false)).append(']');
      } else if (Js.isString(segment)) {
        String name = (String) segment;
        if (PROPERTY.matcher(name).matches()) buf.append('.').append(name);
        else buf.append("['").append(name).append("']");
      } else {
        buf.append('[').append(Js.toStringValue(segment)).append(']');
      }
    }
    return buf.toString();
  }

  public static String segmentsString (List<Object> segments) {
    return segmentsString(segments, false);
  }
}
