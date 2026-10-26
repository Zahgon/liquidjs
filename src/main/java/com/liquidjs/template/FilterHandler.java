/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 Jun Yang
 */
package com.liquidjs.template;

import java.util.List;

/** A filter implementation: it takes the value, the arguments and its own context. */
public interface FilterHandler {
  /** Apply the filter to a value, with the arguments it was given. */
  Object apply (FilterImpl impl, Object value, List<Object> args);
}
