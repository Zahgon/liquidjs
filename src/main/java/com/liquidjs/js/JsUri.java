/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 Jun Yang
 */
package com.liquidjs.js;

import java.nio.charset.StandardCharsets;

/**
 * URI escaping as encodeURIComponent, encodeURI and decodeURIComponent define it: the unreserved
 * set keeps {@code ! ' ( ) * - . _ ~} and encodeURI additionally keeps the reserved delimiters.
 */
public final class JsUri {
  private JsUri () {}

  private static final String COMPONENT_UNESCAPED = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
      + "abcdefghijklmnopqrstuvwxyz0123456789-_.!~*'()";
  private static final String URI_UNESCAPED = COMPONENT_UNESCAPED + ";/?:@&=+$,#";

  /** Percent encode everything outside the unreserved set. */
  public static String encodeURIComponent (String str) {
    return encode(str, COMPONENT_UNESCAPED);
  }

  /** Percent encode everything outside the unreserved and reserved sets. */
  public static String encodeURI (String str) {
    return encode(str, URI_UNESCAPED);
  }

  private static String encode (String str, String unescaped) {
    StringBuilder out = new StringBuilder();
    int i = 0;
    while (i < str.length()) {
      char ch = str.charAt(i);
      if (unescaped.indexOf(ch) >= 0) {
        out.append(ch);
        i++;
        continue;
      }
      int codePoint = ch;
      int width = 1;
      if (Character.isHighSurrogate(ch) && i + 1 < str.length()
          && Character.isLowSurrogate(str.charAt(i + 1))) {
        codePoint = Character.toCodePoint(ch, str.charAt(i + 1));
        width = 2;
      } else if (Character.isSurrogate(ch)) {
        throw new IllegalArgumentException("URI malformed");
      }
      byte[] bytes = new String(Character.toChars(codePoint)).getBytes(StandardCharsets.UTF_8);
      for (byte b : bytes) out.append('%').append(String.format("%02X", b & 0xff));
      i += width;
    }
    return out.toString();
  }

  /** Undo percent encoding, raising on a malformed sequence. */
  public static String decodeURIComponent (String str) {
    java.io.ByteArrayOutputStream bytes = new java.io.ByteArrayOutputStream();
    StringBuilder out = new StringBuilder();
    int i = 0;
    while (i < str.length()) {
      char ch = str.charAt(i);
      if (ch == '%') {
        if (i + 2 >= str.length() + 1 || i + 3 > str.length()) throw new IllegalArgumentException("URI malformed");
        int value;
        try {
          value = Integer.parseInt(str.substring(i + 1, i + 3), 16);
        } catch (NumberFormatException err) {
          throw new IllegalArgumentException("URI malformed");
        }
        bytes.write(value);
        i += 3;
        continue;
      }
      if (bytes.size() > 0) {
        out.append(new String(bytes.toByteArray(), StandardCharsets.UTF_8));
        bytes.reset();
      }
      out.append(ch);
      i++;
    }
    if (bytes.size() > 0) out.append(new String(bytes.toByteArray(), StandardCharsets.UTF_8));
    return out.toString();
  }
}
