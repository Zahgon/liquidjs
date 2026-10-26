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

/** The filters over arrays. */
class ArrayFiltersTest {
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
  void sortsMixedValues () {
    assertEquals("string\u0001[\"A\",\"a10\",\"a2\",\"b\",2,10,null]",
        render("{{ a | sort | json }}", map("a", list("b", "A", "a10", "a2", Double.valueOf(10), Double.valueOf(2), null)), null));
    assertEquals("string\u0001[10,2,\"A\",\"a10\",\"a2\",\"b\",null]",
        render("{{ a | sort_natural | json }}", map("a", list("b", "A", "a10", "a2", Double.valueOf(10), Double.valueOf(2), null)), null));
    assertEquals("string\u0001[{\"n\":1},{\"n\":2}]",
        render("{{ a | sort: \"n\" | json }}", map("a", list(map("n", Double.valueOf(2)), map("n", Double.valueOf(1)))), null));
  }

  @Test
  void selectsByProperty () {
    assertEquals("string\u0001[{\"k\":\"x\"}]",
        render("{{ a | where: \"k\", \"x\" | json }}", map("a", list(map("k", "x"), map("k", "y"))), null));
    assertEquals("string\u0001[{\"k\":0},{\"k\":1}]",
        render("{{ a | where: \"k\" | json }}", map("a", list(map("k", Double.valueOf(0)), map("k", Double.valueOf(1)))), null));
    assertEquals("string\u0001[{\"k\":\"y\"}]",
        render("{{ a | reject: \"k\", \"x\" | json }}", map("a", list(map("k", "x"), map("k", "y"))), null));
    assertEquals("string\u0001{\"k\":\"y\"}",
        render("{{ a | find: \"k\", \"y\" | json }}", map("a", list(map("k", "x"), map("k", "y"))), null));
    assertEquals("string\u00011",
        render("{{ a | find_index: \"k\", \"y\" }}", map("a", list(map("k", "x"), map("k", "y"))), null));
  }

  @Test
  void selectsByExpression () {
    assertEquals("string\u0001[{\"n\":2}]",
        render("{{ a | where_exp: \"i\", \"i.n > 1\" | json }}", map("a", list(map("n", Double.valueOf(1)), map("n", Double.valueOf(2)))), null));
    assertEquals("string\u0001[{\"n\":1}]",
        render("{{ a | reject_exp: \"i\", \"i.n > 1\" | json }}", map("a", list(map("n", Double.valueOf(1)), map("n", Double.valueOf(2)))), null));
    assertEquals("string\u0001false",
        render("{{ a | has_exp: \"i\", \"i.n > 1\" }}", map("a", list(map("n", Double.valueOf(1)))), null));
  }

  @Test
  void groups () {
    assertEquals("string\u0001[{\"name\":\"x\",\"items\":[{\"k\":\"x\",\"n\":1},{\"k\":\"x\",\"n\":2}]},{\"name\":\"y\",\"items\":[{\"k\":\"y\"}]}]",
        render("{{ a | group_by: \"k\" | json }}", map("a", list(map("k", "x", "n", Double.valueOf(1)), map("k", "y"), map("k", "x", "n", Double.valueOf(2)))), null));
    assertEquals("string\u0001[{\"name\":\"x\",\"items\":[{\"k\":\"x\"}]},{\"name\":\"y\",\"items\":[{\"k\":\"y\"}]}]",
        render("{{ a | group_by_exp: \"i\", \"i.k\" | json }}", map("a", list(map("k", "x"), map("k", "y"))), null));
  }

  @Test
  void mapsAndSums () {
    assertEquals("string\u0001[1,2,null]",
        render("{{ a | map: \"n\" | json }}", map("a", list(map("n", Double.valueOf(1)), map("n", Double.valueOf(2)), map())), null));
    assertEquals("string\u00013",
        render("{{ a | sum: \"n\" }}", map("a", list(map("n", Double.valueOf(1)), map("n", "x"), map("n", Double.valueOf(2)))), null));
    assertEquals("string\u00013",
        render("{{ a | sum }}", map("a", list(Double.valueOf(1), "2", null)), null));
  }

  @Test
  void keepsUniqueElementsByIdentity () {
    assertEquals("string\u0001[1,\"1\",null]",
        render("{{ a | uniq | json }}", map("a", list(Double.valueOf(1), "1", Double.valueOf(1), null, null)), null));
    assertEquals("string\u0001[{\"x\":1},{\"x\":1}]",
        render("{{ a | uniq | json }}", map("a", list(map("x", Double.valueOf(1)), map("x", Double.valueOf(1)))), null));
  }

  @Test
  void slicesAndEdits () {
    assertEquals("string\u0001[2,3]",
        render("{{ a | slice: 1, 2 | json }}", map("a", list(Double.valueOf(1), Double.valueOf(2), Double.valueOf(3), Double.valueOf(4))), null));
    assertEquals("string\u0001[2]",
        render("{{ a | slice: -2 | json }}", map("a", list(Double.valueOf(1), Double.valueOf(2), Double.valueOf(3))), null));
    assertEquals("string\u0001[1,4]",
        render("{{ a | push: 4 | json }}", map("a", list(Double.valueOf(1))), null));
    assertEquals("string\u0001[1]",
        render("{{ a | pop | json }}", map("a", list(Double.valueOf(1), Double.valueOf(2))), null));
    assertEquals("string\u0001[0,1]",
        render("{{ a | unshift: 0 | json }}", map("a", list(Double.valueOf(1))), null));
    assertEquals("string\u0001[2]",
        render("{{ a | shift | json }}", map("a", list(Double.valueOf(1), Double.valueOf(2))), null));
    assertEquals("string\u0001[1,2]",
        render("{{ a | concat: b | json }}", map("a", list(Double.valueOf(1)), "b", list(Double.valueOf(2))), null));
  }

  @Test
  void readsAnObjectAsPairs () {
    assertEquals("string\u0001\"\"",
        render("{{ o | first | json }}", map("o", map("b", Double.valueOf(1), "a", Double.valueOf(2))), null));
    assertEquals("string\u00012=x;b=1;a=3;",
        render("{% for pair in o %}{{ pair[0] }}={{ pair[1] }};{% endfor %}", map("o", map("2", "x", "b", Double.valueOf(1), "a", Double.valueOf(3))), null));
  }
}
