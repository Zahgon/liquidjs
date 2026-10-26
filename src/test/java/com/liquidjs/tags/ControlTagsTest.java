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

/** The control flow tags. */
class ControlTagsTest {
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
  void loopsOverACollection () {
    assertEquals("string\u0001123",
        render("{% for i in a %}{{ i }}{% endfor %}", map("a", list(Double.valueOf(1), Double.valueOf(2), Double.valueOf(3))), null));
    assertEquals("string\u000123",
        render("{% for i in a limit: 2 offset: 1 %}{{ i }}{% endfor %}", map("a", list(Double.valueOf(1), Double.valueOf(2), Double.valueOf(3), Double.valueOf(4))), null));
    assertEquals("string\u000121",
        render("{% for i in a reversed %}{{ i }}{% endfor %}", map("a", list(Double.valueOf(1), Double.valueOf(2))), null));
    assertEquals("string\u0001none",
        render("{% for i in a %}x{% else %}none{% endfor %}", map("a", list()), null));
    assertEquals("string\u0001123",
        render("{% for i in (1..3) %}{{ i }}{% endfor %}", null, null));
  }

  @Test
  void exposesTheLoopState () {
    assertEquals("string\u00011truefalse2;2falsetrue1;",
        render("{% for i in a %}{{ forloop.index }}{{ forloop.first }}{{ forloop.last }}{{ forloop.rindex }};{% endfor %}", map("a", list(Double.valueOf(1), Double.valueOf(2))), null));
    assertEquals("string\u0001i-a|1",
        render("{% for i in a %}{{ forloop.name }}|{{ forloop.length }}{% endfor %}", map("a", list(Double.valueOf(1))), null));
    assertEquals("string\u0001{\"i\":0,\"length\":1,\"name\":\"i-a\"}",
        render("{% for i in a %}{{ forloop }}{% endfor %}", map("a", list(Double.valueOf(1))), null));
  }

  @Test
  void breaksAndContinues () {
    assertEquals("string\u00011",
        render("{% for i in a %}{% if i == 2 %}{% break %}{% endif %}{{ i }}{% endfor %}", map("a", list(Double.valueOf(1), Double.valueOf(2), Double.valueOf(3))), null));
    assertEquals("string\u000113",
        render("{% for i in a %}{% if i == 2 %}{% continue %}{% endif %}{{ i }}{% endfor %}", map("a", list(Double.valueOf(1), Double.valueOf(2), Double.valueOf(3))), null));
  }

  @Test
  void continuesWhereItLeftOff () {
    assertEquals("string\u00011234",
        render("{% for i in a limit: 2 %}{{ i }}{% endfor %}{% for i in a offset: continue %}{{ i }}{% endfor %}", map("a", list(Double.valueOf(1), Double.valueOf(2), Double.valueOf(3), Double.valueOf(4))), null));
  }

  @Test
  void laysOutATable () {
    assertEquals("string\u0001<tr class=\"row1\"><td class=\"col1\">1</td><td class=\"col2\">2</td></tr><tr class=\"row2\"><td class=\"col1\">3</td></tr>",
        render("{% tablerow i in a cols: 2 %}{{ i }}{% endtablerow %}", map("a", list(Double.valueOf(1), Double.valueOf(2), Double.valueOf(3))), null));
    assertEquals("string\u0001<tr class=\"row1\"><td class=\"col1\">1</td><td class=\"col2\">2</td></tr>",
        render("{% tablerow i in a %}{{ i }}{% endtablerow %}", map("a", list(Double.valueOf(1), Double.valueOf(2))), null));
    assertEquals("string\u0001<tr class=\"row1\"><td class=\"col1\">1:1:true;</td><td class=\"col2\">1:2:false;</td></tr><tr class=\"row2\"><td class=\"col1\">2:1:true;</td></tr>",
        render("{% tablerow i in a cols: 2 %}{{ tablerowloop.row }}:{{ tablerowloop.col }}:{{ tablerowloop.col_first }};{% endtablerow %}", map("a", list(Double.valueOf(1), Double.valueOf(2), Double.valueOf(3))), null));
    assertEquals("string\u0001",
        render("{% tablerow i in a %}{{ i }}{% endtablerow %}", map("a", list()), null));
  }

  @Test
  void cyclesThroughCandidates () {
    assertEquals("string\u0001xyx",
        render("{% for i in a %}{% cycle \"x\", \"y\" %}{% endfor %}", map("a", list(Double.valueOf(1), Double.valueOf(2), Double.valueOf(3))), null));
    assertEquals("string\u0001xxyy",
        render("{% for i in a %}{% cycle \"g\": \"x\", \"y\" %}{% cycle \"h\": \"x\", \"y\" %}{% endfor %}", map("a", list(Double.valueOf(1), Double.valueOf(2))), null));
  }

  @Test
  void countsUpAndDown () {
    assertEquals("string\u0001010",
        render("{% increment a %}{% increment a %}{% increment b %}", null, null));
    assertEquals("string\u0001-1-2",
        render("{% decrement a %}{% decrement a %}", null, null));
    assertEquals("string\u000101",
        render("{% increment a %}{{ a }}", null, null));
    assertEquals("string\u0001556",
        render("{{ a }}{% increment a %}{{ a }}", map("a", Double.valueOf(5)), null));
  }

  @Test
  void choosesABranch () {
    assertEquals("string\u0001B",
        render("{% if a %}A{% elsif b %}B{% else %}C{% endif %}", map("a", Boolean.FALSE, "b", Boolean.TRUE), null));
    assertEquals("string\u0001A",
        render("{% unless a %}A{% else %}B{% endunless %}", map("a", Boolean.FALSE), null));
    assertEquals("string\u0001two",
        render("{% case a %}{% when 1 %}one{% when 2, 3 %}two{% else %}other{% endcase %}", map("a", Double.valueOf(3)), null));
    assertEquals("string\u0001num",
        render("{% case a %}{% when \"1\" %}str{% when 1 %}num{% endcase %}", map("a", Double.valueOf(1)), null));
  }
}
