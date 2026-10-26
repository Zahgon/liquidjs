/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 Jun Yang
 */
package com.liquidjs;

import com.liquidjs.context.Context;
import com.liquidjs.emitters.Emitter;
import com.liquidjs.filters.Filters;
import com.liquidjs.fs.LookupType;
import com.liquidjs.js.Js;
import com.liquidjs.parser.Parser;
import com.liquidjs.render.Render;
import com.liquidjs.tags.Tags;
import com.liquidjs.template.Analysis;
import com.liquidjs.template.StaticAnalysis;
import com.liquidjs.template.StaticAnalysisOptions;
import com.liquidjs.template.TagClass;
import com.liquidjs.template.Template;
import com.liquidjs.template.Value;
import com.liquidjs.template.Variable;
import com.liquidjs.util.Underscore;

import java.io.Reader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

/** The template engine: it parses templates, renders them and reports what they read. */
public class Liquid {
  public final LiquidOptions options;
  public final Render renderer = new Render();
  /**
   * @deprecated will be removed. In tags use the parser the tag was given instead.
   */
  @Deprecated
  public final Parser parser;
  public final Map<String, Object> filters = new LinkedHashMap<>();
  public final Map<String, TagClass> tags = new LinkedHashMap<>();

  public Liquid () {
    this(new LinkedHashMap<>());
  }

  /** The engine, with the options read out of a mapping of option names to values. */
  public Liquid (Map<String, Object> opts) {
    this(LiquidOptions.normalize(opts));
  }

  public Liquid (LiquidOptions options) {
    this.options = options;
    this.parser = new Parser(this);
    for (Map.Entry<String, Object> entry : Tags.all().entrySet()) {
      registerTag(entry.getKey(), (TagClass) entry.getValue());
    }
    for (Map.Entry<String, Object> entry : Filters.all().entrySet()) {
      registerFilter(entry.getKey(), entry.getValue());
    }
  }

  public List<Template> parse (String html) {
    return parse(html, null);
  }

  public List<Template> parse (String html, String filepath) {
    return new Parser(this).parse(html, filepath);
  }

  public Object renderInternal (List<Template> tpl, Object scope, RenderOptions renderOptions) {
    Context ctx = scope instanceof Context
        ? (Context) scope
        : new Context(scope, this.options, renderOptions, this);
    return this.renderer.renderTemplates(tpl, ctx);
  }

  public CompletableFuture<Object> render (List<Template> tpl, Object scope) {
    return render(tpl, scope, null);
  }

  public CompletableFuture<Object> render (List<Template> tpl, Object scope,
                                           RenderOptions renderOptions) {
    return CompletableFuture.completedFuture(
        renderInternal(tpl, scope, withSync(renderOptions, false)));
  }

  public Object renderSync (List<Template> tpl, Object scope) {
    return renderSync(tpl, scope, null);
  }

  public Object renderSync (List<Template> tpl, Object scope, RenderOptions renderOptions) {
    return renderInternal(tpl, scope, withSync(renderOptions, true));
  }

  public Reader renderToNodeStream (List<Template> tpl, Object scope) {
    return renderToNodeStream(tpl, scope, new RenderOptions());
  }

  public Reader renderToNodeStream (List<Template> tpl, Object scope,
                                    RenderOptions renderOptions) {
    Context ctx = new Context(scope, this.options, renderOptions, this);
    return this.renderer.renderTemplatesToNodeStream(tpl, ctx);
  }

  public Object parseAndRenderInternal (String html, Object scope, RenderOptions renderOptions) {
    List<Template> tpl = parse(html);
    return renderInternal(tpl, scope, renderOptions);
  }

  public CompletableFuture<Object> parseAndRender (String html, Object scope) {
    return parseAndRender(html, scope, null);
  }

  public CompletableFuture<Object> parseAndRender (String html, Object scope,
                                                   RenderOptions renderOptions) {
    return CompletableFuture.completedFuture(
        parseAndRenderInternal(html, scope, withSync(renderOptions, false)));
  }

  public Object parseAndRenderSync (String html) {
    return parseAndRenderSync(html, null, null);
  }

  public Object parseAndRenderSync (String html, Object scope) {
    return parseAndRenderSync(html, scope, null);
  }

  public Object parseAndRenderSync (String html, Object scope, RenderOptions renderOptions) {
    return parseAndRenderInternal(html, scope, withSync(renderOptions, true));
  }

  public List<Template> parsePartialFile (String file, boolean sync, String currentFile) {
    return new Parser(this).parseFile(file, LookupType.Partials, currentFile);
  }

  public List<Template> parseLayoutFile (String file, boolean sync, String currentFile) {
    return new Parser(this).parseFile(file, LookupType.Layouts, currentFile);
  }

  public List<Template> parseFileInternal (String file, boolean sync, LookupType lookupType,
                                           String currentFile) {
    return new Parser(this).parseFile(file,
        lookupType == null ? LookupType.Root : lookupType, currentFile);
  }

  public CompletableFuture<List<Template>> parseFile (String file) {
    return parseFile(file, LookupType.Root);
  }

  public CompletableFuture<List<Template>> parseFile (String file, LookupType lookupType) {
    return CompletableFuture.completedFuture(parseFileInternal(file, false, lookupType, null));
  }

  public List<Template> parseFileSync (String file) {
    return parseFileSync(file, LookupType.Root);
  }

  public List<Template> parseFileSync (String file, LookupType lookupType) {
    return parseFileInternal(file, true, lookupType, null);
  }

  public Object renderFileInternal (String file, Object ctx, RenderOptions options,
                                    LookupType lookupType) {
    List<Template> templates = parseFileInternal(file, options.sync, lookupType, null);
    return renderInternal(templates, ctx, options);
  }

  public CompletableFuture<Object> renderFile (String file, Object ctx) {
    return renderFile(file, ctx, null, null);
  }

  public CompletableFuture<Object> renderFile (String file, Object ctx, RenderOptions options,
                                               LookupType lookupType) {
    return CompletableFuture.completedFuture(
        renderFileInternal(file, ctx, withSync(options, false), lookupType));
  }

  public Object renderFileSync (String file, Object ctx) {
    return renderFileSync(file, ctx, null, null);
  }

  public Object renderFileSync (String file, Object ctx, RenderOptions options,
                                LookupType lookupType) {
    return renderFileInternal(file, ctx, withSync(options, true), lookupType);
  }

  public CompletableFuture<Reader> renderFileToNodeStream (String file, Object scope) {
    return renderFileToNodeStream(file, scope, new RenderOptions());
  }

  public CompletableFuture<Reader> renderFileToNodeStream (String file, Object scope,
                                                           RenderOptions renderOptions) {
    List<Template> templates = parseFileInternal(file, false, LookupType.Root, null);
    return CompletableFuture.completedFuture(renderToNodeStream(templates, scope, renderOptions));
  }

  public Object evalValueInternal (String str, Object scope) {
    Value value = new Value(str, this);
    Context ctx = scope instanceof Context
        ? (Context) scope
        : new Context(scope, this.options, new RenderOptions(), this);
    return value.value(ctx);
  }

  public CompletableFuture<Object> evalValue (String str, Object scope) {
    return CompletableFuture.completedFuture(evalValueInternal(str, scope));
  }

  public Object evalValueSync (String str, Object scope) {
    return evalValueInternal(str, scope);
  }

  public void registerFilter (String name, Object filter) {
    this.filters.put(name, filter);
  }

  public void unregisterFilter (String name) {
    this.filters.remove(name);
  }

  public void registerTag (String name, TagClass tag) {
    this.tags.put(name, tag);
  }

  /** Run a plugin, which registers tags and filters of its own. */
  public void plugin (java.util.function.Consumer<Liquid> plugin) {
    plugin.accept(this);
  }

  /**
   * A renderer for a web framework that asks for one: it takes a file path, a scope and a
   * callback, and adds the framework's own view root to this engine's lookup paths.
   */
  public ExpressRenderer express () {
    Liquid self = this;
    return new ExpressRenderer(self);
  }

  /** The renderer express() returns. */
  public static final class ExpressRenderer {
    private final Liquid liquid;
    private boolean firstCall = true;

    ExpressRenderer (Liquid liquid) {
      this.liquid = liquid;
    }

    public void render (String root, String filePath, Object ctx,
                        BiConsumer<Throwable, Object> callback) {
      if (firstCall) {
        firstCall = false;
        List<String> dirs = LiquidOptions.normalizeDirectoryList(root);
        liquid.options.root.addAll(0, dirs);
        liquid.options.layouts.addAll(0, dirs);
        liquid.options.partials.addAll(0, dirs);
      }
      try {
        callback.accept(null, liquid.renderFileSync(filePath, ctx));
      } catch (RuntimeException err) {
        callback.accept(err, null);
      }
    }
  }

  public CompletableFuture<StaticAnalysis> analyze (List<Template> template) {
    return analyze(template, new StaticAnalysisOptions());
  }

  public CompletableFuture<StaticAnalysis> analyze (List<Template> template,
                                                    StaticAnalysisOptions options) {
    return CompletableFuture.completedFuture(Analysis.analyze(template, options));
  }

  public StaticAnalysis analyzeSync (List<Template> template) {
    return analyzeSync(template, new StaticAnalysisOptions());
  }

  public StaticAnalysis analyzeSync (List<Template> template, StaticAnalysisOptions options) {
    return Analysis.analyzeSync(template, options);
  }

  public CompletableFuture<StaticAnalysis> parseAndAnalyze (String html) {
    return parseAndAnalyze(html, null, new StaticAnalysisOptions());
  }

  public CompletableFuture<StaticAnalysis> parseAndAnalyze (String html, String filename,
                                                            StaticAnalysisOptions options) {
    return CompletableFuture.completedFuture(Analysis.analyze(parse(html, filename), options));
  }

  public StaticAnalysis parseAndAnalyzeSync (String html) {
    return parseAndAnalyzeSync(html, null, new StaticAnalysisOptions());
  }

  public StaticAnalysis parseAndAnalyzeSync (String html, String filename) {
    return parseAndAnalyzeSync(html, filename, new StaticAnalysisOptions());
  }

  public StaticAnalysis parseAndAnalyzeSync (String html, String filename,
                                             StaticAnalysisOptions options) {
    return Analysis.analyzeSync(parse(html, filename), options);
  }

  /** Every variable, without its properties. */
  public CompletableFuture<List<String>> variables (String template) {
    return CompletableFuture.completedFuture(variablesSync(template));
  }

  /** Every variable, without its properties. */
  public List<String> variablesSync (String template) {
    return variablesSync(parse(template), new StaticAnalysisOptions());
  }

  public List<String> variablesSync (List<Template> template, StaticAnalysisOptions options) {
    return new ArrayList<>(Analysis.analyzeSync(template, options).variables.keySet());
  }

  /** Every variable, including its properties and paths. */
  public CompletableFuture<List<String>> fullVariables (String template) {
    return CompletableFuture.completedFuture(fullVariablesSync(template));
  }

  /** Every variable, including its properties and paths. */
  public List<String> fullVariablesSync (String template) {
    return fullVariablesSync(parse(template), new StaticAnalysisOptions());
  }

  public List<String> fullVariablesSync (List<Template> template, StaticAnalysisOptions options) {
    return pathsOf(Analysis.analyzeSync(template, options).variables);
  }

  /** Every variable, each one as a list of its segments. */
  public CompletableFuture<List<Object>> variableSegments (String template) {
    return CompletableFuture.completedFuture(variableSegmentsSync(template));
  }

  /** Every variable, each one as a list of its segments. */
  public List<Object> variableSegmentsSync (String template) {
    return variableSegmentsSync(parse(template), new StaticAnalysisOptions());
  }

  public List<Object> variableSegmentsSync (List<Template> template,
                                            StaticAnalysisOptions options) {
    return segmentsOf(Analysis.analyzeSync(template, options).variables);
  }

  /** Every variable the template expects to be given, without its properties. */
  public CompletableFuture<List<String>> globalVariables (String template) {
    return CompletableFuture.completedFuture(globalVariablesSync(template));
  }

  /** Every variable the template expects to be given, without its properties. */
  public List<String> globalVariablesSync (String template) {
    return globalVariablesSync(parse(template), new StaticAnalysisOptions());
  }

  public List<String> globalVariablesSync (List<Template> template,
                                           StaticAnalysisOptions options) {
    return new ArrayList<>(Analysis.analyzeSync(template, options).globals.keySet());
  }

  /** Every variable the template expects to be given, including its properties and paths. */
  public CompletableFuture<List<String>> globalFullVariables (String template) {
    return CompletableFuture.completedFuture(globalFullVariablesSync(template));
  }

  /** Every variable the template expects to be given, including its properties and paths. */
  public List<String> globalFullVariablesSync (String template) {
    return globalFullVariablesSync(parse(template), new StaticAnalysisOptions());
  }

  public List<String> globalFullVariablesSync (List<Template> template,
                                               StaticAnalysisOptions options) {
    return pathsOf(Analysis.analyzeSync(template, options).globals);
  }

  /** Every variable the template expects to be given, each one as a list of its segments. */
  public CompletableFuture<List<Object>> globalVariableSegments (String template) {
    return CompletableFuture.completedFuture(globalVariableSegmentsSync(template));
  }

  /** Every variable the template expects to be given, each one as a list of its segments. */
  public List<Object> globalVariableSegmentsSync (String template) {
    return globalVariableSegmentsSync(parse(template), new StaticAnalysisOptions());
  }

  public List<Object> globalVariableSegmentsSync (List<Template> template,
                                                  StaticAnalysisOptions options) {
    return segmentsOf(Analysis.analyzeSync(template, options).globals);
  }

  private static List<String> pathsOf (Map<String, List<Variable>> variables) {
    List<String> out = new ArrayList<>();
    for (List<Variable> found : variables.values()) {
      for (Variable variable : found) {
        String path = variable.toString();
        if (!out.contains(path)) out.add(path);
      }
    }
    return out;
  }

  private static List<Object> segmentsOf (Map<String, List<Variable>> variables) {
    List<Object> all = new ArrayList<>();
    for (List<Variable> found : variables.values()) {
      for (Variable variable : found) all.add(variable.toArray());
    }
    return Underscore.strictUniq(all);
  }

  private static RenderOptions withSync (RenderOptions options, boolean sync) {
    RenderOptions out = new RenderOptions();
    if (options != null) {
      out.globals = options.globals;
      out.strictVariables = options.strictVariables;
      out.ownPropertyOnly = options.ownPropertyOnly;
      out.templateLimit = options.templateLimit;
      out.renderLimit = options.renderLimit;
      out.memoryLimit = options.memoryLimit;
    }
    out.sync = sync;
    return out;
  }

  /** The engine's version, as the package declares it. */
  public static final String VERSION = "10.30.0";

  /** The value a template wrote, as a string. */
  public static String stringify (Object value) {
    return Underscore.stringify(value);
  }

  /** Write a value into an emitter, as an output does. */
  public static void write (Emitter emitter, Object value) {
    emitter.write(Js.toValue(value));
  }
}
