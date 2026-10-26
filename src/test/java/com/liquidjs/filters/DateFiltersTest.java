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

/** The date filters. */
class DateFiltersTest {
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
  void parsesTheFormsTheRuntimeAccepts () {
    assertEquals("string\u00012021-01-01 10:20:30",
        render("{{ d | date: \"%Y-%m-%d %H:%M:%S\" }}", map("d", "2021-01-01T10:20:30Z"), null));
    assertEquals("string\u00012021-01-01",
        render("{{ d | date: \"%Y-%m-%d\" }}", map("d", "2021/01/01"), null));
    assertEquals("string\u00012021-01-01",
        render("{{ d | date: \"%Y-%m-%d\" }}", map("d", "January 1, 2021"), null));
    assertEquals("string\u00012021-03-09 10:00",
        render("{{ d | date: \"%Y-%m-%d %H:%M\" }}", map("d", "Tue Mar 09 2021 10:00:00"), null));
    assertEquals("string\u00012021-13-45",
        render("{{ d | date: \"%Y\" }}", map("d", "2021-13-45"), null));
    assertEquals("string\u00012021-01-01",
        render("{{ d | date: \"%Y-%m-%d\" }}", map("d", Double.valueOf(1609459200)), null));
  }

  @Test
  void formatsEveryConversion () {
    assertEquals("string\u0001Tuesday March 9 2021",
        render("{{ d | date: \"%A %B %-e %Y\" }}", map("d", "2021-03-09T07:08:09.045Z"), null));
    assertEquals("string\u0001Tue Mar  9 07:08:09 2021",
        render("{{ d | date: \"%a %b %e %H:%M:%S %Y\" }}", map("d", "2021-03-09T07:08:09.045Z"), null));
    assertEquals("string\u0001068 10 10 2 2",
        render("{{ d | date: \"%j %U %W %u %w\" }}", map("d", "2021-03-09T07:08:09.045Z"), null));
    assertEquals("string\u000107:08 PM| 7:08 pm",
        render("{{ d | date: \"%I:%M %p|%l:%M %P\" }}", map("d", "2021-03-09T19:08:09Z"), null));
    assertEquals("string\u0001045000000|045|045",
        render("{{ d | date: \"%N|%3N|%L\" }}", map("d", "2021-03-09T07:08:09.045Z"), null));
    assertEquals("string\u0001th|2021|1615273689",
        render("{{ d | date: \"%q|%C%y|%s\" }}", map("d", "2021-03-09T07:08:09.045Z"), null));
  }

  @Test
  void appliesTheFlagsAndTheWidths () {
    assertEquals("string\u0001TUESDAY|MARCH|9| 9|00009",
        render("{{ d | date: \"%^A|%#B|%-d|%_d|%05d\" }}", map("d", "2021-03-09T07:08:09Z"), null));
    assertEquals("string\u0001+0000|+00:00",
        render("{{ d | date: \"%z|%:z\" }}", map("d", "2021-03-09T07:08:09Z"), null));
    assertEquals("string\u0001\t\n%",
        render("{{ d | date: \"%t%n%%\" }}", map("d", "2021-03-09T07:08:09Z"), null));
  }

  @Test
  void shiftsToATimezone () {
    assertEquals("string\u00012021-07-01 14:00 +0200",
        render("{{ d | date: \"%Y-%m-%d %H:%M %z\" }}", map("d", "2021-07-01T12:00:00Z"), map("timezoneOffset", Double.valueOf(-120))));
    assertEquals("string\u00012021-07-01 20:00 +0800",
        render("{{ d | date: \"%Y-%m-%d %H:%M %z\" }}", map("d", "2021-07-01T12:00:00Z"), map("timezoneOffset", "Asia/Shanghai")));
    assertEquals("string\u00012021-08-06 02:29 +0800",
        render("{{ d | date: \"%Y-%m-%d %H:%M %z\" }}", map("d", "2021-08-06T02:29:00.000+08:00"), map("preserveTimezones", Boolean.TRUE)));
  }

  @Test
  void formatsWithTheJekyllFilters () {
    assertEquals("string\u00012021-01-02T03:04:05+00:00",
        render("{{ d | date_to_xmlschema }}", map("d", "2021-01-02T03:04:05Z"), null));
    assertEquals("string\u0001Sat, 02 Jan 2021 03:04:05 +0000",
        render("{{ d | date_to_rfc822 }}", map("d", "2021-01-02T03:04:05Z"), null));
    assertEquals("string\u000102 Jan 2021",
        render("{{ d | date_to_string }}", map("d", "2021-01-02T03:04:05Z"), null));
    assertEquals("string\u0001January 2nd, 2021",
        render("{{ d | date_to_long_string: \"ordinal\", \"US\" }}", map("d", "2021-01-02T03:04:05Z"), null));
  }

  @Test
  void usesTheDefaultFormat () {
    assertEquals("string\u0001Saturday, January 2, 2021 at 3:04 am +0000",
        render("{{ d | date }}", map("d", "2021-01-02T03:04:05Z"), null));
    assertEquals("string\u00012021/01",
        render("{{ d | date }}", map("d", "2021-01-02T03:04:05Z"), map("dateFormat", "%Y/%m")));
  }
}
