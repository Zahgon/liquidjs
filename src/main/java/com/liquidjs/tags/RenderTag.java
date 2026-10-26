/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 Jun Yang
 */
package com.liquidjs.tags;

import com.liquidjs.Liquid;
import com.liquidjs.context.Context;
import com.liquidjs.drop.ForloopDrop;
import com.liquidjs.emitters.Emitter;
import com.liquidjs.js.Js;
import com.liquidjs.parser.Parser;
import com.liquidjs.parser.Tokenizer;
import com.liquidjs.render.Expression;
import com.liquidjs.template.Hash;
import com.liquidjs.template.PartialScope;
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
import java.util.List;
import java.util.Map;

/** Renders another template in a scope of its own. */
public class RenderTag extends Tag {
  /** How the parser builds this tag. */
  public static final TagClass CLASS = RenderTag::new;

  /** A name bound to a value, optionally under an alias. */
  private static final class Binding {
    final Token value;
    final String alias;

    Binding (Token value, String alias) {
      this.value = value;
      this.alias = alias;
    }
  }

  private final Object file;
  private final String currentFile;
  private final Hash hash;
  private Binding with;
  private Binding forBinding;

  /** The render tag, read from its token and the tokens that follow it. */
  public RenderTag (TagToken token, List<Token> remainTokens, Liquid liquid, Parser parser) {
    super(token, remainTokens, liquid);
    Tokenizer tokenizer = this.tokenizer;
    this.file = FilePath.parseFilePath(tokenizer, this.liquid, parser);
    this.currentFile = token.file;
    while (!tokenizer.end()) {
      tokenizer.skipBlank();
      int begin = tokenizer.p;
      IdentifierToken keyword = tokenizer.readIdentifier();
      if ("with".equals(keyword.content) || "for".equals(keyword.content)) {
        tokenizer.skipBlank();
        // it may be a plain key and value pair, as in "with: true"
        if (!":".equals(tokenizer.peek())) {
          Token value = tokenizer.readValue();
          // it may be a plain key, as in "with,"
          if (value != null) {
            int beforeAs = tokenizer.p;
            IdentifierToken asStr = tokenizer.readIdentifier();
            String alias = null;
            if ("as".equals(asStr.content)) alias = tokenizer.readIdentifier().content;
            else tokenizer.p = beforeAs;

            Binding binding = new Binding(value, alias);
            if ("with".equals(keyword.content)) this.with = binding;
            else this.forBinding = binding;
            tokenizer.skipBlank();
            if (",".equals(tokenizer.peek())) tokenizer.advance();
            continue;
          }
        }
      }
      // the cursor goes back when neither with nor for matched
      tokenizer.p = begin;
      break;
    }
    this.hash = new Hash(tokenizer, liquid.options.keyValueSeparator);
  }

  @Override
  public Object render (Context ctx, Emitter emitter) {
    Object filepathValue = FilePath.renderFilePath(this.file, ctx, this.liquid);
    String filepath = Js.isNil(filepathValue) ? null : Js.toStringValue(filepathValue);
    Assertions.assertTruthy(filepath,
        "illegal file path \"" + Js.toStringValue(filepathValue) + "\"");

    Context childCtx = ctx.spawn();
    Map<String, Object> scope = childCtx.bottom();
    scope.putAll(this.hash.render(ctx));
    if (this.with != null) {
      String key = this.with.alias != null ? this.with.alias : filepath;
      scope.put(key, Expression.evalToken(this.with.value, ctx));
    }

    if (this.forBinding != null) {
      List<Object> collection = Underscore.toEnumerable(
          Expression.evalToken(this.forBinding.value, ctx));
      ForloopDrop forloop = new ForloopDrop(collection.size(), this.forBinding.value.getText(),
          this.forBinding.alias);
      scope.put("forloop", forloop);
      for (Object item : collection) {
        scope.put(this.forBinding.alias, item);
        List<Template> templates = this.liquid.parsePartialFile(filepath, childCtx.sync,
            this.currentFile);
        this.liquid.renderer.renderTemplates(templates, childCtx, emitter);
        forloop.next();
      }
    } else {
      List<Template> templates = this.liquid.parsePartialFile(filepath, childCtx.sync,
          this.currentFile);
      this.liquid.renderer.renderTemplates(templates, childCtx, emitter);
    }
    return null;
  }

  @Override
  public boolean hasChildren () {
    return true;
  }

  @Override
  public List<Template> children (boolean partials, boolean sync) {
    if (partials && Js.isString(this.file)) {
      return this.liquid.parsePartialFile((String) this.file, sync, this.currentFile);
    }
    return new ArrayList<>();
  }

  @Override
  public boolean hasPartialScope () {
    return true;
  }

  @Override
  public PartialScope partialScope () {
    if (!Js.isString(this.file)) return null;
    List<Object> names = new ArrayList<>(this.hash.hash.keySet());

    if (this.with != null) {
      if (this.with.alias != null) names.add(new Object[] { this.with.alias, this.with.value });
      else names.add(new Object[] { this.file, this.with.value });
    }
    if (this.forBinding != null) {
      if (this.forBinding.alias != null) {
        names.add(new Object[] { this.forBinding.alias, this.forBinding.value });
      } else {
        names.add(new Object[] { this.file, this.forBinding.value });
      }
    }
    return new PartialScope((String) this.file, true, names);
  }

  @Override
  public List<Object> arguments () {
    List<Object> args = new ArrayList<>();
    for (Token value : this.hash.hash.values()) {
      if (value instanceof ValueToken) args.add(value);
    }
    if (this.with != null && this.with.value instanceof ValueToken) args.add(this.with.value);
    if (this.forBinding != null && this.forBinding.value instanceof ValueToken) {
      args.add(this.forBinding.value);
    }
    return args;
  }
}
