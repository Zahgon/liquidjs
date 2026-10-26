/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 Jun Yang
 */
package com.liquidjs.context;

import com.liquidjs.Liquid;
import com.liquidjs.LiquidOptions;
import com.liquidjs.RenderOptions;
import com.liquidjs.drop.Drop;
import com.liquidjs.js.Js;
import com.liquidjs.util.InternalUndefinedVariableError;
import com.liquidjs.util.Limiter;
import com.liquidjs.util.Underscore;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** The scopes a render call reads its variables from, innermost first. */
public class Context {
  /**
   * A context level empty scope, for tags such as capture and assign to write into.
   */
  private final List<Object> scopes = new ArrayList<>();
  private final Map<String, Object> registers = Js.newObject();
  /**
   * The scope the caller passed in. The increment and decrement tags change this scope, while
   * capture and assign only hide it.
   */
  public Object environments;
  /** The scope missing variables fall back to. */
  public Object globals;
  public boolean sync;
  public boolean breakCalled = false;
  public boolean continueCalled = false;
  /** The normalised options. */
  public final LiquidOptions opts;
  /** The engine, so a filter can be resolved while rendering. */
  public Liquid liquid;
  /** Raise when a variable that is not defined is read? */
  public boolean strictVariables;
  public boolean ownPropertyOnly;
  public Limiter memoryLimit;
  public Limiter renderLimit;

  public Context () {
    this(null, LiquidOptions.defaults(), new RenderOptions(), null, null, null);
  }

  public Context (Object env, LiquidOptions opts, RenderOptions renderOptions, Liquid liquid) {
    this(env, opts, renderOptions, null, null, liquid);
  }

  public Context (Object env, LiquidOptions opts, RenderOptions renderOptions, Limiter memoryLimit,
                  Limiter renderLimit, Liquid liquid) {
    this.scopes.add(Scope.createScope());
    RenderOptions options = renderOptions == null ? new RenderOptions() : renderOptions;
    this.sync = options.sync;
    this.opts = opts == null ? LiquidOptions.defaults() : opts;
    this.globals = options.globals != null ? options.globals : this.opts.globals;
    this.environments = Js.isObject(env) ? env : Js.newObject();
    this.strictVariables = options.strictVariables != null
        ? options.strictVariables : this.opts.strictVariables;
    this.ownPropertyOnly = options.ownPropertyOnly != null
        ? options.ownPropertyOnly : this.opts.ownPropertyOnly;
    this.memoryLimit = memoryLimit != null ? memoryLimit : new Limiter("memory alloc",
        options.memoryLimit != null ? options.memoryLimit : this.opts.memoryLimit);
    this.renderLimit = renderLimit != null ? renderLimit : new Limiter("template render",
        now() + (options.renderLimit != null ? options.renderLimit : this.opts.renderLimit));
    this.liquid = liquid;
  }

  /** The milliseconds a render limit is measured against. */
  public static double now () {
    return System.currentTimeMillis();
  }

  @SuppressWarnings("unchecked")
  public <T> T getRegister (String key) {
    return (T) registers.get(key);
  }

  @SuppressWarnings("unchecked")
  public <T> T getRegister (String key, T defaultValue) {
    Object value = registers.get(key);
    if (value == null) {
      registers.put(key, defaultValue);
      return defaultValue;
    }
    return (T) value;
  }

  public Object setRegister (String key, Object value) {
    registers.put(key, value);
    return value;
  }

  public List<Object[]> saveRegister (String... keys) {
    List<Object[]> saved = new ArrayList<>();
    for (String key : keys) saved.add(new Object[] { key, getRegister(key) });
    return saved;
  }

  public void restoreRegister (List<Object[]> keyValues) {
    for (Object[] pair : keyValues) setRegister((String) pair[0], pair[1]);
  }

  /** Every scope flattened into one object, innermost last. */
  public Map<String, Object> getAll () {
    Map<String, Object> all = Js.newObject();
    assign(all, globals);
    assign(all, environments);
    for (Object scope : scopes) assign(all, scope);
    return all;
  }

  private static void assign (Map<String, Object> target, Object source) {
    if (source instanceof Map) target.putAll(Js.asObject(source));
  }

  public Object get (List<Object> paths) {
    Object scope = findScope(paths.isEmpty() ? null : paths.get(0));
    return getFromScope(scope, paths);
  }

  public Object getSync (List<Object> paths) {
    return get(paths);
  }

  public Object getFromScope (Object scope, String path) {
    return getFromScope(scope, new ArrayList<>(com.liquidjs.js.JsString.split(path, ".")),
        strictVariables);
  }

  public Object getFromScope (Object scope, String path, boolean strict) {
    return getFromScope(scope, new ArrayList<>(com.liquidjs.js.JsString.split(path, ".")), strict);
  }

  public Object getFromScope (Object scope, List<Object> paths) {
    return getFromScope(scope, paths, strictVariables);
  }

  public Object getFromScope (Object input, List<Object> paths, boolean strict) {
    Object scope = input;
    for (int i = 0; i < paths.size(); i++) {
      scope = readProperty(scope, paths.get(i));
      if (strict && Js.isUndefined(scope)) {
        List<String> read = new ArrayList<>();
        for (int j = 0; j <= i; j++) read.add(Js.toStringValue(paths.get(j)));
        throw new InternalUndefinedVariableError(String.join(".", read));
      }
    }
    return scope;
  }

  public void push (Object ctx) {
    scopes.add(ctx);
  }

  public Object pop () {
    return scopes.isEmpty() ? null : scopes.remove(scopes.size() - 1);
  }

  @SuppressWarnings("unchecked")
  public Map<String, Object> bottom () {
    return (Map<String, Object>) scopes.get(0);
  }

  public Context spawn () {
    return spawn(Scope.createScope());
  }

  public Context spawn (Object scope) {
    RenderOptions options = new RenderOptions();
    options.sync = this.sync;
    options.globals = this.globals instanceof Map ? Js.asObject(this.globals) : null;
    options.strictVariables = this.strictVariables;
    options.ownPropertyOnly = this.ownPropertyOnly;
    return new Context(scope, this.opts, options, this.memoryLimit, this.renderLimit, this.liquid);
  }

  private Object findScope (Object key) {
    for (int i = scopes.size() - 1; i >= 0; i--) {
      Object candidate = scopes.get(i);
      if (hasKey(candidate, key)) return candidate;
    }
    if (hasKey(environments, key)) return environments;
    return globals;
  }

  /** Whether a scope has a key at all, as the in operator asks. */
  private static boolean hasKey (Object scope, Object key) {
    if (scope instanceof Map) return Js.asObject(scope).containsKey(Js.toStringValue(key));
    if (scope instanceof Drop) return !Js.isUndefined(((Drop) scope).getProperty(Js.toStringValue(key)));
    if (scope instanceof List) {
      String name = Js.toStringValue(key);
      return Js.isArrayIndex(name) && Long.parseLong(name) < Js.asArray(scope).size();
    }
    return false;
  }

  /** Read one property of a value, with the size, first and last a template may ask for. */
  public Object readProperty (Object input, Object rawKey) {
    Object obj = input;
    Object key = Js.toValue(rawKey);
    if (Js.isNil(obj)) return obj;
    if (Js.isArray(obj) && Js.isNumber(key)) {
      return Underscore.readArrayElement(Js.asArray(obj), (int) (double) (Double) key,
          ownPropertyOnly);
    }
    String name = Js.toStringValue(key);
    Object value = readJsProperty(obj, name, ownPropertyOnly);
    if (Js.isUndefined(value) && obj instanceof Drop) {
      return ((Drop) obj).liquidMethodMissing(key, this);
    }
    if (!Js.isUndefined(value)) return value;
    if ("size".equals(name)) return readSize(obj);
    if ("first".equals(name)) return readFirst(obj, ownPropertyOnly);
    if ("last".equals(name)) return readLast(obj, ownPropertyOnly);
    return value;
  }

  public static Object readJsProperty (Object obj, String key, boolean ownPropertyOnly) {
    if (obj instanceof Drop) return ((Drop) obj).getProperty(key);
    if (obj instanceof Map) {
      Map<String, Object> object = Js.asObject(obj);
      if (!object.containsKey(key)) return Js.UNDEFINED;
      Object value = object.get(key);
      return value == null ? null : value;
    }
    if (obj instanceof List) {
      List<Object> array = Js.asArray(obj);
      if (Js.isArrayIndex(key)) {
        long index = Long.parseLong(key);
        return index < array.size() ? array.get((int) index) : Js.UNDEFINED;
      }
      if ("length".equals(key)) return (double) array.size();
      return Js.UNDEFINED;
    }
    if (obj instanceof String) {
      String str = (String) obj;
      if ("length".equals(key)) return (double) str.length();
      if (Js.isArrayIndex(key)) {
        long index = Long.parseLong(key);
        return index < str.length() ? String.valueOf(str.charAt((int) index)) : Js.UNDEFINED;
      }
      return Js.UNDEFINED;
    }
    return Js.UNDEFINED;
  }

  private static Object readFirst (Object obj, boolean ownPropertyOnly) {
    if (Js.isArray(obj)) return Underscore.readArrayElement(Js.asArray(obj), 0, ownPropertyOnly);
    return readJsProperty(obj, "first", ownPropertyOnly);
  }

  private static Object readLast (Object obj, boolean ownPropertyOnly) {
    if (Js.isArray(obj)) return Underscore.readArrayElement(Js.asArray(obj), -1, ownPropertyOnly);
    return readJsProperty(obj, "last", ownPropertyOnly);
  }

  private static Object readSize (Object obj) {
    Object own = readJsProperty(obj, "size", false);
    if (!Js.isUndefined(own)) return own;
    if (Js.isArray(obj)) return (double) Js.asArray(obj).size();
    if (Js.isString(obj)) return (double) ((String) obj).length();
    if (obj instanceof Map) return (double) Js.keys(Js.asObject(obj)).size();
    return Js.UNDEFINED;
  }
}
