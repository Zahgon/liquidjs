/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 Jun Yang
 */
package com.liquidjs.util;

/**
 * Character classes as a bit mask, one entry per code unit. Beyond the ASCII range a code unit is
 * a word character unless it is listed here as blank or as punctuation.
 */
public final class Characters {
  private Characters () {}

  public static final int WORD = 1;
  public static final int OPERATOR = 2;
  public static final int BLANK = 4;
  public static final int QUOTE = 8;
  public static final int INLINE_BLANK = 16;
  public static final int NUMBER = 32;
  public static final int SIGN = 64;
  public static final int PUNCTUATION = 128;

  private static final int[] ASCII = {
    0, 0, 0, 0, 0, 0, 0, 0, 0, 20, 4, 4, 4, 20, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
    0, 20, 2, 8, 0, 0, 0, 0, 8, 0, 0, 0, 64, 0, 65, 0, 0, 33, 33, 33, 33, 33, 33, 33, 33, 33, 33,
    0, 0, 2, 2, 2, 1, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
    1, 0, 0, 0, 0, 1, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
    1, 0, 0, 0, 0, 0
  };

  /** One entry per code unit, so a lookup never branches on the range. */
  public static final int[] TYPES = new int[65536];

  static {
    System.arraycopy(ASCII, 0, TYPES, 0, ASCII.length);
    int[] blanks = {
      160, 5760, 6158, 8192, 8193, 8194, 8195, 8196, 8197, 8198, 8199, 8200, 8201, 8202, 8232,
      8233, 8239, 8287, 12288
    };
    for (int code : blanks) TYPES[code] = BLANK;
    TYPES[8220] = PUNCTUATION;
    TYPES[8221] = PUNCTUATION;
  }

  public static int typeOf (char ch) {
    return TYPES[ch];
  }

  public static boolean isWord (String ch) {
    if (ch == null || ch.isEmpty()) return false;
    return isWord(ch.charAt(0));
  }

  public static boolean isWord (char ch) {
    int code = ch;
    return code >= 128 ? TYPES[code] == 0 : (TYPES[code] & WORD) != 0;
  }
}
