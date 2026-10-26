/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 Jun Yang
 */
package com.liquidjs.parser;

/** The kind of a token, as a bit so a set of kinds is one mask. */
public final class TokenKind {
  private TokenKind () {}

  public static final int Number = 1;
  public static final int Literal = 2;
  public static final int Tag = 4;
  public static final int Output = 8;
  public static final int HTML = 16;
  public static final int Filter = 32;
  public static final int Hash = 64;
  public static final int PropertyAccess = 128;
  public static final int Word = 256;
  public static final int Range = 512;
  public static final int Quoted = 1024;
  public static final int Operator = 2048;
  public static final int FilteredValue = 4096;
  public static final int GroupedExpression = 8192;
  public static final int Delimited = Tag | Output;
}
