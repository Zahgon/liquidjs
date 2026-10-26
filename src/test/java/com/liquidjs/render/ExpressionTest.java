/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 Jun Yang
 */
package com.liquidjs.render;

import com.liquidjs.Liquid;
import com.liquidjs.js.Js;
import com.liquidjs.template.StaticAnalysis;
import com.liquidjs.template.StaticAnalysisOptions;
import com.liquidjs.template.Variable;
import com.liquidjs.util.LiquidError;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

/** Operators over untyped values. */
class ExpressionTest {
  private static Map<String, Object> map (Object... pairs) {
    Map<String, Object> out = new LinkedHashMap<>();
    for (int i = 0; i < pairs.length; i += 2) out.put((String) pairs[i], pairs[i + 1]);
    return out;
  }

  private static List<Object> list (Object... items) {
    List<Object> out = new ArrayList<>();
    for (Object item : items) out.add(item);
    return out;
  }

  /** Render a template and return what the engine produced, tagged with its type. */
  private static String render (String template, Object scope, Map<String, Object> options) {
    return tagged(new Liquid(options == null ? map() : options).parseAndRenderSync(template, scope));
  }

  private static String evaluate (String expression, Object scope, Map<String, Object> options) {
    return tagged(new Liquid(options == null ? map() : options).evalValueSync(expression, scope));
  }

  private static String analysis (String template, Map<String, Object> options) {
    StaticAnalysis report = new Liquid(options == null ? map() : options)
        .parseAndAnalyzeSync(template, null, new StaticAnalysisOptions(true));
    StringBuilder out = new StringBuilder("{");
    String[] names = { "variables", "globals", "locals" };
    Map<String, List<Variable>>[] groups = new Map[] {
      report.getVariables(), report.getGlobals(), report.getLocals()
    };
    for (int i = 0; i < names.length; i++) {
      if (i > 0) out.append(',');
      out.append('"').append(names[i]).append("\":{");
      boolean first = true;
      for (Map.Entry<String, List<Variable>> entry : groups[i].entrySet()) {
        if (!first) out.append(',');
        out.append('"').append(entry.getKey()).append("\":[");
        for (int j = 0; j < entry.getValue().size(); j++) {
          Variable variable = entry.getValue().get(j);
          if (j > 0) out.append(',');
          out.append('"').append(variable).append('@').append(variable.getRow()).append(':')
             .append(variable.getCol()).append('"');
        }
        out.append(']');
        first = false;
      }
      out.append('}');
    }
    return "json\u0001" + out.append('}');
  }

  private static String names (List<?> found) {
    StringBuilder out = new StringBuilder("[");
    for (int i = 0; i < found.size(); i++) {
      if (i > 0) out.append(',');
      out.append(segments(found.get(i)));
    }
    return "json\u0001" + out.append(']');
  }

  private static String segments (Object value) {
    if (value instanceof List) {
      StringBuilder out = new StringBuilder("[");
      List<?> items = (List<?>) value;
      for (int i = 0; i < items.size(); i++) {
        if (i > 0) out.append(',');
        out.append(segments(items.get(i)));
      }
      return out.append(']').toString();
    }
    if (value instanceof Double) return Js.toStringValue(value);
    return '"' + String.valueOf(value) + '"';
  }

  /** The value and its type, so a number and the string of that number do not read alike. */
  private static String tagged (Object value) {
    if (value == null) return "nil\u0001";
    if (value instanceof List || value instanceof Map) {
      return (value instanceof List ? "array" : "object") + "\u0001" + segments(value);
    }
    if (value instanceof Double) return "number\u0001" + Js.toStringValue(value);
    if (value instanceof Boolean) return "boolean\u0001" + value;
    if ("undefined".equals(String.valueOf(value))) return "nil\u0001";
    return "string\u0001" + value;
  }

  /** The kind, the message and the context of the error a template raised. */
  private static String raised (Runnable body) {
    try {
      body.run();
    } catch (LiquidError err) {
      return err.name + "\u0001" + err.getMessage() + "\u0001" + err.context;
    } catch (RuntimeException err) {
      String name = "AssertionError".equals(err.getClass().getSimpleName()) ? "AssertionError" : "Error";
      return name + "\u0001" + err.getMessage() + "\u0001";
    }
    return fail("expected the template to raise");
  }

  @Test
  void comparesStringsAsStrings () {
    assertEquals("boolean\u0001true",
        evaluate("'2' > '10'", null, null));
    assertEquals("boolean\u0001true",
        evaluate("'a' < 'b'", null, null));
    assertEquals("boolean\u0001false",
        evaluate("2 > '10'", null, null));
  }

  @Test
  void comparesAbsentValues () {
    assertEquals("boolean\u0001false",
        evaluate("null > 0", null, null));
    assertEquals("boolean\u0001false",
        evaluate("null >= 0", null, null));
    assertEquals("boolean\u0001true",
        evaluate("missing == nil", null, null));
    assertEquals("boolean\u0001false",
        evaluate("empty == blank", null, null));
  }

  @Test
  void comparesArraysElementWise () {
    assertEquals("boolean\u0001true",
        evaluate("a == b", map("a", list(Double.valueOf(1), Double.valueOf(2)), "b", list(Double.valueOf(1), Double.valueOf(2))), null));
    assertEquals("boolean\u0001false",
        evaluate("a == b", map("a", list(Double.valueOf(1), Double.valueOf(2)), "b", list(Double.valueOf(1), Double.valueOf(3))), null));
    assertEquals("boolean\u0001true",
        evaluate("a == b", map("a", list(Double.valueOf(1), list(Double.valueOf(2))), "b", list(Double.valueOf(1), list(Double.valueOf(2)))), null));
  }

  @Test
  void testsMembership () {
    assertEquals("boolean\u0001true",
        evaluate("'abc' contains 'b'", null, null));
    assertEquals("boolean\u0001true",
        evaluate("a contains 1", map("a", list(Double.valueOf(1), Double.valueOf(2))), null));
    assertEquals("boolean\u0001false",
        evaluate("a contains '1'", map("a", list(Double.valueOf(1), Double.valueOf(2))), null));
  }

  @Test
  void combinesWithAndOr () {
    assertEquals("boolean\u0001true",
        evaluate("true and false or true", null, null));
    assertEquals("boolean\u0001false",
        evaluate("false or true and false", null, null));
    assertEquals("boolean\u0001true",
        evaluate("1 > 2 or 3 > 2", null, null));
  }

  @Test
  void appliesTheTruthinessOfLiquid () {
    assertEquals("string\u0001T",
        render("{% if 0 %}T{% else %}F{% endif %}", null, null));
    assertEquals("string\u0001T",
        render("{% if '' %}T{% else %}F{% endif %}", null, null));
    assertEquals("string\u0001F",
        render("{% if 0 %}T{% else %}F{% endif %}", null, map("jsTruthy", Boolean.TRUE)));
    assertEquals("string\u0001F",
        render("{% if '' %}T{% else %}F{% endif %}", null, map("jsTruthy", Boolean.TRUE)));
  }

  @Test
  void buildsRanges () {
    assertEquals("string\u0001[1,2,3]",
        render("{{ (1..3) | json }}", null, null));
    assertEquals("string\u0001[]",
        render("{{ (3..1) | json }}", null, null));
    assertEquals("string\u0001[1,2,3]",
        render("{{ (a..b) | json }}", map("a", Double.valueOf(1), "b", Double.valueOf(3)), null));
  }
}
