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
import com.liquidjs.render.Expression;
import com.liquidjs.render.Operators;
import com.liquidjs.template.Tag;
import com.liquidjs.template.TagClass;
import com.liquidjs.template.Template;
import com.liquidjs.template.Value;
import com.liquidjs.tokens.TagToken;
import com.liquidjs.tokens.Token;

import java.util.ArrayList;
import java.util.List;

/** Renders the branch whose value equals the one the tag was given. */
public class CaseTag extends Tag {
  /** How the parser builds this tag. */
  public static final TagClass CLASS = CaseTag::new;

  private static final class Branch {
    final List<Token> values = new ArrayList<>();
    final List<Template> templates;

    Branch (List<Template> templates) {
      this.templates = templates;
    }
  }

  private final Value value;
  private final List<Branch> branches = new ArrayList<>();
  private final List<Template> elseTemplates = new ArrayList<>();

  /** The case tag, read from its token and the tokens that follow it. */
  public CaseTag (TagToken tagToken, List<Token> remainTokens, Liquid liquid, Parser parser) {
    super(tagToken, remainTokens, liquid);
    this.value = new Value(this.tokenizer.readFilteredValue(), this.liquid);

    List<List<Template>> current = new ArrayList<>();
    current.add(new ArrayList<>());
    int[] elseCount = { 0 };
    ParseStream stream = parser.parseStream(remainTokens);
    stream
        .onTag("tag:when", token -> {
          if (elseCount[0] > 0) return;
          List<Template> templates = new ArrayList<>();
          current.set(0, templates);
          Branch branch = new Branch(templates);
          while (!token.tokenizer.end()) {
            branch.values.add(token.tokenizer.readValueOrThrow());
            token.tokenizer.skipBlank();
            if (",".equals(token.tokenizer.peek())) {
              token.tokenizer.readTo(",");
            } else {
              token.tokenizer.readTo("or");
            }
          }
          branches.add(branch);
        })
        .on("tag:else", (Runnable) () -> {
          elseCount[0]++;
          current.set(0, elseTemplates);
        })
        .on("tag:endcase", (Runnable) stream::stop)
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
    Object target = Js.toValue(this.value.value(ctx, ctx.opts.lenientIf));
    boolean branchHit = false;
    for (Branch branch : branches) {
      for (Token valueToken : branch.values) {
        Object value = Expression.evalToken(valueToken, ctx, ctx.opts.lenientIf);
        if (Operators.equals(target, value)) {
          r.renderTemplates(branch.templates, ctx, emitter);
          branchHit = true;
          break;
        }
      }
    }
    if (!branchHit) {
      r.renderTemplates(elseTemplates, ctx, emitter);
    }
    return null;
  }

  @Override
  public List<Object> arguments () {
    List<Object> args = new ArrayList<>();
    args.add(value);
    for (Branch branch : branches) args.addAll(branch.values);
    return args;
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
}
