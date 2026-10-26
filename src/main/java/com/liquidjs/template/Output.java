/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 Jun Yang
 */
package com.liquidjs.template;

import com.liquidjs.Liquid;
import com.liquidjs.context.Context;
import com.liquidjs.emitters.Emitter;
import com.liquidjs.js.Js;
import com.liquidjs.parser.Tokenizer;
import com.liquidjs.tokens.FilterToken;
import com.liquidjs.tokens.OutputToken;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

/** An output: a value expression written between the output delimiters. */
public class Output extends TemplateImpl<OutputToken> {
  /** The value expression the output writes. */
  public final Value value;

  /** An output node, with the output escape of the options applied last. */
  public Output (OutputToken token, Liquid liquid) {
    super(token);
    Tokenizer tokenizer = new Tokenizer(token.input, liquid.options.operators, token.file,
        token.contentRange, liquid.options.groupedExpressions);
    this.value = new Value(tokenizer.readFilteredValue(), liquid);
    List<Filter> filters = this.value.filters;
    Function<Object, String> outputEscape = liquid.options.outputEscape;
    boolean lastIsRaw = !filters.isEmpty() && filters.get(filters.size() - 1).raw;
    if (!lastIsRaw && outputEscape != null) {
      FilterToken escapeToken = new FilterToken("[object Function]", new ArrayList<>(), "", 0, 0, null);
      FilterHandler handler = (impl, input, args) -> outputEscape.apply(input);
      filters.add(new Filter(escapeToken, handler, liquid));
    }
  }

  @Override
  public Object render (Context ctx, Emitter emitter) {
    Object val = Js.toValue(value.value(ctx, false));
    emitter.write(val);
    return null;
  }

  @Override
  public List<Object> arguments () {
    return Collections.singletonList(value);
  }
}
