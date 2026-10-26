/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 Jun Yang
 */
package com.liquidjs.filters;

import com.liquidjs.drop.EmptyDrop;
import com.liquidjs.js.Js;
import com.liquidjs.js.JsString;
import com.liquidjs.parser.Tokenizer;
import com.liquidjs.render.Booleans;
import com.liquidjs.render.Expression;
import com.liquidjs.render.Operators;
import com.liquidjs.template.FilterHandler;
import com.liquidjs.template.FilterImpl;
import com.liquidjs.template.Value;
import com.liquidjs.tokens.Token;
import com.liquidjs.util.Underscore;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** The filters over arrays. */
public final class ArrayFilters {
  private ArrayFilters () {}

  public static void register (Map<String, Object> filters) {
    filters.put("join", (FilterHandler) (impl, v, args) -> {
      List<Object> array = Underscore.toArray(Js.toValue(v));
      Object arg = Args.value(args, 0);
      String sep = Js.isNil(arg) ? " " : Underscore.stringify(arg);
      int outputSize = sep.length() * Math.max(array.size() - 1, 0);
      for (Object item : array) outputSize += Js.toStringValue(item).length();
      impl.context.memoryLimit.use(outputSize);
      List<String> parts = new ArrayList<>();
      for (Object item : array) parts.add(Js.isNil(item) ? "" : Js.toStringValue(item));
      return String.join(sep, parts);
    });
    filters.put("last", (FilterHandler) (impl, v, args) -> {
      Object value = Js.toValue(v);
      if (!Js.isArrayLike(value)) return "";
      if (Js.isString(value)) {
        String str = (String) value;
        return str.isEmpty() ? Js.UNDEFINED : String.valueOf(str.charAt(str.length() - 1));
      }
      return Underscore.readArrayElement(Js.asArray(value), -1, impl.context.ownPropertyOnly);
    });
    filters.put("first", (FilterHandler) (impl, v, args) -> {
      Object value = Js.toValue(v);
      if (!Js.isArrayLike(value)) return "";
      if (Js.isString(value)) {
        String str = (String) value;
        return str.isEmpty() ? Js.UNDEFINED : String.valueOf(str.charAt(0));
      }
      return Underscore.readArrayElement(Js.asArray(value), 0, impl.context.ownPropertyOnly);
    });
    filters.put("reverse", (FilterHandler) (impl, v, args) -> {
      List<Object> array = Underscore.toArray(Js.toValue(v));
      impl.context.memoryLimit.use(array.size());
      List<Object> out = new ArrayList<>(array);
      java.util.Collections.reverse(out);
      return out;
    });
    filters.put("sort", (FilterHandler) (impl, v, args) ->
        sortBy(impl, v, Args.at(args, 0), Underscore::orderedCompare));
    filters.put("sort_natural", (FilterHandler) (impl, v, args) ->
        sortBy(impl, v, Args.at(args, 0), Underscore::caseInsensitiveCompare));
    filters.put("size", (FilterHandler) (impl, v, args) -> {
      Object value = Js.toValue(v);
      if (Js.isString(value)) return (double) ((String) value).length();
      if (Js.isArray(value)) return (double) Js.asArray(value).size();
      return 0.0;
    });
    filters.put("map", (FilterHandler) (impl, v, args) -> {
      List<Object> results = new ArrayList<>();
      List<Object> array = Underscore.toArray(Js.toValue(v));
      impl.context.memoryLimit.use(array.size());
      String property = Args.string(args, 0);
      for (Object item : array) {
        results.add(impl.context.getFromScope(item, property, false));
      }
      return results;
    });
    filters.put("sum", (FilterHandler) (impl, v, args) -> {
      double sum = 0;
      List<Object> array = Underscore.toArray(Js.toValue(v));
      Object property = Args.at(args, 0);
      for (Object item : array) {
        Object read = Js.truthy(property)
            ? impl.context.getFromScope(item, Underscore.stringify(property), false)
            : item;
        double data = Js.toNumberValue(read);
        sum += Double.isNaN(data) ? 0 : data;
      }
      return sum;
    });
    filters.put("compact", (FilterHandler) (impl, v, args) -> {
      List<Object> array = Underscore.toArray(Js.toValue(v));
      impl.context.memoryLimit.use(array.size());
      List<Object> out = new ArrayList<>();
      for (Object item : array) {
        if (!Js.isNil(Js.toValue(item))) out.add(item);
      }
      return out;
    });
    filters.put("concat", (FilterHandler) (impl, v, args) -> {
      List<Object> lhs = Underscore.toArray(Js.toValue(v));
      List<Object> rhs = Underscore.toArray(Args.value(args, 0));
      impl.context.memoryLimit.use(lhs.size() + rhs.size());
      List<Object> out = new ArrayList<>(lhs);
      out.addAll(rhs);
      return out;
    });
    filters.put("push", (FilterHandler) (impl, v, args) -> {
      List<Object> lhs = Underscore.toArray(Js.toValue(v));
      List<Object> rhs = new ArrayList<>();
      rhs.add(Args.at(args, 0));
      impl.context.memoryLimit.use(lhs.size() + rhs.size());
      List<Object> out = new ArrayList<>(lhs);
      out.addAll(rhs);
      return out;
    });
    filters.put("unshift", (FilterHandler) (impl, v, args) -> {
      List<Object> array = Underscore.toArray(Js.toValue(v));
      impl.context.memoryLimit.use(array.size());
      List<Object> clone = new ArrayList<>(array);
      clone.add(0, Args.at(args, 0));
      return clone;
    });
    filters.put("pop", (FilterHandler) (impl, v, args) -> {
      List<Object> array = Underscore.toArray(Js.toValue(v));
      impl.context.memoryLimit.use(array.size());
      List<Object> clone = new ArrayList<>(array);
      if (!clone.isEmpty()) clone.remove(clone.size() - 1);
      return clone;
    });
    filters.put("shift", (FilterHandler) (impl, v, args) -> {
      List<Object> array = Underscore.toArray(Js.toValue(v));
      impl.context.memoryLimit.use(array.size());
      List<Object> clone = new ArrayList<>(array);
      if (!clone.isEmpty()) clone.remove(0);
      return clone;
    });
    filters.put("slice", (FilterHandler) (impl, v, args) -> {
      Object value = Js.toValue(v);
      if (Js.isNil(value)) return new ArrayList<>();
      double beginRaw = Args.number(args, 0);
      double lengthRaw = Args.number(args, 1, 1);
      if (!Js.isArray(value)) value = Underscore.stringify(value);
      int size = Js.isArray(value) ? Js.asArray(value).size() : ((String) value).length();
      int begin = (int) beginRaw;
      if (begin < 0) begin = size + begin;
      if (begin < 0 || lengthRaw < 0) return Js.isArray(value) ? new ArrayList<>() : "";
      impl.context.memoryLimit.use(lengthRaw);
      int from = Math.min(Math.max(begin, 0), size);
      int to = (int) Math.min(Math.max(from + lengthRaw, from), size);
      if (Js.isArray(value)) return new ArrayList<>(Js.asArray(value).subList(from, to));
      return ((String) value).substring(from, to);
    });
    filters.put("where", (FilterHandler) (impl, v, args) ->
        filter(impl, true, v, Args.string(args, 0), Args.at(args, 1)));
    filters.put("reject", (FilterHandler) (impl, v, args) ->
        filter(impl, false, v, Args.string(args, 0), Args.at(args, 1)));
    filters.put("where_exp", (FilterHandler) (impl, v, args) ->
        filterExpression(impl, true, v, Args.string(args, 0), Args.string(args, 1)));
    filters.put("reject_exp", (FilterHandler) (impl, v, args) ->
        filterExpression(impl, false, v, Args.string(args, 0), Args.string(args, 1)));
    filters.put("group_by", (FilterHandler) (impl, v, args) -> {
      Map<Object, List<Object>> map = new LinkedHashMap<>();
      List<Object> array = Underscore.toEnumerable(v);
      Token token = new Tokenizer(Args.string(args, 0)).readScopeValue();
      impl.context.memoryLimit.use(array.size());
      for (Object item : array) {
        Object key = Expression.evalToken(token, impl.context.spawn(item));
        map.computeIfAbsent(key, k -> new ArrayList<>()).add(item);
      }
      return groups(map);
    });
    filters.put("group_by_exp", (FilterHandler) (impl, v, args) -> {
      Map<Object, List<Object>> map = new LinkedHashMap<>();
      Value keyTemplate = new Value(Args.string(args, 1), impl.liquid);
      List<Object> array = Underscore.toEnumerable(v);
      String itemName = Args.string(args, 0);
      impl.context.memoryLimit.use(array.size());
      for (Object item : array) {
        impl.context.push(com.liquidjs.context.Scope.of(itemName, item));
        Object key = keyTemplate.value(impl.context);
        impl.context.pop();
        map.computeIfAbsent(key, k -> new ArrayList<>()).add(item);
      }
      return groups(map);
    });
    filters.put("has", (FilterHandler) (impl, v, args) ->
        search(impl, v, Args.string(args, 0), Args.at(args, 1)) != null);
    filters.put("has_exp", (FilterHandler) (impl, v, args) ->
        searchExpression(impl, v, Args.string(args, 0), Args.string(args, 1)) != null);
    filters.put("find_index", (FilterHandler) (impl, v, args) -> {
      Object[] result = search(impl, v, Args.string(args, 0), Args.at(args, 1));
      return result == null ? Js.UNDEFINED : result[0];
    });
    filters.put("find_index_exp", (FilterHandler) (impl, v, args) -> {
      Object[] result = searchExpression(impl, v, Args.string(args, 0), Args.string(args, 1));
      return result == null ? Js.UNDEFINED : result[0];
    });
    filters.put("find", (FilterHandler) (impl, v, args) -> {
      Object[] result = search(impl, v, Args.string(args, 0), Args.at(args, 1));
      return result == null ? Js.UNDEFINED : result[1];
    });
    filters.put("find_exp", (FilterHandler) (impl, v, args) -> {
      Object[] result = searchExpression(impl, v, Args.string(args, 0), Args.string(args, 1));
      return result == null ? Js.UNDEFINED : result[1];
    });
    filters.put("uniq", (FilterHandler) (impl, v, args) -> {
      List<Object> array = Underscore.toArray(Js.toValue(v));
      impl.context.memoryLimit.use(array.size());
      return Underscore.uniq(array);
    });
    filters.put("sample", (FilterHandler) (impl, v, args) -> {
      Object value = Js.toValue(v);
      if (Js.isNil(value)) return new ArrayList<>();
      List<Object> items = new ArrayList<>();
      if (Js.isArray(value)) {
        items.addAll(Js.asArray(value));
      } else {
        String str = Underscore.stringify(value);
        for (int i = 0; i < str.length(); i++) items.add(String.valueOf(str.charAt(i)));
      }
      impl.context.memoryLimit.use(items.size());
      double count = Args.number(args, 0, 1);
      List<Object> shuffled = new ArrayList<>(items);
      java.util.Collections.shuffle(shuffled);
      if (count == 1) return shuffled.isEmpty() ? Js.UNDEFINED : shuffled.get(0);
      int to = (int) Math.min(Math.max(count, 0), shuffled.size());
      return new ArrayList<>(shuffled.subList(0, to));
    });
  }

  private static List<Object> groups (Map<Object, List<Object>> map) {
    List<Object> out = new ArrayList<>();
    for (Map.Entry<Object, List<Object>> entry : map.entrySet()) {
      Map<String, Object> group = Js.newObject();
      group.put("name", entry.getKey());
      group.put("items", entry.getValue());
      out.add(group);
    }
    return out;
  }

  private static List<Object> sortBy (FilterImpl impl, Object arr, Object property,
                                      Comparator<Object> comparator) {
    List<Object[]> values = new ArrayList<>();
    List<Object> array = Underscore.toArray(Js.toValue(arr));
    impl.context.memoryLimit.use(array.size());
    for (Object item : array) {
      Object key = Js.truthy(property)
          ? impl.context.getFromScope(item,
              new ArrayList<>(JsString.split(Underscore.stringify(property), ".")), false)
          : item;
      values.add(new Object[] { item, key });
    }
    values.sort((lhs, rhs) -> comparator.compare(lhs[1], rhs[1]));
    List<Object> out = new ArrayList<>();
    for (Object[] pair : values) out.add(pair[0]);
    return out;
  }

  private static boolean matches (FilterImpl impl, Object value, Object expected) {
    if (impl.context.opts.jekyllWhere) {
      if (EmptyDrop.is(expected)) return Operators.equals(value, expected);
      if (Js.isArray(value)) return Operators.arrayIncludes(Js.asArray(value), expected);
      return Operators.equals(value, expected);
    }
    if (Js.isUndefined(expected)) return Booleans.isTruthy(value, impl.context);
    return Operators.equals(value, expected);
  }

  private static List<Object> filter (FilterImpl impl, boolean include, Object arr,
                                      String property, Object expected) {
    List<Object> array = Underscore.toArray(Js.toValue(arr));
    impl.context.memoryLimit.use(array.size());
    Token token = new Tokenizer(property).readScopeValue();
    List<Object> result = new ArrayList<>();
    for (Object item : array) {
      Object value = Expression.evalToken(token, impl.context.spawn(item));
      if (matches(impl, value, expected) == include) result.add(item);
    }
    return result;
  }

  private static List<Object> filterExpression (FilterImpl impl, boolean include, Object arr,
                                                String itemName, String exp) {
    List<Object> filtered = new ArrayList<>();
    Value keyTemplate = new Value(exp, impl.liquid);
    List<Object> array = Underscore.toArray(Js.toValue(arr));
    impl.context.memoryLimit.use(array.size());
    for (Object item : array) {
      impl.context.push(com.liquidjs.context.Scope.of(itemName, item));
      Object value = keyTemplate.value(impl.context);
      impl.context.pop();
      if (Boolean.valueOf(include).equals(value)) filtered.add(item);
    }
    return filtered;
  }

  private static Object[] search (FilterImpl impl, Object arr, String property, Object expected) {
    Token token = new Tokenizer(property).readScopeValue();
    List<Object> array = Underscore.toArray(Js.toValue(arr));
    for (int index = 0; index < array.size(); index++) {
      Object value = Expression.evalToken(token, impl.context.spawn(array.get(index)));
      if (matches(impl, value, expected)) {
        return new Object[] { (double) index, array.get(index) };
      }
    }
    return null;
  }

  private static Object[] searchExpression (FilterImpl impl, Object arr, String itemName,
                                            String exp) {
    Value predicate = new Value(exp, impl.liquid);
    List<Object> array = Underscore.toArray(Js.toValue(arr));
    for (int index = 0; index < array.size(); index++) {
      impl.context.push(com.liquidjs.context.Scope.of(itemName, array.get(index)));
      Object value = predicate.value(impl.context);
      impl.context.pop();
      if (Js.truthy(value)) return new Object[] { (double) index, array.get(index) };
    }
    return null;
  }
}
