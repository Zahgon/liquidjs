/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 Jun Yang
 *
 * Crypto related filters
 *
 * Implements sha256 and hmac_sha256 filters for Shopify compatibility
 */
package com.liquidjs.filters;

import com.liquidjs.template.FilterHandler;
import com.liquidjs.util.Underscore;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

/** The filters that hash a value. */
public final class CryptoFilters {
  private CryptoFilters () {}

  public static void register (Map<String, Object> filters) {
    filters.put("sha256", (FilterHandler) (impl, value, args) -> {
      String str = Underscore.stringify(value);
      impl.context.memoryLimit.use(str.length());
      return sha256(str);
    });
    filters.put("hmac_sha256", (FilterHandler) (impl, value, args) -> {
      String str = Underscore.stringify(value);
      String key = Args.string(args, 0);
      impl.context.memoryLimit.use(str.length() + key.length());
      return hmacSha256(str, key);
    });
  }

  static String sha256 (String str) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      return hex(digest.digest(str.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException err) {
      throw new IllegalStateException(err);
    }
  }

  static String hmacSha256 (String str, String key) {
    try {
      Mac mac = Mac.getInstance("HmacSHA256");
      byte[] keyBytes = key.getBytes(StandardCharsets.UTF_8);
      mac.init(new SecretKeySpec(keyBytes.length == 0 ? new byte[1] : keyBytes, "HmacSHA256"));
      return hex(mac.doFinal(str.getBytes(StandardCharsets.UTF_8)));
    } catch (java.security.GeneralSecurityException err) {
      throw new IllegalStateException(err);
    }
  }

  private static String hex (byte[] bytes) {
    StringBuilder out = new StringBuilder();
    for (byte b : bytes) out.append(String.format("%02x", b & 0xff));
    return out.toString();
  }
}
