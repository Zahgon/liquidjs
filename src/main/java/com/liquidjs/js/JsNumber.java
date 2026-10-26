/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 Jun Yang
 */
package com.liquidjs.js;

/**
 * Number formatting and parsing as the ECMAScript language defines them, so a rendered number
 * reads exactly as the template engine's own output does.
 */
public final class JsNumber {
  private JsNumber () {}

  /** ECMAScript Number::toString with radix 10. */
  public static String toString (double value) {
    if (Double.isNaN(value)) return "NaN";
    if (value == 0) return "0";
    if (value < 0) return "-" + toString(-value);
    if (Double.isInfinite(value)) return "Infinity";

    // the shortest decimal that recovers the double, split into its digits and its exponent
    String shortest = Double.toString(value);
    int exponent = 0;
    int e = shortest.indexOf('E');
    String mantissa = shortest;
    if (e >= 0) {
      exponent = Integer.parseInt(shortest.substring(e + 1));
      mantissa = shortest.substring(0, e);
    }
    int dot = mantissa.indexOf('.');
    String digits = mantissa.substring(0, dot) + mantissa.substring(dot + 1);
    int pointExponent = dot + exponent;

    int lead = 0;
    while (lead < digits.length() - 1 && digits.charAt(lead) == '0') {
      lead++;
      pointExponent--;
    }
    digits = digits.substring(lead);
    int tail = digits.length();
    while (tail > 1 && digits.charAt(tail - 1) == '0') tail--;
    digits = digits.substring(0, tail);
    if ("0".equals(digits)) return "0";

    int k = digits.length();
    int n = pointExponent;
    // the host keeps at least two significant digits; the language keeps the fewest that recover
    // the same number, so a shorter form is used when it rounds back to this value
    for (int shorter = 1; shorter < k; shorter++) {
      java.math.BigDecimal rounded = new java.math.BigDecimal(new java.math.BigInteger(digits))
          .scaleByPowerOfTen(n - k)
          .round(new java.math.MathContext(shorter))
          .stripTrailingZeros();
      if (rounded.doubleValue() == value) {
        String shorterDigits = rounded.unscaledValue().toString();
        digits = shorterDigits;
        k = digits.length();
        n = k - rounded.scale();
        break;
      }
    }
    if (k <= n && n <= 21) return digits + "0".repeat(n - k);
    if (0 < n && n <= 21) return digits.substring(0, n) + "." + digits.substring(n);
    if (-6 < n && n <= 0) return "0." + "0".repeat(-n) + digits;
    String exp = (n - 1 >= 0 ? "e+" : "e-") + Math.abs(n - 1);
    if (k == 1) return digits + exp;
    return digits.charAt(0) + "." + digits.substring(1) + exp;
  }

  /** ECMAScript ToNumber for a string, including the hexadecimal, octal and binary literal forms. */
  public static double parse (String input) {
    String str = JsString.trim(input);
    if (str.isEmpty()) return 0;
    try {
      if (str.length() > 2 && (str.charAt(0) == '0')) {
        char radixChar = Character.toLowerCase(str.charAt(1));
        String rest = str.substring(2);
        if (radixChar == 'x') return parseRadix(rest, 16);
        if (radixChar == 'o') return parseRadix(rest, 8);
        if (radixChar == 'b') return parseRadix(rest, 2);
      }
      if (str.equals("Infinity") || str.equals("+Infinity")) return Double.POSITIVE_INFINITY;
      if (str.equals("-Infinity")) return Double.NEGATIVE_INFINITY;
      // Double.parseDouble accepts forms the language does not: hexadecimal floats,
      // a trailing type suffix and leading or trailing whitespace of its own choosing
      if (!str.matches("[+-]?(\\d+\\.?\\d*|\\.\\d+)([eE][+-]?\\d+)?")) return Double.NaN;
      return Double.parseDouble(str);
    } catch (NumberFormatException err) {
      return Double.NaN;
    }
  }

  private static double parseRadix (String digits, int radix) {
    if (digits.isEmpty()) return Double.NaN;
    double value = 0;
    for (int i = 0; i < digits.length(); i++) {
      int digit = Character.digit(digits.charAt(i), radix);
      if (digit < 0) return Double.NaN;
      value = value * radix + digit;
    }
    return value;
  }

  /** ECMAScript Math.round: half away from zero towards positive infinity. */
  public static double round (double value) {
    if (Double.isNaN(value) || Double.isInfinite(value)) return value;
    double floor = Math.floor(value);
    return value - floor >= 0.5 ? floor + 1 : floor;
  }

  /** The remainder operator, which keeps the sign of the left operand. */
  public static double remainder (double lhs, double rhs) {
    return lhs % rhs;
  }
}
