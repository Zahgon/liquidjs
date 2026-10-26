/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 Jun Yang
 */
package com.liquidjs.parser;

import com.liquidjs.LiquidOptions;
import com.liquidjs.tokens.DelimitedToken;
import com.liquidjs.tokens.HTMLToken;
import com.liquidjs.tokens.TagToken;
import com.liquidjs.tokens.Token;
import com.liquidjs.util.Characters;

import java.util.List;

/** Trims the blanks around a tag or an output that asked for it. */
public final class WhitespaceCtrl {
  private WhitespaceCtrl () {}

  /** Trim the blanks around every tag and output that asked for it. */
  public static void whiteSpaceCtrl (List<Token> tokens, LiquidOptions options) {
    boolean inRaw = false;

    for (int i = 0; i < tokens.size(); i++) {
      Token token = tokens.get(i);
      if (!(token instanceof DelimitedToken)) continue;
      DelimitedToken delimited = (DelimitedToken) token;
      if (!inRaw && delimited.trimLeft) {
        trimLeft(i - 1 >= 0 ? tokens.get(i - 1) : null, options.greedy);
      }

      if (token instanceof TagToken) {
        String name = ((TagToken) token).name;
        if ("raw".equals(name)) inRaw = true;
        else if ("endraw".equals(name)) inRaw = false;
      }

      if (!inRaw && delimited.trimRight) {
        trimRight(i + 1 < tokens.size() ? tokens.get(i + 1) : null, options.greedy);
      }
    }
  }

  private static void trimLeft (Token token, boolean greedy) {
    if (!(token instanceof HTMLToken)) return;
    HTMLToken html = (HTMLToken) token;
    int mask = greedy ? Characters.BLANK : Characters.INLINE_BLANK;
    while (charTypeAt(html.input, html.end - 1 - html.trimRight, mask)) html.trimRight++;
  }

  private static void trimRight (Token token, boolean greedy) {
    if (!(token instanceof HTMLToken)) return;
    HTMLToken html = (HTMLToken) token;
    int mask = greedy ? Characters.BLANK : Characters.INLINE_BLANK;
    while (charTypeAt(html.input, html.begin + html.trimLeft, mask)) html.trimLeft++;
    int at = html.begin + html.trimLeft;
    if (at >= 0 && at < html.input.length() && html.input.charAt(at) == '\n') html.trimLeft++;
  }

  private static boolean charTypeAt (String input, int at, int mask) {
    if (at < 0 || at >= input.length()) return false;
    return (Characters.TYPES[input.charAt(at)] & mask) != 0;
  }
}
