/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 Jun Yang
 */
package com.liquidjs.template;

import com.liquidjs.context.Context;
import com.liquidjs.emitters.Emitter;
import com.liquidjs.tokens.Token;

import java.util.List;

/**
 * One node of a parsed template.
 *
 * The members below the render method are optional: a template that does not take part in static
 * analysis returns null from them, which is how the analyser tells that it does not implement them.
 */
public interface Template {
  Token getToken();

  /** Render this node, either into the emitter or as the value it returns. */
  Object render (Context ctx, Emitter emitter);

  /** True when this node holds templates of its own. */
  default boolean hasChildren () {
    return false;
  }

  /** The templates this node holds, or null when it holds none of its own. */
  default List<Template> children (boolean partials, boolean sync) {
    return null;
  }

  /** True when this node renders a partial template. */
  default boolean hasPartialScope () {
    return false;
  }

  /** The value expressions this node reads, or null when it reads none. */
  default List<Object> arguments () {
    return null;
  }

  /** Names this node adds to a block scope, or null when it adds none. */
  default List<String> blockScope () {
    return null;
  }

  /** Names this node adds to the template scope, or null when it adds none. */
  default List<Token> localScope () {
    return null;
  }

  /** The partial this node renders, or null when it renders none. */
  default PartialScope partialScope () {
    return null;
  }
}
