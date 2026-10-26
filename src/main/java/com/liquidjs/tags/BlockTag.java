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
import com.liquidjs.drop.BlockDrop;
import com.liquidjs.emitters.Emitter;
import com.liquidjs.js.Js;
import com.liquidjs.parser.Parser;
import com.liquidjs.template.Tag;
import com.liquidjs.template.TagClass;
import com.liquidjs.template.Template;
import com.liquidjs.tokens.TagToken;
import com.liquidjs.tokens.Token;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** A named part of a layout, which a child template may fill. */
public class BlockTag extends Tag {
  /** How the parser builds this tag. */
  public static final TagClass CLASS = BlockTag::new;

  private static final Pattern NAME = Pattern.compile("\\w+");

  /** The name of the block, or an empty string for the anonymous one. */
  public final String block;
  /** The nodes of the block's body. */
  public final List<Template> templates = new ArrayList<>();

  /** The block tag, read from its token and the tokens that follow it. */
  public BlockTag (TagToken token, List<Token> remainTokens, Liquid liquid, Parser parser) {
    super(token, remainTokens, liquid);
    Matcher match = NAME.matcher(token.getArgs());
    this.block = match.find() ? match.group() : "";
    while (!remainTokens.isEmpty()) {
      Token next = remainTokens.remove(0);
      if (next.kind == com.liquidjs.parser.TokenKind.Tag && "endblock".equals(((TagToken) next).name)) return;
      this.templates.add(parser.parseToken(next, remainTokens));
    }
    throw new RuntimeException("tag " + token.getText() + " not closed");
  }

  @Override
  public Object render (Context ctx, Emitter emitter) {
    BiConsumer<Object, Emitter> blockRender = getBlockRender(ctx);
    if (ctx.getRegister("blockMode") == BlockMode.STORE) {
      Map<String, Object> blocks = ctx.getRegister("blocks", Js.newObject());
      blocks.put(this.block, blockRender);
    } else {
      blockRender.accept(new BlockDrop(), emitter);
    }
    return null;
  }

  @SuppressWarnings("unchecked")
  private BiConsumer<Object, Emitter> getBlockRender (Context ctx) {
    BlockTag self = this;
    Liquid liquid = this.liquid;
    List<Template> templates = this.templates;
    Map<String, Object> blocks = ctx.getRegister("blocks", Js.newObject());
    Object child = blocks.get(this.block);
    BiConsumer<Object, Emitter> renderCurrent = (superBlock, emitter) -> {
      List<Tag> stack = ctx.getRegister("blockStack", new ArrayList<>());
      if (stack.contains(self)) throw new RuntimeException("block tag cannot be nested");

      stack.add(self);
      ctx.push(Scope.of("block", superBlock));
      liquid.renderer.renderTemplates(templates, ctx, emitter);
      ctx.pop();
      stack.remove(stack.size() - 1);
    };
    if (child == null) return renderCurrent;
    BiConsumer<Object, Emitter> renderChild = (BiConsumer<Object, Emitter>) child;
    return (superBlock, emitter) -> renderChild.accept(
        new BlockDrop(into -> renderCurrent.accept(superBlock, into)), emitter);
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
  public List<String> blockScope () {
    return Collections.singletonList("block");
  }
}
