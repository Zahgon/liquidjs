/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 Jun Yang
 */
package com.liquidjs.template;

import java.util.List;

/** A filter implementation: it takes the value, the arguments and its own context. */
public interface FilterHandler {
  Object apply (FilterImpl impl, Object value, List<Object> args);
}
