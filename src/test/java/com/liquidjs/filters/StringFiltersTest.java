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

/** The filters over strings. */
class StringFiltersTest {
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
  void appendsAndPrepends () {
    assertEquals("string\u0001ab",
        render("{{ 'a' | append: 'b' }}", null, null));
    assertEquals("string\u0001ba",
        render("{{ 'a' | prepend: 'b' }}", null, null));
    assertEquals("string\u00010.3333333333333333x",
        render("{{ n | append: 'x' }}", map("n", Double.valueOf(0.3333333333333333)), null));
  }

  @Test
  void stripsTheWhitespaceOfTheLanguage () {
    assertEquals("string\u0001[x]",
        render("[{{ s | strip }}]", map("s", " \u00a0 x \ufeff"), null));
    assertEquals("string\u0001[x ]",
        render("[{{ s | lstrip }}]", map("s", "\u3000 x "), null));
    assertEquals("string\u0001[ x]",
        render("[{{ s | rstrip }}]", map("s", " x \u2028"), null));
    assertEquals("string\u0001[c]",
        render("[{{ s | strip: \"ab\" }}]", map("s", "abcba"), null));
  }

  @Test
  void changesCase () {
    assertEquals("string\u0001STRASSE",
        render("{{ s | upcase }}", map("s", "stra\u00dfe"), null));
    assertEquals("string\u0001\u03c3\u03bf\u03c6\u03bf\u03c2",
        render("{{ s | downcase }}", map("s", "\u03a3\u039f\u03a6\u039f\u03a3"), null));
    assertEquals("string\u0001Hello world",
        render("{{ s | capitalize }}", map("s", "hello WORLD"), null));
  }

  @Test
  void truncates () {
    assertEquals("string\u0001abcde...",
        render("{{ s | truncate: 8 }}", map("s", "abcdefghij"), null));
    assertEquals("string\u0001short",
        render("{{ s | truncate }}", map("s", "short"), null));
    assertEquals("string\u0001abcd>",
        render("{{ s | truncate: 5, \">\" }}", map("s", "abcdefghij"), null));
    assertEquals("string\u0001a b...",
        render("{{ s | truncatewords: 2 }}", map("s", "a b c d"), null));
    assertEquals("string\u0001a...",
        render("{{ s | truncatewords: 0 }}", map("s", "a b c"), null));
  }

  @Test
  void splitsAndJoins () {
    assertEquals("string\u0001[\"a\",\"b\",\"\",\"c\"]",
        render("{{ s | split: \",\" | json }}", map("s", "a,b,,c,,"), null));
    assertEquals("string\u0001[\"a\",\"b\"]",
        render("{{ s | split: \"\" | json }}", map("s", "ab"), null));
    assertEquals("string\u00013",
        render("{{ s | number_of_words }}", map("s", "a \u00a0 b  c"), null));
    assertEquals("string\u00013",
        render("{{ s | number_of_words: \"cjk\" }}", map("s", "\u4e2d\u6587 words"), null));
  }

  @Test
  void replacesAndRemoves () {
    assertEquals("string\u0001bAnAnA",
        render("{{ s | replace: \"a\", \"A\" }}", map("s", "banana"), null));
    assertEquals("string\u0001bAnana",
        render("{{ s | replace_first: \"a\", \"A\" }}", map("s", "banana"), null));
    assertEquals("string\u0001bananA",
        render("{{ s | replace_last: \"a\", \"A\" }}", map("s", "banana"), null));
    assertEquals("string\u0001bnn",
        render("{{ s | remove: \"a\" }}", map("s", "banana"), null));
    assertEquals("string\u0001banan",
        render("{{ s | remove_last: \"a\" }}", map("s", "banana"), null));
  }

  @Test
  void normalisesWhitespace () {
    assertEquals("string\u0001[a b c]",
        render("[{{ s | squish }}]", map("s", "  a \t b \u00a0 c  "), null));
    assertEquals("string\u0001[ a b ]",
        render("[{{ s | normalize_whitespace }}]", map("s", " a \n b "), null));
    assertEquals("string\u0001[abc]",
        render("[{{ s | strip_newlines }}]", map("s", "a\r\nb\nc"), null));
  }

  @Test
  void writesASentence () {
    assertEquals("string\u0001a, b, and c",
        render("{{ a | array_to_sentence_string }}", map("a", list("a", "b", "c")), null));
    assertEquals("string\u0001a or b",
        render("{{ a | array_to_sentence_string: \"or\" }}", map("a", list("a", "b")), null));
    assertEquals("string\u0001",
        render("{{ a | array_to_sentence_string }}", map("a", list()), null));
  }
}
