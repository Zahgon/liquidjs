/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 Jun Yang
 */
package com.liquidjs.tokens;

import com.liquidjs.parser.TokenKind;

import java.util.HashMap;
import java.util.Map;

/** An operator in a conditional expression. */
public class OperatorToken extends Token {
  /** Binary operators take a left and a right operand, a unary one takes only the right. */
  public static final int BINARY = 0;
  public static final int UNARY = 1;

  public static final Map<String, Integer> PRECEDENCES = new HashMap<>();
  public static final Map<String, Integer> TYPES = new HashMap<>();

  static {
    PRECEDENCES.put("==", 2);
    PRECEDENCES.put("!=", 2);
    PRECEDENCES.put(">", 2);
    PRECEDENCES.put("<", 2);
    PRECEDENCES.put(">=", 2);
    PRECEDENCES.put("<=", 2);
    PRECEDENCES.put("contains", 2);
    PRECEDENCES.put("not", 1);
    PRECEDENCES.put("and", 0);
    PRECEDENCES.put("or", 0);
    TYPES.put("==", BINARY);
    TYPES.put("!=", BINARY);
    TYPES.put(">", BINARY);
    TYPES.put("<", BINARY);
    TYPES.put(">=", BINARY);
    TYPES.put("<=", BINARY);
    TYPES.put("contains", BINARY);
    TYPES.put("not", UNARY);
    TYPES.put("and", BINARY);
    TYPES.put("or", BINARY);
  }

  public final String operator;

  public OperatorToken (String input, int begin, int end, String file) {
    super(TokenKind.Operator, input, begin, end, file);
    this.operator = getText();
  }

  public int getPrecedence () {
    Integer precedence = PRECEDENCES.get(operator);
    return precedence == null ? 1 : precedence;
  }
}
