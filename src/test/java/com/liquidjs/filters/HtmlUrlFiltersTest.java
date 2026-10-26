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

/** The HTML and URL filters. */
class HtmlUrlFiltersTest {
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
  void escapesTheFiveCharacters () {
    assertEquals("string\u0001a&amp;b&lt;c&gt;d&#34;e&#39;f",
        render("{{ s | escape }}", map("s", "a&b<c>d\"e'f"), null));
    assertEquals("string\u0001&amp;&lt;x",
        render("{{ s | escape_once }}", map("s", "&amp;&lt;x"), null));
    assertEquals("string\u0001&amp;amp;",
        render("{{ s | escape_once }}", map("s", "&amp;amp;"), null));
    assertEquals("string\u0001&lt;a&gt;",
        render("{{ s | xml_escape }}", map("s", "<a>"), null));
  }

  @Test
  void stripsMarkup () {
    assertEquals("string\u0001para",
        render("{{ s | strip_html }}", map("s", "<p>para</p>"), null));
    assertEquals("string\u0001tail",
        render("{{ s | strip_html }}", map("s", "<script>alert(1)</script>tail"), null));
    assertEquals("string\u0001t",
        render("{{ s | strip_html }}", map("s", "<!-- c -->t"), null));
    assertEquals("string\u0001<unclosed",
        render("{{ s | strip_html }}", map("s", "<unclosed"), null));
    assertEquals("string\u0001a<br />\nb",
        render("{{ s | newline_to_br }}", map("s", "a\r\nb"), null));
  }

  @Test
  void percentEncodes () {
    assertEquals("string\u0001a+b!*()'~-_.",
        render("{{ s | url_encode }}", map("s", "a b!*()'~-_."), null));
    assertEquals("string\u0001a b c",
        render("{{ s | url_decode }}", map("s", "a+b%20c"), null));
    assertEquals("string\u0001a+b%21%27%28%29%2A",
        render("{{ s | cgi_escape }}", map("s", "a b!'()*"), null));
    assertEquals("string\u0001a%20b[]%3C%3E",
        render("{{ s | uri_escape }}", map("s", "a b[]<>"), null));
  }

  @Test
  void makesSlugs () {
    assertEquals("string\u0001hello-w\u00f6rld-123",
        render("{{ s | slugify }}", map("s", "Hello, W\u00f6rld! 123"), null));
    assertEquals("string\u0001a-b",
        render("{{ s | slugify: \"raw\" }}", map("s", " a  b "), null));
    assertEquals("string\u0001a.b~c!d'e",
        render("{{ s | slugify: \"pretty\" }}", map("s", "a.b~c!d'e"), null));
    assertEquals("string\u0001caf-bar",
        render("{{ s | slugify: \"ascii\" }}", map("s", "caf\u00e9 bar"), null));
    assertEquals("string\u0001aaessoe",
        render("{{ s | slugify: \"latin\" }}", map("s", "\u00e0\u00e6\u00df\u0153"), null));
    assertEquals("string\u0001Hello-World",
        render("{{ s | slugify: \"default\", true }}", map("s", "Hello World"), null));
  }
}
