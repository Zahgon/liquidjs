/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 Jun Yang
 */
package com.liquidjs.render;

import com.liquidjs.context.Context;
import com.liquidjs.emitters.Emitter;
import com.liquidjs.emitters.KeepingTypeEmitter;
import com.liquidjs.emitters.SimpleEmitter;
import com.liquidjs.emitters.StreamedEmitter;
import com.liquidjs.js.Js;
import com.liquidjs.template.Template;
import com.liquidjs.util.LiquidError;
import com.liquidjs.util.LiquidErrors;
import com.liquidjs.util.RenderError;

import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

/** Renders a list of templates into an emitter. */
public class Render {
  /** Render into a stream, so output is readable before the render finishes. */
  public Reader renderTemplatesToNodeStream (List<Template> templates, Context ctx) {
    StreamedEmitter emitter = new StreamedEmitter();
    Reader stream = emitter.getStream();
    Thread worker = new Thread(() -> {
      try {
        renderTemplates(templates, ctx, emitter);
        emitter.end();
      } catch (RuntimeException err) {
        emitter.error(err);
      }
    });
    worker.setDaemon(true);
    worker.start();
    return stream;
  }

  public Object renderTemplates (List<Template> templates, Context ctx) {
    return renderTemplates(templates, ctx, null);
  }

  public Object renderTemplates (List<Template> templates, Context ctx, Emitter into) {
    Emitter emitter = into;
    if (emitter == null) {
      emitter = ctx.opts.keepOutputType ? new KeepingTypeEmitter() : new SimpleEmitter();
    }
    ctx.renderLimit.check(Context.now());
    List<LiquidError> errors = new ArrayList<>();
    for (Template tpl : templates) {
      ctx.renderLimit.check(Context.now());
      try {
        // a template that writes into the emitter returns nothing to write
        Object html = tpl.render(ctx, emitter);
        if (Js.truthy(html)) emitter.write(html);
        if (ctx.breakCalled || ctx.continueCalled) break;
      } catch (RuntimeException e) {
        LiquidError err = LiquidError.is(e) ? (LiquidError) e : new RenderError(e, tpl);
        if (ctx.opts.catchAllErrors) errors.add(err);
        else throw err;
      }
    }
    if (!errors.isEmpty()) throw new LiquidErrors(errors);
    return emitter.getBuffer();
  }
}
