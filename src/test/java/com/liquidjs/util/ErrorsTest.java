/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 Jun Yang
 */
package com.liquidjs.util;

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

/** The errors, their messages and their context. */
class ErrorsTest {
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
  void reportsTokenizationErrors () {
    assertEquals("string\u0001",
        render("{{ a | }}", null, null));
    assertEquals("TokenizationError\u0001invalid range syntax, line:1, col:10\u0001>> 1| {{ (1..2 }}\n               ^",
        raised(() -> render("{{ (1..2 }}", null, null)));
    assertEquals("TokenizationError\u0001raw \"{% raw %}never closed\" not closed, line:1, col:10\u0001>> 1| {% raw %}never closed\n               ^",
        raised(() -> render("{% raw %}never closed", null, null)));
    assertEquals("TokenizationError\u0001output \"{{ \\\"x }}\" not closed, line:1, col:1\u0001>> 1| {{ \"x }}\n      ^",
        raised(() -> render("{{ \"x }}", null, null)));
    assertEquals("string\u0001before\n\nafter",
        render("before\n{{ a | }}\nafter", null, null));
  }

  @Test
  void reportsParseErrors () {
    assertEquals("ParseError\u0001tag \"nosuchtag\" not found, line:1, col:1\u0001>> 1| {% nosuchtag %}\n      ^",
        raised(() -> render("{% nosuchtag %}", null, null)));
    assertEquals("ParseError\u0001illegal tag: {% for i in %}, line:1, col:1\u0001>> 1| {% for i in %}{% endfor %}\n      ^",
        raised(() -> render("{% for i in %}{% endfor %}", null, null)));
    assertEquals("ParseError\u0001tag {% if a %} not closed, line:1, col:1\u0001>> 1| {% if a %}\n      ^",
        raised(() -> render("{% if a %}", null, null)));
    assertEquals("ParseError\u0001undefined filter: nosuchfilter, line:1, col:1\u0001>> 1| {{ \"a\" | nosuchfilter }}\n      ^",
        raised(() -> render("{{ \"a\" | nosuchfilter }}", null, map("strictFilters", Boolean.TRUE))));
  }

  @Test
  void reportsUndefinedVariables () {
    assertEquals("UndefinedVariableError\u0001undefined variable: missing, line:1, col:4\u0001>> 1| {{ missing }}\n         ^",
        raised(() -> render("{{ missing }}", null, map("strictVariables", Boolean.TRUE))));
    assertEquals("UndefinedVariableError\u0001undefined variable: a.missing, line:1, col:4\u0001>> 1| {{ a.missing }}\n         ^",
        raised(() -> render("{{ a.missing }}", map("a", map()), map("strictVariables", Boolean.TRUE))));
    assertEquals("string\u0001",
        render("{% if missing %}x{% endif %}", null, map("strictVariables", Boolean.TRUE, "lenientIf", Boolean.TRUE)));
    assertEquals("UndefinedVariableError\u0001undefined variable: missing, line:3, col:4\u0001   1| l1\n   2| l2\n>> 3| {{ missing }}\n         ^",
        raised(() -> render("l1\nl2\n{{ missing }}", null, map("strictVariables", Boolean.TRUE))));
  }

  @Test
  void collectsEveryError () {
    assertEquals("LiquidErrors\u00012 errors found, line:1, col:4\u0001>> 1| {{ m1 }}{{ m2 }}\n         ^",
        raised(() -> render("{{ m1 }}{{ m2 }}", null, map("strictVariables", Boolean.TRUE, "catchAllErrors", Boolean.TRUE))));
    assertEquals("LiquidErrors\u00011 error found, line:1, col:4\u0001>> 1| {{ m1 }}\n         ^",
        raised(() -> render("{{ m1 }}", null, map("strictVariables", Boolean.TRUE, "catchAllErrors", Boolean.TRUE))));
  }

  @Test
  void namesTheFileOfAPartial () {
    assertEquals("UndefinedVariableError\u0001undefined variable: missing, line:1, col:4\u0001>> 1| {{ missing }}\n         ^",
        raised(() -> render("{% include \"p\" %}", null, map("strictVariables", Boolean.TRUE, "templates", map("p", "{{ missing }}")))));
  }

  @Test
  void raisesWhenALimitIsPassed () {
    assertEquals("RenderError\u0001memory alloc limit exceeded, line:1, col:1\u0001>> 1| {{ \"abc\" | append: \"def\" }}\n      ^",
        raised(() -> render("{{ \"abc\" | append: \"def\" }}", null, map("memoryLimit", Double.valueOf(3)))));
    assertEquals("string\u0001abcdef",
        render("{{ \"abc\" | append: \"def\" }}", null, map("memoryLimit", Double.valueOf(6))));
  }
}
