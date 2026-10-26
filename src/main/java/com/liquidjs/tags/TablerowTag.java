/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 Jun Yang
 */
package com.liquidjs.tags;

import com.liquidjs.Liquid;
import com.liquidjs.context.Context;
import com.liquidjs.context.Scope;
import com.liquidjs.drop.TablerowloopDrop;
import com.liquidjs.emitters.Emitter;
import com.liquidjs.js.Js;
import com.liquidjs.js.JsNumber;
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
import com.liquidjs.util.Underscore;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/** Renders its body once per element, laid out as a table. */
public class TablerowTag extends Tag {
  /** How the parser builds this tag. */
  public static final TagClass CLASS = TablerowTag::new;

  private final String variable;
  private final Hash args;
  private final List<Template> templates = new ArrayList<>();
  private final Token collection;

  /** The tablerow tag, read from its token and the tokens that follow it. */
  public TablerowTag (TagToken tagToken, List<Token> remainTokens, Liquid liquid, Parser parser) {
    super(tagToken, remainTokens, liquid);
    IdentifierToken variable = this.tokenizer.readIdentifier();
    this.tokenizer.skipBlank();

    IdentifierToken predicate = this.tokenizer.readIdentifier();
    Token collectionToken = this.tokenizer.readValue();
    if (!"in".equals(predicate.content) || collectionToken == null) {
      throw new RuntimeException("illegal tag: " + tagToken.getText());
    }

    this.variable = variable.content;
    this.collection = collectionToken;
    this.args = new Hash(this.tokenizer, liquid.options.keyValueSeparator);

    List<List<Template>> current = new ArrayList<>();
    current.add(templates);
    ParseStream stream = parser.parseStream(remainTokens);
    stream
        .on("start", (Runnable) () -> current.set(0, templates))
        .on("tag:endtablerow", (Runnable) stream::stop)
        .onTemplate(tpl -> current.get(0).add(tpl))
        .on("end", (Runnable) () -> {
          throw new RuntimeException("tag " + tagToken.getText() + " not closed");
        })
        .start();
  }

  @Override
  public Object render (Context ctx, Emitter emitter) {
    List<Object> collection = Underscore.toEnumerable(Expression.evalToken(this.collection, ctx));
    Map<String, Object> args = this.args.render(ctx);
    Object offsetArg = args.get("offset");
    int offset = Js.truthy(offsetArg) ? (int) Js.toNumberValue(offsetArg) : 0;
    Object limitArg = args.get("limit");
    int limit = limitArg == null || Js.isUndefined(limitArg)
        ? collection.size()
        : (int) Js.toNumberValue(limitArg);

    int from = Math.min(Math.max(offset, 0), collection.size());
    int to = Math.min(Math.max(from + limit, from), collection.size());
    collection = new ArrayList<>(collection.subList(from, to));
    Object colsArg = args.get("cols");
    int cols = Js.truthy(colsArg) ? (int) Js.toNumberValue(colsArg) : collection.size();

    com.liquidjs.render.Render r = this.liquid.renderer;
    TablerowloopDrop tablerowloop = new TablerowloopDrop(collection.size(), cols,
        this.collection.getText(), this.variable);
    Map<String, Object> scope = Scope.of("tablerowloop", tablerowloop);
    ctx.push(scope);

    for (int idx = 0; idx < collection.size(); idx++, tablerowloop.next()) {
      scope.put(this.variable, collection.get(idx));
      if (tablerowloop.col0() == 0) {
        if (tablerowloop.row() != 1) emitter.write("</tr>");
        emitter.write("<tr class=\"row" + JsNumber.toString(tablerowloop.row()) + "\">");
      }
      emitter.write("<td class=\"col" + JsNumber.toString(tablerowloop.col()) + "\">");
      r.renderTemplates(this.templates, ctx, emitter);
      emitter.write("</td>");
    }
    if (!collection.isEmpty()) emitter.write("</tr>");
    ctx.pop();
    return null;
  }

  @Override
  public boolean hasChildren () {
    return true;
  }

  @Override
  public List<Template> children (boolean partials, boolean sync) {
    return templates;
  }

  @Override
  public List<Object> arguments () {
    List<Object> out = new ArrayList<>();
    out.add(collection);
    for (Token value : args.hash.values()) {
      if (value instanceof ValueToken) out.add(value);
    }
    return out;
  }

  @Override
  public List<String> blockScope () {
    return Arrays.asList(variable, "tablerowloop");
  }
}
