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
import java.util.function.BiConsumer;

/** Renders this template inside another one, filling that one's blocks. */
public class LayoutTag extends Tag {
  /** How the parser builds this tag. */
  public static final TagClass CLASS = LayoutTag::new;

  private final Hash args;
  private final List<Template> templates;
  private final Object file;
  private final String currentFile;

  /** The layout tag, read from its token and the tokens that follow it. */
  public LayoutTag (TagToken token, List<Token> remainTokens, Liquid liquid, Parser parser) {
    super(token, remainTokens, liquid);
    this.file = FilePath.parseFilePath(this.tokenizer, this.liquid, parser);
    this.currentFile = token.file;
    this.args = new Hash(this.tokenizer, liquid.options.keyValueSeparator);
    this.templates = parser.parseTokens(remainTokens);
  }

  @Override
  public Object render (Context ctx, Emitter emitter) {
    com.liquidjs.render.Render renderer = this.liquid.renderer;
    if (this.file == null) {
      ctx.setRegister("blockMode", BlockMode.OUTPUT);
      renderer.renderTemplates(this.templates, ctx, emitter);
      return null;
    }
    Object filepathValue = FilePath.renderFilePath(this.file, ctx, this.liquid);
    String filepath = Js.isNil(filepathValue) ? null : Js.toStringValue(filepathValue);
    Assertions.assertTruthy(filepath,
        "illegal file path \"" + Js.toStringValue(filepathValue) + "\"");
    List<Template> templates = this.liquid.parseLayoutFile(filepath, ctx.sync, this.currentFile);

    // render what is left of this template and keep the output
    ctx.setRegister("blockMode", BlockMode.STORE);
    Object html = renderer.renderTemplates(this.templates, ctx);
    Map<String, Object> blocks = ctx.getRegister("blocks", Js.newObject());

    // the whole content becomes the anonymous block when none was named
    if (blocks.get("") == null) {
      BiConsumer<Object, Emitter> anonymous = (parent, into) -> into.write(html);
      blocks.put("", anonymous);
    }
    ctx.setRegister("blockMode", BlockMode.OUTPUT);

    // render the layout, with the blocks that were kept
    ctx.push(Scope.createScope(this.args.render(ctx)));
    renderer.renderTemplates(templates, ctx, emitter);
    ctx.pop();
    return null;
  }

  @Override
  public boolean hasChildren () {
    return true;
  }

  @Override
  public List<Template> children (boolean partials, boolean sync) {
    List<Template> out = new ArrayList<>(this.templates);
    if (partials && Js.isString(this.file)) {
      out.addAll(this.liquid.parsePartialFile((String) this.file, true, this.currentFile));
    }
    return out;
  }

  @Override
  public List<Object> arguments () {
    List<Object> out = new ArrayList<>();
    for (Token value : this.args.hash.values()) {
      if (value instanceof ValueToken) out.add(value);
    }
    if (this.file instanceof ValueToken) out.add(this.file);
    return out;
  }

  @Override
  public boolean hasPartialScope () {
    return true;
  }

  @Override
  public PartialScope partialScope () {
    if (!Js.isString(this.file)) return null;
    return new PartialScope((String) this.file, false, new ArrayList<>(this.args.hash.keySet()));
  }
}
