/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 Jun Yang
 */
package com.liquidjs.parser;

import com.liquidjs.template.Template;
import com.liquidjs.tokens.TagToken;
import com.liquidjs.tokens.Token;
import com.liquidjs.parser.TokenKind;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Consumer;

/** Drives a tag's own parsing: it hands out tokens until the tag says it has had enough. */
public class ParseStream {
  /** What a stream does with one token or one template. */
  public interface Handler {
    void accept (Object arg);
  }

  private final List<Token> tokens;
  private final Map<String, Handler> handlers = new HashMap<>();
  private boolean stopRequested = false;
  private final BiFunction<Token, List<Token>, Template> parseToken;

  public ParseStream (List<Token> tokens, BiFunction<Token, List<Token>, Template> parseToken) {
    this.tokens = tokens;
    this.parseToken = parseToken;
  }

  public ParseStream on (String name, Handler cb) {
    handlers.put(name, cb);
    return this;
  }

  public ParseStream on (String name, Runnable cb) {
    handlers.put(name, arg -> cb.run());
    return this;
  }

  public ParseStream onTag (String name, Consumer<TagToken> cb) {
    handlers.put(name, arg -> cb.accept((TagToken) arg));
    return this;
  }

  public ParseStream onTemplate (Consumer<Template> cb) {
    handlers.put("template", arg -> cb.accept((Template) arg));
    return this;
  }

  private boolean trigger (String event, Object arg) {
    Handler handler = handlers.get(event);
    if (handler == null) return false;
    handler.accept(arg);
    return true;
  }

  public ParseStream start () {
    trigger("start", null);
    while (!stopRequested && !tokens.isEmpty()) {
      Token token = tokens.remove(0);
      if (trigger("token", token)) continue;
      if (token.kind == TokenKind.Tag && trigger("tag:" + ((TagToken) token).name, token)) {
        continue;
      }
      Template template = parseToken.apply(token, tokens);
      trigger("template", template);
    }
    if (!stopRequested) trigger("end", null);
    return this;
  }

  public ParseStream stop () {
    this.stopRequested = true;
    return this;
  }
}
