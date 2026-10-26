/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 Jun Yang
 *
 * String related filters
 *
 * Every one of them renders its value first, so an absent value reads as an empty string rather
 * than as the name of the absent value.
 */
package com.liquidjs.filters;

import com.liquidjs.js.Js;
import com.liquidjs.js.JsString;
import com.liquidjs.template.FilterHandler;
import com.liquidjs.util.Assertions;
import com.liquidjs.util.Underscore;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** The filters over strings. */
public final class StringFilters {
  private StringFilters () {}

  // Han (Chinese) characters: 一-鿿
  // Additional Han characters: 豈-﫿 (CJK Compatibility Ideographs)
  // Additional Han characters: 㐀-䶿 (CJK Unified Ideographs Extension A)
  // Katakana (Japanese): ゠-ヿ
  // Hiragana (Japanese): ぀-ゟ
  // Hangul (Korean): 가-힯
  private static final String CJK = "\\u4E00-\\u9FFF\\uF900-\\uFAFF\\u3400-\\u4DBF"
      + "\\u3040-\\u309F\\u30A0-\\u30FF\\uAC00-\\uD7AF";
  private static final Pattern CJK_WORD = Pattern.compile("[" + CJK + "]");
  /** A run of word characters that are not CJK, which is one word. */
  private static final Pattern NON_CJK_WORD =
      Pattern.compile("[^" + CJK + JsString.WHITESPACE_CLASS + "]+");

  public static void register (Map<String, Object> filters) {
    filters.put("append", (FilterHandler) (impl, v, args) -> {
      Assertions.assertTrue(args.size() == 1, "append expect 2 arguments");
      String lhs = Underscore.stringify(v);
      String rhs = Args.string(args, 0);
      impl.context.memoryLimit.use(lhs.length() + rhs.length());
      return lhs + rhs;
    });
    filters.put("prepend", (FilterHandler) (impl, v, args) -> {
      Assertions.assertTrue(args.size() == 1, "prepend expect 2 arguments");
      String lhs = Underscore.stringify(v);
      String rhs = Args.string(args, 0);
      impl.context.memoryLimit.use(lhs.length() + rhs.length());
      return rhs + lhs;
    });
    filters.put("lstrip", (FilterHandler) (impl, v, args) -> {
      String str = Underscore.stringify(v);
      impl.context.memoryLimit.use(str.length());
      Object chars = Args.at(args, 0);
      if (Js.truthy(chars)) {
        String charsStr = Underscore.stringify(chars);
        impl.context.memoryLimit.use(charsStr.length());
        return str.substring(getTrimStart(str, charSet(charsStr)));
      }
      return JsString.trimStart(str);
    });
    filters.put("downcase", (FilterHandler) (impl, v, args) -> {
      String str = Underscore.stringify(v);
      impl.context.memoryLimit.use(str.length());
      return JsString.toLowerCase(str);
    });
    filters.put("upcase", (FilterHandler) (impl, v, args) -> {
      String str = Underscore.stringify(v);
      impl.context.memoryLimit.use(str.length());
      return JsString.toUpperCase(str);
    });
    filters.put("remove", (FilterHandler) (impl, v, args) -> {
      String str = Underscore.stringify(v);
      String arg = Args.string(args, 0);
      impl.context.memoryLimit.use(str.length() + arg.length());
      return String.join("", JsString.split(str, arg));
    });
    filters.put("remove_first", (FilterHandler) (impl, v, args) -> {
      String str = Underscore.stringify(v);
      String arg = Args.string(args, 0);
      impl.context.memoryLimit.use(str.length() + arg.length());
      return JsString.replaceFirst(str, arg, "");
    });
    filters.put("remove_last", (FilterHandler) (impl, v, args) -> {
      String str = Underscore.stringify(v);
      String pattern = Args.string(args, 0);
      impl.context.memoryLimit.use(str.length() + pattern.length());
      int index = str.lastIndexOf(pattern);
      if (index == -1) return str;
      return str.substring(0, index) + str.substring(index + pattern.length());
    });
    filters.put("rstrip", (FilterHandler) (impl, v, args) -> {
      String str = Underscore.stringify(v);
      impl.context.memoryLimit.use(str.length());
      Object chars = Args.at(args, 0);
      if (Js.truthy(chars)) {
        String charsStr = Underscore.stringify(chars);
        impl.context.memoryLimit.use(charsStr.length());
        return str.substring(0, getTrimEnd(str, charSet(charsStr), 0));
      }
      return JsString.trimEnd(str);
    });
    filters.put("split", (FilterHandler) (impl, v, args) -> {
      String str = Underscore.stringify(v);
      impl.context.memoryLimit.use(str.length());
      List<String> parts = JsString.split(str, Args.string(args, 0));
      // as Ruby's split behaves, which is what the reference implementation does
      while (!parts.isEmpty() && parts.get(parts.size() - 1).isEmpty()) {
        parts.remove(parts.size() - 1);
      }
      return new ArrayList<Object>(parts);
    });
    filters.put("strip", (FilterHandler) (impl, v, args) -> {
      String str = Underscore.stringify(v);
      impl.context.memoryLimit.use(str.length());
      Object chars = Args.at(args, 0);
      if (Js.truthy(chars)) {
        Set<String> set = charSet(Underscore.stringify(chars));
        impl.context.memoryLimit.use(set.size());
        int start = getTrimStart(str, set);
        return str.substring(start, getTrimEnd(str, set, start));
      }
      return JsString.trim(str);
    });
    filters.put("strip_newlines", (FilterHandler) (impl, v, args) -> {
      String str = Underscore.stringify(v);
      impl.context.memoryLimit.use(str.length());
      return str.replaceAll("\r?\n", "");
    });
    filters.put("squish", (FilterHandler) (impl, v, args) -> {
      String str = Underscore.stringify(v);
      impl.context.memoryLimit.use(str.length());
      return JsString.trim(JsString.normalizeWhitespace(str, " "));
    });
    filters.put("capitalize", (FilterHandler) (impl, v, args) -> {
      String str = Underscore.stringify(v);
      impl.context.memoryLimit.use(str.length());
      if (str.isEmpty()) return "";
      return JsString.toUpperCase(str.substring(0, 1)) + JsString.toLowerCase(str.substring(1));
    });
    filters.put("replace", (FilterHandler) (impl, v, args) -> {
      String str = Underscore.stringify(v);
      String pattern = Args.string(args, 0);
      String replacement = Args.string(args, 1);
      List<String> parts = JsString.split(str, pattern);
      int outputSize = str.length()
          + (parts.size() - 1) * (replacement.length() - pattern.length());
      impl.context.memoryLimit.use(outputSize);
      return String.join(replacement, parts);
    });
    filters.put("replace_first", (FilterHandler) (impl, v, args) -> {
      String str = Underscore.stringify(v);
      String arg1 = Args.string(args, 0);
      String arg2 = Args.string(args, 1);
      impl.context.memoryLimit.use(str.length() + arg1.length() + arg2.length());
      return JsString.replaceFirst(str, arg1, arg2);
    });
    filters.put("replace_last", (FilterHandler) (impl, v, args) -> {
      String str = Underscore.stringify(v);
      String pattern = Args.string(args, 0);
      String replacement = Args.string(args, 1);
      impl.context.memoryLimit.use(str.length() + pattern.length() + replacement.length());
      int index = str.lastIndexOf(pattern);
      if (index == -1) return str;
      return str.substring(0, index) + replacement + str.substring(index + pattern.length());
    });
    filters.put("truncate", (FilterHandler) (impl, v, args) -> {
      String str = Underscore.stringify(v);
      double l = Args.number(args, 0, 50);
      String o = Args.string(args, 1, "...");
      impl.context.memoryLimit.use(str.length() + o.length());
      // a value that is short enough is returned as it was given, not as a string
      if (str.length() <= l) return v;
      return str.substring(0, Math.max((int) l - o.length(), 0)) + o;
    });
    filters.put("truncatewords", (FilterHandler) (impl, v, args) -> {
      String str = Underscore.stringify(v);
      double words = Args.number(args, 0, 15);
      String o = Args.string(args, 1, "...");
      impl.context.memoryLimit.use(str.length() + o.length());
      List<String> arr = JsString.splitWhitespace(JsString.trimStart(str));
      if (words <= 0) words = 1;
      int count = (int) Math.min(words, arr.size());
      String ret = String.join(" ", arr.subList(0, count));
      if (arr.size() >= words) ret += o;
      return ret;
    });
    filters.put("normalize_whitespace", (FilterHandler) (impl, v, args) -> {
      String str = Underscore.stringify(v);
      impl.context.memoryLimit.use(str.length());
      return JsString.normalizeWhitespace(str, " ");
    });
    filters.put("number_of_words", (FilterHandler) (impl, v, args) -> {
      String str = Underscore.stringify(v);
      impl.context.memoryLimit.use(str.length());
      String input = JsString.trim(str);
      if (input.isEmpty()) return 0.0;
      String mode = Js.isUndefined(Args.at(args, 0)) ? null : Args.string(args, 0);
      if ("cjk".equals(mode)) {
        return (double) (count(CJK_WORD, input) + count(NON_CJK_WORD, input));
      }
      if ("auto".equals(mode)) {
        return CJK_WORD.matcher(input).find()
            ? (double) (count(CJK_WORD, input) + count(NON_CJK_WORD, input))
            : (double) JsString.splitWhitespace(input).size();
      }
      return (double) JsString.splitWhitespace(input).size();
    });
    filters.put("array_to_sentence_string", (FilterHandler) (impl, v, args) -> {
      Object value = Js.toValue(v);
      String connector = Args.string(args, 0, "and");
      if (Js.isNil(value)) {
        throw new RuntimeException("Cannot read properties of "
            + (value == null ? "null" : "undefined") + " (reading 'length')");
      }
      // the filter reads the value as an array; a string has a length and elements of its own,
      // and anything else has neither
      List<Object> array = new ArrayList<>();
      boolean isString = Js.isString(value);
      if (Js.isArray(value)) {
        array.addAll(Js.asArray(value));
      } else if (isString) {
        String str = (String) value;
        for (int i = 0; i < str.length(); i++) array.add(String.valueOf(str.charAt(i)));
      } else {
        throw new RuntimeException("array.slice is not a function");
      }
      int outputSize = connector.length() + array.size() * 2;
      for (Object item : array) outputSize += Underscore.stringify(item).length();
      impl.context.memoryLimit.use(outputSize);
      switch (array.size()) {
        case 0:
          return "";
        case 1:
          return array.get(0);
        case 2:
          return Js.toStringValue(array.get(0)) + " " + connector + " "
              + Js.toStringValue(array.get(1));
        default: {
          if (isString) throw new RuntimeException("array.slice(...).join is not a function");
          List<String> head = new ArrayList<>();
          for (int i = 0; i < array.size() - 1; i++) head.add(Js.toStringValue(array.get(i)));
          return String.join(", ", head) + ", " + connector + " "
              + Js.toStringValue(array.get(array.size() - 1));
        }
      }
    });
  }

  private static int count (Pattern pattern, String input) {
    Matcher matcher = pattern.matcher(input);
    int found = 0;
    while (matcher.find()) found++;
    return found;
  }

  /** The characters of a string, as a set, so a trim can test one character at a time. */
  private static Set<String> charSet (String chars) {
    Set<String> set = new LinkedHashSet<>();
    int i = 0;
    while (i < chars.length()) {
      int codePoint = chars.codePointAt(i);
      String ch = new String(Character.toChars(codePoint));
      set.add(ch);
      i += ch.length();
    }
    return set;
  }

  private static int getTrimStart (String str, Set<String> chars) {
    int start = 0;
    while (start < str.length()) {
      int codePoint = str.codePointAt(start);
      String ch = new String(Character.toChars(codePoint));
      if (!chars.contains(ch)) break;
      start += ch.length();
    }
    return start;
  }

  private static int getTrimEnd (String str, Set<String> chars, int start) {
    int end = str.length();
    while (end > start) {
      int size = end - start >= 2 && JsString.codePointAt(str, end - 2) > 0xFFFF ? 2 : 1;
      if (!chars.contains(str.substring(end - size, end))) break;
      end -= size;
    }
    return end;
  }
}
