/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 Jun Yang
 */
package com.liquidjs.filters;

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

/** The arithmetic filters. */
class MathFiltersTest {
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
  void roundsHalfUpWithTheEpsilonScale () {
    assertEquals("string\u00012.68",
        render("{{ 2.675 | round: 2 }}", null, null));
    assertEquals("string\u00011.01",
        render("{{ 1.005 | round: 2 }}", null, null));
    assertEquals("string\u00013",
        render("{{ 2.5 | round }}", null, null));
    assertEquals("string\u0001-3",
        render("{{ -2.5 | round }}", null, null));
    assertEquals("string\u0001183.36",
        render("{{ 183.357 | round: 2 }}", null, null));
  }

  @Test
  void divides () {
    assertEquals("string\u00010.3333333333333333",
        render("{{ 1 | divided_by: 3 }}", null, null));
    assertEquals("string\u0001Infinity",
        render("{{ 1 | divided_by: 0 }}", null, null));
    assertEquals("string\u0001-Infinity",
        render("{{ -1 | divided_by: 0 }}", null, null));
    assertEquals("string\u0001NaN",
        render("{{ 0 | divided_by: 0 }}", null, null));
    assertEquals("string\u00013",
        render("{{ 7 | divided_by: 2, true }}", null, null));
  }

  @Test
  void takesTheRemainderWithTheSignOfTheDivisor () {
    assertEquals("string\u00012",
        render("{{ -7 | modulo: 3 }}", null, null));
    assertEquals("string\u0001-2",
        render("{{ 7 | modulo: -3 }}", null, null));
    assertEquals("string\u00010",
        render("{{ 7.5 | modulo: 0.5 }}", null, null));
  }

  @Test
  void readsItsArgumentsAsNumbers () {
    assertEquals("string\u00016",
        render("{{ '5' | plus: 1 }}", null, null));
    assertEquals("string\u00011",
        render("{{ 'abc' | plus: 1 }}", null, null));
    assertEquals("string\u00011",
        render("{{ nothing | plus: 1 }}", map("nothing", null), null));
    assertEquals("string\u000124",
        render("{{ ' 12 ' | times: 2 }}", null, null));
    assertEquals("string\u000116",
        render("{{ '0x10' | plus: 0 }}", null, null));
  }

  @Test
  void clampsAndRoundsToIntegers () {
    assertEquals("string\u00012",
        render("{{ 1.2 | ceil }}", null, null));
    assertEquals("string\u0001-2",
        render("{{ -1.2 | floor }}", null, null));
    assertEquals("string\u00013",
        render("{{ -3 | abs }}", null, null));
    assertEquals("string\u00012",
        render("{{ 1 | at_least: 2 }}", null, null));
    assertEquals("string\u00011",
        render("{{ 1 | at_most: 2 }}", null, null));
  }
}
