/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 Jun Yang
 */
package com.liquidjs.filters;

import com.liquidjs.template.FilterHandler;
import com.liquidjs.template.FilterImpl;
import com.liquidjs.util.Underscore;

import java.util.LinkedHashMap;
import java.util.Map;

/** The filters that escape or strip HTML. */
public final class HtmlFilters {
  private HtmlFilters () {}

  private static final Map<Character, String> ESCAPE_MAP = escapeMap();
  private static final Map<String, String> UNESCAPE_MAP = unescapeMap();

  private static Map<Character, String> escapeMap () {
    Map<Character, String> map = new LinkedHashMap<>();
    map.put('&', "&amp;");
    map.put('<', "&lt;");
    map.put('>', "&gt;");
    map.put('"', "&#34;");
    map.put('\'', "&#39;");
    return map;
  }

  private static Map<String, String> unescapeMap () {
    Map<String, String> map = new LinkedHashMap<>();
    map.put("&amp;", "&");
    map.put("&lt;", "<");
    map.put("&gt;", ">");
    map.put("&#34;", "\"");
    map.put("&#39;", "'");
    return map;
  }

  /** Register these filters under the names a template calls them by. */
  public static void register (Map<String, Object> filters) {
    filters.put("escape", (FilterHandler) (impl, v, args) -> escape(impl, v));
    filters.put("xml_escape", (FilterHandler) (impl, v, args) -> escape(impl, v));
    filters.put("escape_once", (FilterHandler) (impl, v, args) ->
        escape(impl, unescape(impl, v)));
    filters.put("newline_to_br", (FilterHandler) (impl, v, args) -> {
      String str = Underscore.stringify(v);
      impl.context.memoryLimit.use(str.length());
      return str.replaceAll("\r?\n", "<br />\n");
    });
    filters.put("strip_html", (FilterHandler) (impl, v, args) -> {
      String str = Underscore.stringify(v);
      impl.context.memoryLimit.use(str.length());
      return stripHtml(str);
    });
  }

  private static String escape (FilterImpl impl, Object value) {
    String str = Underscore.stringify(value);
    impl.context.memoryLimit.use(str.length());
    StringBuilder out = new StringBuilder();
    for (int i = 0; i < str.length(); i++) {
      char ch = str.charAt(i);
      String replacement = ESCAPE_MAP.get(ch);
      if (replacement == null) out.append(ch);
      else out.append(replacement);
    }
    return out.toString();
  }

  private static String unescape (FilterImpl impl, Object value) {
    String str = Underscore.stringify(value);
    impl.context.memoryLimit.use(str.length());
    StringBuilder out = new StringBuilder();
    int i = 0;
    while (i < str.length()) {
      boolean matched = false;
      if (str.charAt(i) == '&') {
        for (Map.Entry<String, String> entry : UNESCAPE_MAP.entrySet()) {
          if (str.startsWith(entry.getKey(), i)) {
            out.append(entry.getValue());
            i += entry.getKey().length();
            matched = true;
            break;
          }
        }
      }
      if (!matched) out.append(str.charAt(i++));
    }
    return out.toString();
  }

  /** The escape the outputEscape option names. */
  public static String escapeValue (Object value) {
    String str = Underscore.stringify(value);
    StringBuilder out = new StringBuilder();
    for (int i = 0; i < str.length(); i++) {
      char ch = str.charAt(i);
      String replacement = ESCAPE_MAP.get(ch);
      if (replacement == null) out.append(ch);
      else out.append(replacement);
    }
    return out.toString();
  }

  /**
   * Strips the raw text blocks of HTML and then every remaining element; a regular expression
   * doing the same takes quadratic time on an opener that is never closed.
   */
  static String stripHtml (String str) {
    Map<String, String> blocks = new LinkedHashMap<>();
    blocks.put("<script", "</script>");
    blocks.put("<style", "</style>");
    blocks.put("<!--", "-->");
    blocks.put("<", ">");
    StringBuilder out = new StringBuilder();
    int i = 0;
    while (i < str.length()) {
      int lt = str.indexOf('<', i);
      if (lt < 0) return out + str.substring(i);
      out.append(str, i, lt);
      for (Map.Entry<String, String> entry : new LinkedHashMap<>(blocks).entrySet()) {
        String opener = entry.getKey();
        if (!str.startsWith(opener, lt)) continue;
        int e = str.indexOf(entry.getValue(), lt + opener.length());
        if (e >= 0) {
          i = e + entry.getValue().length();
          break;
        }
        blocks.remove(opener);
      }
      if (i <= lt) return out + str.substring(lt);
    }
    return out.toString();
  }
}
