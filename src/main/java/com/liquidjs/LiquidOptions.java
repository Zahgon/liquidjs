/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 Jun Yang
 */
package com.liquidjs;

import com.liquidjs.cache.LRU;
import com.liquidjs.cache.LiquidCache;
import com.liquidjs.filters.HtmlFilters;
import com.liquidjs.filters.MiscFilters;
import com.liquidjs.fs.FS;
import com.liquidjs.fs.FsImpl;
import com.liquidjs.fs.LookupType;
import com.liquidjs.fs.MapFS;
import com.liquidjs.js.Js;
import com.liquidjs.render.Operators;
import com.liquidjs.util.Assertions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Every option the engine takes, already normalised: the directory lists are lists, the cache is
 * a cache instance and the output escape is a function.
 */
public class LiquidOptions {
  /** Directories layout and include templates are resolved from, in order. */
  public List<String> root = new ArrayList<>(Collections.singletonList("."));
  /** Directories included templates are resolved from; defaults to root. */
  public List<String> partials = new ArrayList<>(Collections.singletonList("."));
  /** Directories layout templates are resolved from; defaults to root. */
  public List<String> layouts = new ArrayList<>(Collections.singletonList("."));
  /** Allow layouts and partials to be referenced by a relative path. */
  public boolean relativeReference = true;
  /** Pass include parameters through an include variable, as Jekyll does. */
  public boolean jekyllInclude = false;
  /** Let the where filter match an item of an array, as Jekyll does. */
  public boolean jekyllWhere = false;
  /** An extension added to a template name that has none. */
  public String extname = "";
  /** Where parsed templates are kept, when they are kept. */
  public LiquidCache cache = null;
  /** Use the truthiness of the host language rather than Liquid's. */
  public boolean jsTruthy = false;
  /** Treat a partial's file path as an expression rather than as a literal. */
  public boolean dynamicPartials = true;
  /** Raise on an undefined filter instead of skipping it. */
  public boolean strictFilters = false;
  /** Raise on an undefined variable instead of rendering it as an empty string. */
  public boolean strictVariables = false;
  /** Collect every error instead of stopping at the first. */
  public boolean catchAllErrors = false;
  /** Read only a scope's own properties. */
  public boolean ownPropertyOnly = true;
  /** Let a single undefined variable be false in a condition and null in the default filter. */
  public boolean lenientIf = false;
  /** A timezone name or an offset in minutes for the date filter. */
  public Object timezoneOffset = null;
  /** The format the date filter uses when it is given none. */
  public String dateFormat = "%A, %B %-e, %Y at %-l:%M %P %z";
  /** The locale the date filter names months and weekdays in. */
  public String locale = "";
  /** Strip blanks from the right of a tag, up to and including the newline. */
  public boolean trimTagRight = false;
  /** Strip blanks from the left of a tag, up to the newline. */
  public boolean trimTagLeft = false;
  /** Strip blanks from the right of an output, up to and including the newline. */
  public boolean trimOutputRight = false;
  /** Strip blanks from the left of an output, up to the newline. */
  public boolean trimOutputLeft = false;
  public String tagDelimiterLeft = "{%";
  public String tagDelimiterRight = "%}";
  public String outputDelimiterLeft = "{{";
  public String outputDelimiterRight = "}}";
  /** Keep the timezone an input string declares. */
  public boolean preserveTimezones = false;
  /** Trim every consecutive blank, newlines included. */
  public boolean greedy = true;
  /** Where templates are read from. */
  public FS fs = FsImpl.INSTANCE;
  /** The separator between a tag argument's key and its value. */
  public String keyValueSeparator = ":";
  /** Templates held in memory instead of on a file system. */
  public Map<String, String> templates = null;
  /** The scope every template, partial and layout sees. */
  public Map<String, Object> globals = Js.newObject();
  /** Keep the type of a value written on its own. */
  public boolean keepOutputType = false;
  /** An escape applied to every output value, unless the value is marked raw. */
  public Function<Object, String> outputEscape = null;
  /** The operators a conditional expression may use. */
  public Map<String, Object> operators = Operators.DEFAULT_OPERATORS;
  /** Respect the order filter parameters were written in. */
  public boolean orderedFilterParameters = false;
  /** Allow a parenthesised expression as an operand. */
  public boolean groupedExpressions = false;
  /** Limit the total length of the templates one parse call reads. */
  public double parseLimit = Double.POSITIVE_INFINITY;
  /** Limit the time one render call may take, in milliseconds. */
  public double renderLimit = Double.POSITIVE_INFINITY;
  /** Limit how much new data one render call may create. */
  public double memoryLimit = Double.POSITIVE_INFINITY;

  /** The directory list a lookup type searches. */
  public List<String> directories (LookupType type) {
    switch (type) {
      case Partials: return partials;
      case Layouts: return layouts;
      default: return root;
    }
  }

  /** The locale the host reports, used when no locale option is set. */
  public static String defaultLocale () {
    return "en-US";
  }

  /** A copy of the defaults, which a caller may then change. */
  public static LiquidOptions defaults () {
    return new LiquidOptions();
  }

  /**
   * Read the options out of a mapping whose keys are the documented option names, and fill in
   * every default the mapping leaves out.
   */
  public static LiquidOptions normalize (Map<String, Object> input) {
    Map<String, Object> raw = input == null ? Js.newObject() : new LinkedHashMap<>(input);
    LiquidOptions options = new LiquidOptions();

    if (raw.containsKey("root")) {
      if (!raw.containsKey("partials")) raw.put("partials", raw.get("root"));
      if (!raw.containsKey("layouts")) raw.put("layouts", raw.get("root"));
    }
    if (raw.containsKey("jekyllInclude") && Js.truthy(raw.get("jekyllInclude"))
        && !raw.containsKey("dynamicPartials")) {
      options.dynamicPartials = false;
    }

    for (Map.Entry<String, Object> entry : raw.entrySet()) {
      String key = entry.getKey();
      Object value = entry.getValue();
      switch (key) {
        case "root": options.root = normalizeDirectoryList(value); break;
        case "partials": options.partials = normalizeDirectoryList(value); break;
        case "layouts": options.layouts = normalizeDirectoryList(value); break;
        case "relativeReference": options.relativeReference = Js.truthy(value); break;
        case "jekyllInclude": options.jekyllInclude = Js.truthy(value); break;
        case "jekyllWhere": options.jekyllWhere = Js.truthy(value); break;
        case "extname": options.extname = Js.toStringValue(value); break;
        case "cache": options.cache = normalizeCache(value); break;
        case "jsTruthy": options.jsTruthy = Js.truthy(value); break;
        case "dynamicPartials": options.dynamicPartials = Js.truthy(value); break;
        case "strictFilters": options.strictFilters = Js.truthy(value); break;
        case "strictVariables": options.strictVariables = Js.truthy(value); break;
        case "catchAllErrors": options.catchAllErrors = Js.truthy(value); break;
        case "ownPropertyOnly": options.ownPropertyOnly = Js.truthy(value); break;
        case "lenientIf": options.lenientIf = Js.truthy(value); break;
        case "timezoneOffset": options.timezoneOffset = value; break;
        case "dateFormat": options.dateFormat = Js.toStringValue(value); break;
        case "locale": options.locale = Js.toStringValue(value); break;
        case "trimTagRight": options.trimTagRight = Js.truthy(value); break;
        case "trimTagLeft": options.trimTagLeft = Js.truthy(value); break;
        case "trimOutputRight": options.trimOutputRight = Js.truthy(value); break;
        case "trimOutputLeft": options.trimOutputLeft = Js.truthy(value); break;
        case "tagDelimiterLeft": options.tagDelimiterLeft = Js.toStringValue(value); break;
        case "tagDelimiterRight": options.tagDelimiterRight = Js.toStringValue(value); break;
        case "outputDelimiterLeft": options.outputDelimiterLeft = Js.toStringValue(value); break;
        case "outputDelimiterRight": options.outputDelimiterRight = Js.toStringValue(value); break;
        case "preserveTimezones": options.preserveTimezones = Js.truthy(value); break;
        case "greedy": options.greedy = Js.truthy(value); break;
        case "keyValueSeparator": options.keyValueSeparator = Js.toStringValue(value); break;
        case "keepOutputType": options.keepOutputType = Js.truthy(value); break;
        case "orderedFilterParameters": options.orderedFilterParameters = Js.truthy(value); break;
        case "groupedExpressions": options.groupedExpressions = Js.truthy(value); break;
        case "parseLimit": options.parseLimit = Js.toNumberValue(value); break;
        case "renderLimit": options.renderLimit = Js.toNumberValue(value); break;
        case "memoryLimit": options.memoryLimit = Js.toNumberValue(value); break;
        case "outputEscape": options.outputEscape = outputEscapeFunction(value); break;
        case "fs":
          if (value instanceof FS) options.fs = (FS) value;
          break;
        case "cacheInstance":
          if (value instanceof LiquidCache) options.cache = (LiquidCache) value;
          break;
        case "globals":
          if (value instanceof Map) options.globals = Js.asObject(value);
          break;
        case "templates":
          options.templates = normalizeTemplates(value);
          break;
        case "operators":
          if (value instanceof Map) options.operators = Js.asObject(value);
          break;
        default:
          break;
      }
    }

    if ((options.fs.dirname("x") == null || options.fs.sep() == null) && options.relativeReference) {
      options.relativeReference = false;
    }
    if (options.locale.isEmpty()) options.locale = defaultLocale();
    if (options.templates != null) {
      options.fs = new MapFS(options.templates);
      options.relativeReference = true;
      options.root = normalizeDirectoryList(".");
      options.partials = normalizeDirectoryList(".");
      options.layouts = normalizeDirectoryList(".");
    }
    return options;
  }

  private static Map<String, String> normalizeTemplates (Object value) {
    if (!(value instanceof Map)) return null;
    Map<String, String> templates = new LinkedHashMap<>();
    for (Map.Entry<String, Object> entry : Js.asObject(value).entrySet()) {
      templates.put(entry.getKey(), Js.toStringValue(entry.getValue()));
    }
    return templates;
  }

  private static LiquidCache normalizeCache (Object value) {
    if (value instanceof LiquidCache) return (LiquidCache) value;
    if (Js.isNumber(value)) {
      double size = (Double) value;
      return size > 0 ? new LRU((int) size) : null;
    }
    return Js.truthy(value) ? new LRU(1024) : null;
  }

  private static Function<Object, String> outputEscapeFunction (Object nameOrFunction) {
    if (Js.isNil(nameOrFunction)) return null;
    if ("escape".equals(nameOrFunction)) return HtmlFilters::escapeValue;
    if ("json".equals(nameOrFunction)) return MiscFilters::jsonValue;
    if (nameOrFunction instanceof Function) {
      @SuppressWarnings("unchecked")
      Function<Object, String> escape = (Function<Object, String>) nameOrFunction;
      return escape;
    }
    Assertions.assertTrue(false, "`outputEscape` need to be of type string or function");
    return null;
  }

  /** A directory option as a list, whether it was written as one directory or as several. */
  public static List<String> normalizeDirectoryList (Object value) {
    List<String> list = new ArrayList<>();
    if (Js.isArray(value)) {
      for (Object item : Js.asArray(value)) list.add(Js.toStringValue(item));
    } else if (Js.isString(value)) {
      list.add((String) value);
    }
    return list;
  }
}
