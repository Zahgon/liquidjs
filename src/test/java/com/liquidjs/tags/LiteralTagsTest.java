/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 Jun Yang
 */
package com.liquidjs.tags;

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

/** The assign, capture, echo, raw, comment and liquid tags. */
class LiteralTagsTest {
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
  void assignsIntoTheTemplateScope () {
    assertEquals("string\u00011",
        render("{% assign a = 1 %}{{ a }}", null, null));
    assertEquals("string\u0001X",
        render("{% assign a = \"x\" | upcase %}{{ a }}", null, null));
    assertEquals("string\u00011",
        render("{% assign a = b %}{{ a }}", map("b", list(Double.valueOf(1))), null));
    assertEquals("string\u00012",
        render("{% for i in (1..2) %}{% assign a = i %}{% endfor %}{{ a }}", null, null));
  }

  @Test
  void capturesRenderedOutput () {
    assertEquals("string\u0001[aB]",
        render("{% capture c %}a{{ b }}{% endcapture %}[{{ c }}]", map("b", "B"), null));
    assertEquals("string\u0001[x]",
        render("{% capture 'c' %}x{% endcapture %}[{{ c }}]", null, null));
    assertEquals("string\u000112",
        render("{% capture c %}{% for i in (1..2) %}{{ i }}{% endfor %}{% endcapture %}{{ c }}", null, null));
  }

  @Test
  void echoesAValue () {
    assertEquals("string\u0001x",
        render("{% echo \"x\" %}", null, null));
    assertEquals("string\u0001B",
        render("{% echo b | upcase %}", map("b", "b"), null));
    assertEquals("string\u0001[]",
        render("[{% echo %}]", null, null));
  }

  @Test
  void writesRawBodies () {
    assertEquals("string\u0001{{ a }}",
        render("{% raw %}{{ a }}{% endraw %}", null, null));
    assertEquals("string\u0001{% if %}",
        render("{% raw %}{% if %}{% endraw %}", null, null));
    assertEquals("string\u0001[  x  ]",
        render("[{% raw %}  x  {% endraw %}]", null, null));
  }

  @Test
  void dropsComments () {
    assertEquals("string\u0001x",
        render("{% comment %}{{ a }}{% endcomment %}x", null, null));
    assertEquals("string\u0001x",
        render("{% # inline %}x", null, null));
    assertEquals("string\u0001x",
        render("{% #\n# two %}x", null, null));
  }

  @Test
  void readsTheMultilineTag () {
    assertEquals("string\u00011",
        render("{% liquid assign a = 1\necho a %}", null, null));
    assertEquals("string\u000112",
        render("{% liquid for i in (1..2)\n echo i\nendfor %}", null, null));
    assertEquals("string\u0001n",
        render("{% liquid if b\n echo \"y\"\nelse\n echo \"n\"\nendif %}", map("b", Boolean.FALSE), null));
  }

  @Test
  void readsOnlyOwnProperties () {
    assertEquals("string\u0001[]",
        render("[{{ o.toString }}]", map("o", map("a", Double.valueOf(1))), null));
    assertEquals("string\u0001[1]",
        render("[{{ o.a }}]", map("o", map("a", Double.valueOf(1))), null));
    assertEquals("string\u0001[1]",
        render("[{{ o.size }}]", map("o", map("a", Double.valueOf(1))), null));
    assertEquals("string\u0001[1][3][3]",
        render("[{{ a.first }}][{{ a.last }}][{{ a.size }}]", map("a", list(Double.valueOf(1), Double.valueOf(2), Double.valueOf(3))), null));
    assertEquals("string\u0001[3]",
        render("[{{ s.size }}]", map("s", "abc"), null));
  }
}
