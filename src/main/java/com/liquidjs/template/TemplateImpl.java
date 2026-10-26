/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 Jun Yang
 */
package com.liquidjs.template;

import com.liquidjs.tokens.Token;

/** A template node that keeps the token it was built from. */
public abstract class TemplateImpl<T extends Token> implements Template {
  public final T token;

  protected TemplateImpl (T token) {
    this.token = token;
  }

  @Override
  public Token getToken () {
    return token;
  }
}
