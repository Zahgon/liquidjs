/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 Jun Yang
 */
package com.liquidjs.template;

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

/** The static analysis reports. */
class StaticAnalysisTest {
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
  void listsVariables () {
    assertEquals("json\u0001[\"a\",\"b\"]",
        names(new Liquid(null == null ? map() : null).variablesSync(new Liquid(null == null ? map() : null).parse("{{ a }}{{ b.c }}"), new StaticAnalysisOptions(true))));
    assertEquals("json\u0001[\"a.b[c].d\",\"c\"]",
        names(new Liquid(null == null ? map() : null).fullVariablesSync(new Liquid(null == null ? map() : null).parse("{{ a.b[c].d }}"), new StaticAnalysisOptions(true))));
    assertEquals("json\u0001[[\"a\",\"b\",[\"c\"],\"d\"],[\"c\"]]",
        names(new Liquid(null == null ? map() : null).variableSegmentsSync(new Liquid(null == null ? map() : null).parse("{{ a.b[c].d }}"), new StaticAnalysisOptions(true))));
    assertEquals("json\u0001[\"a['b c']\"]",
        names(new Liquid(null == null ? map() : null).fullVariablesSync(new Liquid(null == null ? map() : null).parse("{{ a[\"b c\"] }}"), new StaticAnalysisOptions(true))));
  }

  @Test
  void listsWhatTheTemplateExpectsToBeGiven () {
    assertEquals("json\u0001[\"a\",\"y\"]",
        names(new Liquid(null == null ? map() : null).globalVariablesSync(new Liquid(null == null ? map() : null).parse("{% assign x = a %}{{ x }}{{ y }}"), new StaticAnalysisOptions(true))));
    assertEquals("json\u0001[\"a\"]",
        names(new Liquid(null == null ? map() : null).globalVariablesSync(new Liquid(null == null ? map() : null).parse("{% for i in a %}{{ i.b }}{{ forloop.index }}{% endfor %}"), new StaticAnalysisOptions(true))));
    assertEquals("json\u0001[\"a\"]",
        names(new Liquid(null == null ? map() : null).globalFullVariablesSync(new Liquid(null == null ? map() : null).parse("{% capture x %}{{ a }}{% endcapture %}{{ x }}"), new StaticAnalysisOptions(true))));
    assertEquals("json\u0001[[\"a\",[\"b\",[\"c\"]]],[\"b\",[\"c\"]],[\"c\"]]",
        names(new Liquid(null == null ? map() : null).globalVariableSegmentsSync(new Liquid(null == null ? map() : null).parse("{{ a[b[c]] }}"), new StaticAnalysisOptions(true))));
  }

  @Test
  void locatesEveryReference () {
    assertEquals("json\u0001{\"variables\":{\"a\":[\"a@1:4\",\"a.b@2:4\"]},\"globals\":{\"a\":[\"a@1:4\",\"a.b@2:4\"]},\"locals\":{}}",
        analysis("{{ a }}\n{{ a.b }}", null));
    assertEquals("json\u0001{\"variables\":{\"x\":[\"x@1:22\"],\"y\":[\"y@1:29\"]},\"globals\":{\"y\":[\"y@1:29\"]},\"locals\":{\"x\":[\"x@1:11\"]}}",
        analysis("{% assign x = 1 %}{{ x }}{{ y }}", null));
  }

  @Test
  void followsPartials () {
    assertEquals("json\u0001[\"a\",\"b\"]",
        names(new Liquid(map("templates", map("p", "{{ a }}{{ b }}")) == null ? map() : map("templates", map("p", "{{ a }}{{ b }}"))).globalVariablesSync(new Liquid(map("templates", map("p", "{{ a }}{{ b }}")) == null ? map() : map("templates", map("p", "{{ a }}{{ b }}"))).parse("{% include \"p\" %}"), new StaticAnalysisOptions(true))));
    assertEquals("json\u0001[\"x\",\"b\"]",
        names(new Liquid(map("templates", map("p", "{{ a }}{{ b }}")) == null ? map() : map("templates", map("p", "{{ a }}{{ b }}"))).globalVariablesSync(new Liquid(map("templates", map("p", "{{ a }}{{ b }}")) == null ? map() : map("templates", map("p", "{{ a }}{{ b }}"))).parse("{% render \"p\" with x as a %}"), new StaticAnalysisOptions(true))));
  }
}
