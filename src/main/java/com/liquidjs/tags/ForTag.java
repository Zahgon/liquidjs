/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 Jun Yang
 */
package com.liquidjs.tags;

import com.liquidjs.Liquid;
import com.liquidjs.context.Context;
import com.liquidjs.context.Scope;
import com.liquidjs.drop.ForloopDrop;
import com.liquidjs.emitters.Emitter;
import com.liquidjs.js.Js;
import com.liquidjs.parser.ParseStream;
import com.liquidjs.parser.Parser;
import com.liquidjs.render.Expression;
import com.liquidjs.template.Hash;
import com.liquidjs.template.Tag;
import com.liquidjs.template.TagClass;
import com.liquidjs.template.Template;
import com.liquidjs.tokens.IdentifierToken;
import com.liquidjs.tokens.TagToken;
import com.liquidjs.tokens.Token;
import com.liquidjs.tokens.ValueToken;
import com.liquidjs.util.Assertions;
import com.liquidjs.util.Underscore;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/** Renders its body once for every element of a collection. */
public class ForTag extends Tag {
  public static final TagClass CLASS = ForTag::new;

  private static final List<String> MODIFIERS = Arrays.asList("offset", "limit", "reversed");

  private final String variable;
  private final Token collection;
  private final Hash hash;
  private final List<Template> templates = new ArrayList<>();
  private final List<Template> elseTemplates = new ArrayList<>();

  public ForTag (TagToken token, List<Token> remainTokens, Liquid liquid, Parser parser) {
    super(token, remainTokens, liquid);
    IdentifierToken variable = this.tokenizer.readIdentifier();
    IdentifierToken inStr = this.tokenizer.readIdentifier();
    Token collection = this.tokenizer.readValue();
    if (variable.size() == 0 || !"in".equals(inStr.content) || collection == null) {
      throw new RuntimeException("illegal tag: " + token.getText());
    }

    this.variable = variable.content;
    this.collection = collection;
    this.hash = new Hash(this.tokenizer, liquid.options.keyValueSeparator);

    List<List<Template>> current = new ArrayList<>();
    current.add(templates);
    ParseStream stream = parser.parseStream(remainTokens);
    stream
        .on("start", (Runnable) () -> current.set(0, templates))
        .onTag("tag:else", tag -> {
          Assertions.assertEmpty(tag.getArgs().isEmpty() ? null : tag.getArgs());
          current.set(0, elseTemplates);
        })
        .onTag("tag:endfor", tag -> {
          Assertions.assertEmpty(tag.getArgs().isEmpty() ? null : tag.getArgs());
          stream.stop();
        })
        .onTemplate(tpl -> current.get(0).add(tpl))
        .on("end", (Runnable) () -> {
          throw new RuntimeException("tag " + token.getText() + " not closed");
        })
        .start();
  }

  @Override
  public Object render (Context ctx, Emitter emitter) {
    com.liquidjs.render.Render r = this.liquid.renderer;
    List<Object> collection = Underscore.toEnumerable(Expression.evalToken(this.collection, ctx));

    if (collection.isEmpty()) {
      r.renderTemplates(this.elseTemplates, ctx, emitter);
      return null;
    }

    String continueKey = "continue-" + this.variable + "-" + this.collection.getText();
    ctx.push(Scope.of("continue", ctx.getRegister(continueKey, (Object) Js.newObject())));
    Map<String, Object> hash = this.hash.render(ctx);
    ctx.pop();

    List<String> modifiers = new ArrayList<>();
    if (this.liquid.options.orderedFilterParameters) {
      for (String key : hash.keySet()) {
        if (MODIFIERS.contains(key)) modifiers.add(key);
      }
    } else {
      for (String key : MODIFIERS) {
        if (hash.containsKey(key) && !Js.isUndefined(hash.get(key))) modifiers.add(key);
      }
    }

    for (String modifier : modifiers) {
      if ("offset".equals(modifier)) collection = offset(collection, hash.get("offset"));
      else if ("limit".equals(modifier)) collection = limit(collection, hash.get("limit"));
      else collection = reversed(collection);
    }

    Object offsetValue = hash.get("offset");
    double offset = Js.truthy(offsetValue) ? Js.toNumberValue(offsetValue) : 0;
    ctx.setRegister(continueKey, offset + collection.size());
    ForloopDrop forloop = new ForloopDrop(collection.size(), this.collection.getText(),
        this.variable);
    Map<String, Object> scope = Scope.of("forloop", forloop);
    ctx.push(scope);
    for (Object item : collection) {
      scope.put(this.variable, item);
      ctx.continueCalled = false;
      ctx.breakCalled = false;
      r.renderTemplates(this.templates, ctx, emitter);
      if (ctx.breakCalled) break;
      forloop.next();
    }
    ctx.continueCalled = false;
    ctx.breakCalled = false;
    ctx.pop();
    return null;
  }

  @Override
  public boolean hasChildren () {
    return true;
  }

  @Override
  public List<Template> children (boolean partials, boolean sync) {
    List<Template> out = new ArrayList<>(templates);
    out.addAll(elseTemplates);
    return out;
  }

  @Override
  public List<Object> arguments () {
    List<Object> args = new ArrayList<>();
    args.add(collection);
    for (Token value : hash.hash.values()) {
      if (value instanceof ValueToken) args.add(value);
    }
    return args;
  }

  @Override
  public List<String> blockScope () {
    return Arrays.asList(variable, "forloop");
  }

  private static List<Object> reversed (List<Object> array) {
    List<Object> out = new ArrayList<>(array);
    java.util.Collections.reverse(out);
    return out;
  }

  private static List<Object> offset (List<Object> array, Object count) {
    int from = (int) Js.toNumberValue(count);
    if (from < 0) from = Math.max(array.size() + from, 0);
    if (from > array.size()) from = array.size();
    return new ArrayList<>(array.subList(from, array.size()));
  }

  private static List<Object> limit (List<Object> array, Object count) {
    double raw = Js.toNumberValue(count);
    int to = Double.isNaN(raw) ? 0 : (int) raw;
    if (to < 0) to = Math.max(array.size() + to, 0);
    if (to > array.size()) to = array.size();
    return new ArrayList<>(array.subList(0, to));
  }
}
