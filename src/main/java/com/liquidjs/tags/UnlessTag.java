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

import java.util.ArrayList;
import java.util.List;

/** Renders its body unless the condition holds. */
public class UnlessTag extends Tag {
  public static final TagClass CLASS = UnlessTag::new;

  private static final class Branch {
    final Value value;
    final boolean negated;
    final List<Template> templates;

    Branch (Value value, boolean negated, List<Template> templates) {
      this.value = value;
      this.negated = negated;
      this.templates = templates;
    }
  }

  private final List<Branch> branches = new ArrayList<>();
  private final List<Template> elseTemplates = new ArrayList<>();

  public UnlessTag (TagToken tagToken, List<Token> remainTokens, Liquid liquid, Parser parser) {
    super(tagToken, remainTokens, liquid);
    List<List<Template>> current = new ArrayList<>();
    current.add(new ArrayList<>());
    int[] elseCount = { 0 };
    ParseStream stream = parser.parseStream(remainTokens);
    stream
        .on("start", (Runnable) () -> {
          List<Template> templates = new ArrayList<>();
          branches.add(new Branch(
              new Value(tagToken.tokenizer.readFilteredValue(), this.liquid), true, templates));
          current.set(0, templates);
        })
        .onTag("tag:elsif", token -> {
          if (elseCount[0] > 0) {
            current.set(0, new ArrayList<>());
            return;
          }
          List<Template> templates = new ArrayList<>();
          branches.add(new Branch(new Value(token.tokenizer.readFilteredValue(), this.liquid),
              false, templates));
          current.set(0, templates);
        })
        .on("tag:else", (Runnable) () -> {
          elseCount[0]++;
          current.set(0, elseTemplates);
        })
        .on("tag:endunless", (Runnable) stream::stop)
        .onTemplate(tpl -> {
          if (current.get(0) != elseTemplates || elseCount[0] == 1) current.get(0).add(tpl);
        })
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
      boolean hit = branch.negated ? Booleans.isFalsy(v, ctx) : Booleans.isTruthy(v, ctx);
      if (hit) {
        r.renderTemplates(branch.templates, ctx, emitter);
        return null;
      }
    }
    r.renderTemplates(elseTemplates, ctx, emitter);
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
    templates.addAll(elseTemplates);
    return templates;
  }

  @Override
  public List<Object> arguments () {
    List<Object> args = new ArrayList<>();
    for (Branch branch : branches) args.add(branch.value);
    return args;
  }
}
