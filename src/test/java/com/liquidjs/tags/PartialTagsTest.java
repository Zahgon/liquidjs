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

/** The include, render, layout and block tags. */
class PartialTagsTest {
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
  void includesInTheCurrentScope () {
    assertEquals("string\u0001PV",
        render("{% include \"p\" %}", map("v", "V"), map("templates", map("p", "P{{ v }}"))));
    assertEquals("string\u0001PA",
        render("{% include \"p\", v: \"A\" %}", map("v", "V"), map("templates", map("p", "P{{ v }}"))));
    assertEquals("string\u0001PW",
        render("{% include \"p\" with \"W\" %}", null, map("templates", map("p", "P{{ p }}"))));
    assertEquals("string\u0001PA",
        render("{% assign v = \"A\" %}{% include \"p\" %}", null, map("templates", map("p", "P{{ v }}"))));
  }

  @Test
  void rendersInAScopeOfItsOwn () {
    assertEquals("string\u0001P",
        render("{% render \"p\" %}", map("v", "V"), map("templates", map("p", "P{{ v }}"))));
    assertEquals("string\u0001PA",
        render("{% render \"p\", v: \"A\" %}", map("v", "V"), map("templates", map("p", "P{{ v }}"))));
    assertEquals("string\u0001PW",
        render("{% render \"p\" with \"W\" as w %}", null, map("templates", map("p", "P{{ w }}"))));
    assertEquals("string\u00011122",
        render("{% render \"p\" for a as i %}", map("a", list(Double.valueOf(1), Double.valueOf(2))), map("templates", map("p", "{{ i }}{{ forloop.index }}"))));
  }

  @Test
  void passesParametersAsJekyllDoes () {
    assertEquals("RenderError\u0001ENOENT: Failed to lookup \"\"p\"\" in \".\", line:1, col:1\u0001>> 1| {% include \"p\" %}\n      ^",
        raised(() -> render("{% include \"p\" %}", map("v", "V"), map("jekyllInclude", Boolean.TRUE, "templates", map("p", "P{{ include.v }}")))));
    assertEquals("RenderError\u0001ENOENT: Failed to lookup \"\"p\"\" in \".\", line:1, col:1\u0001>> 1| {% include \"p\" v=\"A\" %}\n      ^",
        raised(() -> render("{% include \"p\" v=\"A\" %}", null, map("jekyllInclude", Boolean.TRUE, "templates", map("p", "P{{ include.v }}")))));
  }

  @Test
  void fillsTheBlocksOfALayout () {
    assertEquals("string\u0001<B>",
        render("{% layout \"l\" %}{% block %}B{% endblock %}", null, map("templates", map("l", "<{% block %}d{% endblock %}>"))));
    assertEquals("string\u0001<>",
        render("{% layout \"l\" %}", null, map("templates", map("l", "<{% block %}d{% endblock %}>"))));
    assertEquals("string\u0001<d+B>",
        render("{% layout \"l\" %}{% block %}{{ block.super }}+B{% endblock %}", null, map("templates", map("l", "<{% block %}d{% endblock %}>"))));
    assertEquals("string\u0001<A|>",
        render("{% layout \"l\" %}{% block \"a\" %}A{% endblock %}", null, map("templates", map("l", "<{% block \"a\" %}da{% endblock %}|{% block %}db{% endblock %}>"))));
    assertEquals("string\u0001B",
        render("{% layout none %}{% block %}B{% endblock %}", null, map("templates", map("l", "<>"))));
  }

  @Test
  void resolvesTheNameOfAPartial () {
    assertEquals("string\u0001P",
        render("{% include f %}", map("f", "p"), map("templates", map("p", "P"))));
    assertEquals("RenderError\u0001ENOENT: Failed to lookup \"\"p\"\" in \".\", line:1, col:1\u0001>> 1| {% include \"p\" %}\n      ^",
        raised(() -> render("{% include \"p\" %}", null, map("dynamicPartials", Boolean.FALSE, "templates", map("p", "P")))));
    assertEquals("string\u0001P",
        render("{% include \"dir/p\" %}", null, map("templates", map("dir/p", "P"))));
    assertEquals("string\u0001P",
        render("{% include \"p\" %}", null, map("extname", ".liquid", "templates", map("p.liquid", "P"))));
  }
}
