/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 Jun Yang
 */
package com.liquidjs.tags;

import com.liquidjs.Liquid;
import com.liquidjs.context.BlockMode;
import com.liquidjs.context.Context;
import com.liquidjs.context.Scope;
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
import com.liquidjs.tokens.TagToken;
import com.liquidjs.tokens.Token;
import com.liquidjs.tokens.ValueToken;
import com.liquidjs.util.Assertions;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Renders another template in the current scope. */
public class IncludeTag extends Tag {
  /** How the parser builds this tag. */
  public static final TagClass CLASS = IncludeTag::new;

  private final Object file;
  private final String currentFile;
  private Token withVar;
  private final Hash hash;

  /** The include tag, read from its token and the tokens that follow it. */
  public IncludeTag (TagToken token, List<Token> remainTokens, Liquid liquid, Parser parser) {
    super(token, remainTokens, liquid);
    Tokenizer tokenizer = token.tokenizer;
    this.file = FilePath.parseFilePath(tokenizer, this.liquid, parser);
    this.currentFile = token.file;

    int begin = tokenizer.p;
    String withStr = tokenizer.readIdentifier().content;
    if ("with".equals(withStr)) {
      tokenizer.skipBlank();
      if (!":".equals(tokenizer.peek())) {
        this.withVar = tokenizer.readValue();
      } else {
        tokenizer.p = begin;
      }
    } else {
      tokenizer.p = begin;
    }

    this.hash = new Hash(tokenizer,
        liquid.options.jekyllInclude ? Boolean.TRUE : liquid.options.keyValueSeparator);
  }

  @Override
  public Object render (Context ctx, Emitter emitter) {
    com.liquidjs.render.Render renderer = this.liquid.renderer;
    Object filepathValue = FilePath.renderFilePath(this.file, ctx, this.liquid);
    String filepath = Js.isNil(filepathValue) ? null : Js.toStringValue(filepathValue);
    Assertions.assertTruthy(filepath,
        "illegal file path \"" + Js.toStringValue(filepathValue) + "\"");

    List<Object[]> saved = ctx.saveRegister("blocks", "blockMode");
    ctx.setRegister("blocks", Js.newObject());
    ctx.setRegister("blockMode", BlockMode.OUTPUT);
    Map<String, Object> scope = Scope.createScope(this.hash.render(ctx));
    if (withVar != null) scope.put(filepath, Expression.evalToken(withVar, ctx));
    List<Template> templates = this.liquid.parsePartialFile(filepath, ctx.sync, this.currentFile);
    ctx.push(ctx.opts.jekyllInclude ? Scope.of("include", scope) : scope);
    renderer.renderTemplates(templates, ctx, emitter);
    ctx.pop();
    ctx.restoreRegister(saved);
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
    List<Object> names = new ArrayList<>();
    if (this.liquid.options.jekyllInclude) {
      names.add("include");
    } else {
      names.addAll(this.hash.hash.keySet());
      if (this.withVar != null) {
        names.add(new Object[] { this.file, this.withVar });
      }
    }
    return new PartialScope((String) this.file, false, names);
  }

  @Override
  public List<Object> arguments () {
    List<Object> args = new ArrayList<>();
    for (Token value : this.hash.hash.values()) {
      if (value instanceof ValueToken) args.add(value);
    }
    if (this.file instanceof ValueToken) args.add(this.file);
    if (this.withVar instanceof ValueToken) args.add(this.withVar);
    return args;
  }
}
