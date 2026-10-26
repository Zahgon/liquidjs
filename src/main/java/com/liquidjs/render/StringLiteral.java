/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 Jun Yang
 */
package com.liquidjs.render;

/** Reads the escape sequences of a quoted string. */
public final class StringLiteral {
  private StringLiteral () {}

  private static boolean isHex (char ch) {
    return (ch >= '0' && ch <= '9') || (ch >= 'a' && ch <= 'f') || (ch >= 'A' && ch <= 'F');
  }

  private static boolean isOctal (char ch) {
    return ch >= '0' && ch <= '7';
  }

  private static char escapeChar (char ch) {
    switch (ch) {
      case 'b': return '\b';
      case 'f': return '\f';
      case 'n': return '\n';
      case 'r': return '\r';
      case 't': return '\t';
      case 'v': return '\u000b';
      default: return 0;
    }
  }

  private static int hexVal (char ch) {
    int code = ch;
    if (code >= 97) return code - 87;
    if (code >= 65) return code - 55;
    return code - 48;
  }

  public static String parseStringLiteral (String str) {
    StringBuilder ret = new StringBuilder();
    for (int i = 1; i < str.length() - 1; i++) {
      if (str.charAt(i) != '\\') {
        ret.append(str.charAt(i));
        continue;
      }
      char next = i + 1 < str.length() ? str.charAt(i + 1) : 0;
      char escaped = escapeChar(next);
      if (escaped != 0) {
        ret.append(escaped);
        i++;
      } else if (next == 'u') {
        int val = 0;
        int j = i + 2;
        while (j <= i + 5 && j < str.length() && isHex(str.charAt(j))) {
          val = val * 16 + hexVal(str.charAt(j++));
        }
        i = j - 1;
        ret.append((char) val);
      } else if (!isOctal(next)) {
        ret.append(next);
        i++;
      } else {
        int j = i + 1;
        int val = 0;
        while (j <= i + 3 && j < str.length() && isOctal(str.charAt(j))) {
          val = val * 8 + hexVal(str.charAt(j++));
        }
        i = j - 1;
        ret.append((char) val);
      }
    }
    return ret.toString();
  }
}
