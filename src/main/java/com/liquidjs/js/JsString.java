/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 Jun Yang
 */
package com.liquidjs.js;

import java.util.ArrayList;
import java.util.List;

/**
 * String operations with the character classes and the case mapping of the source runtime:
 * its whitespace class is wider than the one a regular expression engine uses by default.
 */
public final class JsString {
  private JsString () {}

  /** WhiteSpace and LineTerminator of the source language, as a character class body. */
  public static final String WHITESPACE_CLASS =
      " \\t\\n\\u000b\\f\\r\\u00a0\\u1680\\u2000-\\u200a\\u2028\\u2029\\u202f\\u205f\\u3000\\ufeff";
  /** A whitespace run, equivalent to the source language's \s+ */
  public static final String WHITESPACE_RUN = "[" + WHITESPACE_CLASS + "]+";

  /** Whether a code unit is whitespace or a line terminator of the source language. */
  public static boolean isWhitespace (char ch) {
    switch (ch) {
      case '\t': case '\n': case '\u000b': case '\f': case '\r': case ' ':
      case '\u00a0': case '\u1680': case '\u2028': case '\u2029':
      case '\u202f': case '\u205f': case '\u3000': case '\ufeff':
        return true;
      default:
        return ch >= '\u2000' && ch <= '\u200a';
    }
  }

  /** The string without its leading and trailing whitespace. */
  public static String trim (String str) {
    return trimEnd(trimStart(str));
  }

  /** The string without its leading whitespace. */
  public static String trimStart (String str) {
    int start = 0;
    while (start < str.length() && isWhitespace(str.charAt(start))) start++;
    return str.substring(start);
  }

  /** The string without its trailing whitespace. */
  public static String trimEnd (String str) {
    int end = str.length();
    while (end > 0 && isWhitespace(str.charAt(end - 1))) end--;
    return str.substring(0, end);
  }

  /** Full Unicode lower case mapping, without any language-sensitive tailoring. */
  public static String toLowerCase (String str) {
    return str.toLowerCase(java.util.Locale.ROOT);
  }

  /** Full Unicode upper case mapping, without any language-sensitive tailoring. */
  public static String toUpperCase (String str) {
    return str.toUpperCase(java.util.Locale.ROOT);
  }

  /** Split on a literal separator, keeping every empty field, as String.prototype.split does. */
  public static List<String> split (String str, String separator) {
    List<String> parts = new ArrayList<>();
    if (separator.isEmpty()) {
      for (int i = 0; i < str.length(); i++) parts.add(String.valueOf(str.charAt(i)));
      return parts;
    }
    int from = 0;
    while (true) {
      int at = str.indexOf(separator, from);
      if (at < 0) {
        parts.add(str.substring(from));
        return parts;
      }
      parts.add(str.substring(from, at));
      from = at + separator.length();
    }
  }

  /** Split on a whitespace run, as the source language's split(/\s+/) does. */
  public static List<String> splitWhitespace (String str) {
    List<String> parts = new ArrayList<>();
    int i = 0;
    int from = 0;
    while (i < str.length()) {
      if (isWhitespace(str.charAt(i))) {
        parts.add(str.substring(from, i));
        while (i < str.length() && isWhitespace(str.charAt(i))) i++;
        from = i;
        continue;
      }
      i++;
    }
    parts.add(str.substring(from));
    return parts;
  }

  /** Collapse every whitespace run into one space. */
  public static String normalizeWhitespace (String str, String replacement) {
    StringBuilder out = new StringBuilder();
    int i = 0;
    while (i < str.length()) {
      if (isWhitespace(str.charAt(i))) {
        out.append(replacement);
        while (i < str.length() && isWhitespace(str.charAt(i))) i++;
        continue;
      }
      out.append(str.charAt(i++));
    }
    return out.toString();
  }

  /** Replace the first occurrence of a literal pattern. */
  public static String replaceFirst (String str, String pattern, String replacement) {
    int at = str.indexOf(pattern);
    if (at < 0) return str;
    return str.substring(0, at) + replacement + str.substring(at + pattern.length());
  }

  /** String.prototype.slice, which accepts negative and out-of-range bounds. */
  public static String slice (String str, int begin, int end) {
    int length = str.length();
    int from = begin < 0 ? Math.max(length + begin, 0) : Math.min(begin, length);
    int to = end < 0 ? Math.max(length + end, 0) : Math.min(end, length);
    if (to <= from) return "";
    return str.substring(from, to);
  }

  /** The string padded on the left to a length. */
  public static String padStart (String str, int length, String pad) {
    int n = length - str.length();
    if (n <= 0 || pad.isEmpty()) return str;
    return repeatTo(pad, n) + str;
  }

  /** The string padded on the right to a length. */
  public static String padEnd (String str, int length, String pad) {
    int n = length - str.length();
    if (n <= 0 || pad.isEmpty()) return str;
    return str + repeatTo(pad, n);
  }

  private static String repeatTo (String pad, int n) {
    StringBuilder out = new StringBuilder();
    while (out.length() < n) out.append(pad);
    return out.substring(0, n);
  }

  /**
   * The UTF-8 bytes of a string, with an unpaired surrogate written as the replacement character,
   * as the source runtime's byte buffers write it.
   */
  public static byte[] utf8Bytes (String str) {
    StringBuilder out = new StringBuilder();
    for (int i = 0; i < str.length(); i++) {
      char ch = str.charAt(i);
      if (Character.isHighSurrogate(ch) && i + 1 < str.length()
          && Character.isLowSurrogate(str.charAt(i + 1))) {
        out.append(ch).append(str.charAt(++i));
      } else if (Character.isSurrogate(ch)) {
        out.append('\ufffd');
      } else {
        out.append(ch);
      }
    }
    return out.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
  }

  /** The code point at an index, or -1 past the end. */
  public static int codePointAt (String str, int index) {
    if (index < 0 || index >= str.length()) return -1;
    return str.codePointAt(index);
  }
}
