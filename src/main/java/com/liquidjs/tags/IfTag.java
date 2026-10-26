/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 Jun Yang
 */
package com.liquidjs.tags;

import com.liquidjs.Liquid;
import com.liquidjs.context.Context;
import com.liquidjs.emitters.Emitter;
import com.liquidjs.js.Js;
import com.liquidjs.parser.ParseStream;
import com.liquidjs.parser.Parser;
import com.liquidjs.render.Booleans;
import com.liquidjs.template.Tag;
import com.liquidjs.template.TagClass;
import com.liquidjs.template.Template;
import com.liquidjs.template.Value;
import com.liquidjs.tokens.TagToken;
import com.liquidjs.tokens.Token;
import com.liquidjs.util.Assertions;

import java.util.ArrayList;
import java.util.List;

/** Renders the body of the first branch whose condition holds. */
public class IfTag extends Tag {
  public static final TagClass CLASS = IfTag::new;

  /** One branch: its condition and the body to render when the condition holds. */
  static final class Branch {
    final Value value;
    final List<Template> templates;

    Branch (Value value, List<Template> templates) {
      this.value = value;
      this.templates = templates;
    }
  }

  final List<Branch> branches = new ArrayList<>();
  List<Template> elseTemplates;

  public IfTag (TagToken tagToken, List<Token> remainTokens, Liquid liquid, Parser parser) {
    super(tagToken, remainTokens, liquid);
    List<List<Template>> current = new ArrayList<>();
    current.add(new ArrayList<>());
    ParseStream stream = parser.parseStream(remainTokens);
    stream
        .on("start", (Runnable) () -> {
          List<Template> templates = new ArrayList<>();
          branches.add(new Branch(
              new Value(tagToken.tokenizer.readFilteredValue(), this.liquid), templates));
          current.set(0, templates);
        })
        .onTag("tag:elsif", token -> {
          Assertions.assertTrue(elseTemplates == null, "unexpected elsif after else");
          List<Template> templates = new ArrayList<>();
          branches.add(new Branch(new Value(token.tokenizer.readFilteredValue(), this.liquid),
              templates));
          current.set(0, templates);
        })
        .onTag("tag:else", tag -> {
          Assertions.assertEmpty(tag.getArgs().isEmpty() ? null : tag.getArgs());
          Assertions.assertTrue(elseTemplates == null, "duplicated else");
          elseTemplates = new ArrayList<>();
          current.set(0, elseTemplates);
        })
        .onTag("tag:endif", tag -> {
          Assertions.assertEmpty(tag.getArgs().isEmpty() ? null : tag.getArgs());
          stream.stop();
        })
        .onTemplate(tpl -> current.get(0).add(tpl))
        .on("end", (Runnable) () -> {
          throw new RuntimeException("tag " + tagToken.getText() + " not closed");
        })
        .start();
  }

  @Override
  public Object render (Context ctx, Emitter emitter) {
    com.liquidjs.render.Render r = this.liquid.renderer;
    for (Branch branch : branches) {
      Object v = Js.toValue(branch.value.value(ctx, ctx.opts.lenientIf));
      if (Booleans.isTruthy(v, ctx)) {
        r.renderTemplates(branch.templates, ctx, emitter);
        return null;
      }
    }
    r.renderTemplates(elseTemplates == null ? new ArrayList<>() : elseTemplates, ctx, emitter);
    return null;
  }

  @Override
  public boolean hasChildren () {
    return true;
  }

  @Override
  public List<Template> children (boolean partials, boolean sync) {
    List<Template> templates = new ArrayList<>();
    for (Branch branch : branches) templates.addAll(branch.templates);
    if (elseTemplates != null) templates.addAll(elseTemplates);
    return templates;
  }

  @Override
  public List<Object> arguments () {
    List<Object> args = new ArrayList<>();
    for (Branch branch : branches) args.add(branch.value);
    return args;
  }
}
