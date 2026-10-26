/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 Jun Yang
 */
package com.liquidjs.parser;

/** The kind of a token, as a bit so a set of kinds is one mask. */
public final class TokenKind {
  private TokenKind () {}

  /** A number literal. */
  public static final int Number = 1;
  /** One of the named literals. */
  public static final int Literal = 2;
  /** A tag, with or without delimiters of its own. */
  public static final int Tag = 4;
  /** An output. */
  public static final int Output = 8;
  /** A run of template text. */
  public static final int HTML = 16;
  /** One filter of a chain. */
  public static final int Filter = 32;
  /** One key and value argument of a tag. */
  public static final int Hash = 64;
  /** A path into the scope or into a value. */
  public static final int PropertyAccess = 128;
  /** A bare word. */
  public static final int Word = 256;
  /** A range of numbers. */
  public static final int Range = 512;
  /** A quoted string. */
  public static final int Quoted = 1024;
  /** An operator of a conditional expression. */
  public static final int Operator = 2048;
  /** A value expression together with its filters. */
  public static final int FilteredValue = 4096;
  /** A parenthesised expression. */
  public static final int GroupedExpression = 8192;
  /** Either of the two kinds written between delimiters. */
  public static final int Delimited = Tag | Output;
}
