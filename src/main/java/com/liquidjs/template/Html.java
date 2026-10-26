/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 Jun Yang
 */
package com.liquidjs.template;

import com.liquidjs.context.Context;
import com.liquidjs.emitters.Emitter;
import com.liquidjs.tokens.HTMLToken;

/** Template text outside any tag or output. */
public class Html extends TemplateImpl<HTMLToken> {
  private final String str;

  /** A node writing the template text its token covers. */
  public Html (HTMLToken token) {
    super(token);
    this.str = token.getContentText();
  }

  @Override
  public Object render (Context ctx, Emitter emitter) {
    emitter.write(str);
    return null;
  }
}
