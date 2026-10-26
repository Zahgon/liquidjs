/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 Jun Yang
 *
 * Base64 related filters
 *
 * Implements base64_encode and base64_decode filters for Shopify compatibility
 */
package com.liquidjs.filters;

import com.liquidjs.template.FilterHandler;
import com.liquidjs.util.Underscore;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

/** The filters that encode and decode base 64. */
public final class Base64Filters {
  private Base64Filters () {}

  /** Register these filters under the names a template calls them by. */
  public static void register (Map<String, Object> filters) {
    filters.put("base64_encode", (FilterHandler) (impl, value, args) -> {
      String str = Underscore.stringify(value);
      impl.context.memoryLimit.use(str.length());
      return Base64.getEncoder().encodeToString(str.getBytes(StandardCharsets.UTF_8));
    });
    filters.put("base64_decode", (FilterHandler) (impl, value, args) -> {
      String str = Underscore.stringify(value);
      impl.context.memoryLimit.use(str.length());
      return new String(decode(str), StandardCharsets.UTF_8);
    });
  }

  /**
   * Decode leniently, as a byte buffer built from a base 64 string does: a character outside the
   * alphabet is dropped, and the padding may be missing or short.
   */
  static byte[] decode (String input) {
    StringBuilder cleaned = new StringBuilder();
    for (int i = 0; i < input.length(); i++) {
      char ch = input.charAt(i);
      boolean valid = (ch >= 'A' && ch <= 'Z') || (ch >= 'a' && ch <= 'z')
          || (ch >= '0' && ch <= '9') || ch == '+' || ch == '/' || ch == '-' || ch == '_';
      if (valid) cleaned.append(ch == '-' ? '+' : ch == '_' ? '/' : ch);
    }
    int over = cleaned.length() % 4;
    if (over == 1) cleaned.setLength(cleaned.length() - 1);
    else if (over == 2) cleaned.append("==");
    else if (over == 3) cleaned.append('=');
    return Base64.getDecoder().decode(cleaned.toString());
  }
}
