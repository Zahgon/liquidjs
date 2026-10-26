/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 Jun Yang
 */
package com.liquidjs.filters;

import com.liquidjs.js.Js;
import com.liquidjs.js.JsJson;
import com.liquidjs.render.Booleans;
import com.liquidjs.template.FilterHandler;
import com.liquidjs.template.FilterOptions;
import com.liquidjs.util.Limiter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** The filters that are not about one kind of value: defaults, JSON and conversion. */
public final class MiscFilters {
  private MiscFilters () {}

  public static void register (Map<String, Object> filters) {
    filters.put("default", (FilterHandler) (impl, input, args) -> {
      Object value = Js.toValue(input);
      Object defaultValue = Args.at(args, 0);
      if (Js.isArray(value)) {
        return Js.asArray(value).isEmpty() ? defaultValue : value;
      }
      if (Js.isString(value)) {
        return ((String) value).isEmpty() ? defaultValue : value;
      }
      if (Boolean.FALSE.equals(value) && Js.truthy(Args.keyValue(args, "allow_false"))) {
        return Boolean.FALSE;
      }
      return Booleans.isFalsy(value, impl.context) ? defaultValue : value;
    });
    filters.put("raw", new FilterOptions((impl, value, args) -> value, true));
    filters.put("jsonify", (FilterHandler) (impl, value, args) ->
        json(impl.context.memoryLimit, value, Args.at(args, 0)));
    filters.put("json", (FilterHandler) (impl, value, args) ->
        json(impl.context.memoryLimit, value, Args.at(args, 0)));
    filters.put("inspect", (FilterHandler) (impl, value, args) ->
        inspect(impl.context.memoryLimit, value, Args.at(args, 0)));
    filters.put("to_integer", (FilterHandler) (impl, value, args) -> Js.toNumberValue(value));
  }

  /** What a value costs the memory limit when it is serialised. */
  private static void chargeJsonReplacerValue (Limiter memoryLimit, Object val) {
    if (Js.isString(val)) {
      memoryLimit.use(((String) val).length());
    } else if (val == null || Js.isNumber(val) || Js.isBoolean(val)) {
      String serialised = JsJson.stringify(val);
      memoryLimit.use(serialised == null ? 0 : serialised.length());
    } else if (Js.isArray(val)) {
      memoryLimit.use(Js.asArray(val).size() + 1);
    } else if (Js.isObject(val)) {
      memoryLimit.use(2);
    }
  }

  static Object json (Limiter memoryLimit, Object value, Object space) {
    String out = JsJson.stringify(value, JsJson.indentOf(space), (holder, key, val) -> {
      chargeJsonReplacerValue(memoryLimit, val);
      return val;
    });
    return out == null ? Js.UNDEFINED : out;
  }

  /** The serialisation the json filter produces, for the outputEscape option. */
  public static String jsonValue (Object value) {
    String out = JsJson.stringify(value, "", null);
    return out == null ? "" : out;
  }

  static Object inspect (Limiter memoryLimit, Object value, Object space) {
    List<Object> ancestors = new ArrayList<>();
    String out = JsJson.stringify(value, JsJson.indentOf(space), (holder, key, val) -> {
      if (!Js.isObject(val) || val == null) {
        chargeJsonReplacerValue(memoryLimit, val);
        return val;
      }
      // the holder is the value's direct parent, which is how a cycle is recognised
      while (!ancestors.isEmpty() && ancestors.get(ancestors.size() - 1) != holder) {
        ancestors.remove(ancestors.size() - 1);
      }
      for (Object ancestor : ancestors) {
        if (ancestor == val) {
          memoryLimit.use("[Circular]".length());
          return "[Circular]";
        }
      }
      ancestors.add(val);
      chargeJsonReplacerValue(memoryLimit, val);
      return val;
    });
    return out == null ? Js.UNDEFINED : out;
  }
}
