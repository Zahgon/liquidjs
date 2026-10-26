/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 Jun Yang
 */
package com.liquidjs.filters;

import com.liquidjs.js.JsString;
import com.liquidjs.js.JsUri;
import com.liquidjs.template.FilterHandler;
import com.liquidjs.util.Underscore;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

/** The filters that escape or unescape a URL, and the one that makes a slug. */
public final class UrlFilters {
  private UrlFilters () {}

  private static final Map<String, Pattern> SLUGIFY_REPLACERS = slugifyReplacers();

  private static Map<String, Pattern> slugifyReplacers () {
    Pattern bydefault = Pattern.compile("[^\\p{M}\\p{L}\\p{Nd}]+");
    Map<String, Pattern> map = new LinkedHashMap<>();
    map.put("raw", Pattern.compile(JsString.WHITESPACE_RUN));
    map.put("default", bydefault);
    map.put("pretty", Pattern.compile("[^\\p{M}\\p{L}\\p{Nd}._~!$&'()+,;=@]+"));
    map.put("ascii", Pattern.compile("[^A-Za-z0-9]+"));
    map.put("latin", bydefault);
    map.put("none", null);
    return map;
  }

  /** Register these filters under the names a template calls them by. */
  public static void register (Map<String, Object> filters) {
    filters.put("url_decode", (FilterHandler) (impl, v, args) ->
        JsUri.decodeURIComponent(Underscore.stringify(v).replace("+", " ")));
    filters.put("url_encode", (FilterHandler) (impl, v, args) ->
        JsUri.encodeURIComponent(Underscore.stringify(v)).replace("%20", "+"));
    filters.put("cgi_escape", (FilterHandler) (impl, v, args) -> {
      String encoded = JsUri.encodeURIComponent(Underscore.stringify(v)).replace("%20", "+");
      StringBuilder out = new StringBuilder();
      for (int i = 0; i < encoded.length(); i++) {
        char ch = encoded.charAt(i);
        if ("!'()*".indexOf(ch) >= 0) {
          out.append('%').append(Integer.toHexString(ch).toUpperCase(java.util.Locale.ROOT));
        } else {
          out.append(ch);
        }
      }
      return out.toString();
    });
    filters.put("uri_escape", (FilterHandler) (impl, v, args) ->
        JsUri.encodeURI(Underscore.stringify(v)).replace("%5B", "[").replace("%5D", "]"));
    filters.put("slugify", (FilterHandler) (impl, v, args) -> {
      String mode = Args.string(args, 0, "default");
      boolean cased = com.liquidjs.js.Js.truthy(Args.at(args, 1));
      return slugify(Underscore.stringify(v), mode, cased);
    });
  }

  static String slugify (String input, String mode, boolean cased) {
    String str = input;
    if (!SLUGIFY_REPLACERS.containsKey(mode)) return cased ? str : JsString.toLowerCase(str);
    Pattern replacer = SLUGIFY_REPLACERS.get(mode);
    if (replacer != null) {
      if ("latin".equals(mode)) str = removeAccents(str);
      str = replacer.matcher(str).replaceAll("-");
      str = str.replaceAll("^-|-$", "");
    }
    return cased ? str : JsString.toLowerCase(str);
  }

  private static String removeAccents (String input) {
    return input
        .replaceAll("[àáâãäå]", "a")
        .replaceAll("[æ]", "ae")
        .replaceAll("[ç]", "c")
        .replaceAll("[èéêë]", "e")
        .replaceAll("[ìíîï]", "i")
        .replaceAll("[ð]", "d")
        .replaceAll("[ñ]", "n")
        .replaceAll("[òóôõöø]", "o")
        .replaceAll("[ùúûü]", "u")
        .replaceAll("[ýÿ]", "y")
        .replaceAll("[ß]", "ss")
        .replaceAll("[œ]", "oe")
        .replaceAll("[þ]", "th")
        .replaceAll("[ẞ]", "SS")
        .replaceAll("[Œ]", "OE")
        .replaceAll("[Þ]", "TH");
  }
}
