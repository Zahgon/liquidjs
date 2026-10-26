/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 Jun Yang
 */
package com.liquidjs.template;

import com.liquidjs.Liquid;
import com.liquidjs.parser.Parser;
import com.liquidjs.tokens.TagToken;
import com.liquidjs.tokens.Token;

import java.util.List;

/** How a tag is built from its token and from the tokens that follow it. */
public interface TagClass {
  Tag create (TagToken token, List<Token> remainTokens, Liquid liquid, Parser parser);
}
