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

/** The default, JSON, base 64 and hash filters. */
class MiscFiltersTest {
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
  void writesJsonInKeyOrder () {
    assertEquals("string\u0001{\"2\":\"x\",\"10\":\"y\",\"b\":1,\"a\":3}",
        render("{{ o | json }}", map("o", map("2", "x", "10", "y", "b", Double.valueOf(1), "a", Double.valueOf(3))), null));
    assertEquals("string\u0001{\n  \"a\": [\n    1,\n    2\n  ]\n}",
        render("{{ o | json: 2 }}", map("o", map("a", list(Double.valueOf(1), Double.valueOf(2)))), null));
    assertEquals("string\u0001\"\u2028 and \\\"quotes\\\"\"",
        render("{{ v | json }}", map("v", "\u2028 and \"quotes\""), null));
    assertEquals("string\u0001[null,1,true]",
        render("{{ v | json }}", map("v", list(null, Double.valueOf(1), Boolean.TRUE)), null));
  }

  @Test
  void marksACycleWhenInspecting () {
    assertEquals("string\u0001{\"a\":{\"b\":1}}",
        render("{{ o | inspect }}", map("o", map("a", map("b", Double.valueOf(1)))), null));
    assertEquals("string\u0001{\n \"a\": 1\n}",
        render("{{ o | inspect: 1 }}", map("o", map("a", Double.valueOf(1))), null));
  }

  @Test
  void fallsBackToADefault () {
    assertEquals("string\u0001[D]",
        render("[{{ v | default: \"D\" }}]", map("v", ""), null));
    assertEquals("string\u0001[0]",
        render("[{{ v | default: \"D\" }}]", map("v", Double.valueOf(0)), null));
    assertEquals("string\u0001[D]",
        render("[{{ v | default: \"D\" }}]", map("v", Boolean.FALSE), null));
    assertEquals("string\u0001[false]",
        render("[{{ v | default: \"D\", allow_false: true }}]", map("v", Boolean.FALSE), null));
    assertEquals("string\u0001[D]",
        render("[{{ v | default: \"D\" }}]", map("v", list()), null));
  }

  @Test
  void encodesBase64Leniently () {
    assertEquals("string\u0001aGVsbG8=",
        render("{{ s | base64_encode }}", map("s", "hello"), null));
    assertEquals("string\u0001hello",
        render("{{ s | base64_decode }}", map("s", "aGVsbG8="), null));
    assertEquals("string\u0001hello",
        render("{{ s | base64_decode }}", map("s", "a!GVsbG8"), null));
    assertEquals("string\u0001a",
        render("{{ s | base64_decode }}", map("s", "YQ"), null));
  }

  @Test
  void hashes () {
    assertEquals("string\u00012cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824",
        render("{{ s | sha256 }}", map("s", "hello"), null));
    assertEquals("string\u00019307b3b915efb5171ff14d8cb55fbcc798c6c0ef1456d66ded1a6aa723a58b7b",
        render("{{ s | hmac_sha256: \"key\" }}", map("s", "hello"), null));
    assertEquals("string\u00014352b26e33fe0d769a8922a6ba29004109f01688e26acc9e6cb347e5a5afc4da",
        render("{{ s | hmac_sha256: \"\" }}", map("s", "hello"), null));
  }

  @Test
  void convertsToANumber () {
    assertEquals("string\u00015",
        render("{{ v | to_integer }}", map("v", "5"), null));
    assertEquals("string\u00015.5",
        render("{{ v | to_integer }}", map("v", "5.5"), null));
    assertEquals("string\u0001NaN",
        render("{{ v | to_integer }}", map("v", "abc"), null));
  }
}
