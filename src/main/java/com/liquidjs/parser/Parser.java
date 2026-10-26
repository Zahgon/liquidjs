/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 Jun Yang
 */
package com.liquidjs.parser;

import com.liquidjs.Liquid;
import com.liquidjs.cache.LiquidCache;
import com.liquidjs.fs.FS;
import com.liquidjs.fs.Loader;
import com.liquidjs.fs.LookupType;
import com.liquidjs.js.Js;
import com.liquidjs.template.Html;
import com.liquidjs.template.Output;
import com.liquidjs.template.TagClass;
import com.liquidjs.template.Template;
import com.liquidjs.tokens.OutputToken;
import com.liquidjs.tokens.TagToken;
import com.liquidjs.tokens.Token;
import com.liquidjs.util.Assertions;
import com.liquidjs.util.Limiter;
import com.liquidjs.util.LiquidError;
import com.liquidjs.util.LiquidErrors;
import com.liquidjs.util.ParseError;

import java.util.ArrayList;
import java.util.List;

/** Builds templates out of the tokens a tokenizer read. */
public class Parser {
  private final Liquid liquid;
  private final FS fs;
  private final LiquidCache cache;
  private final Loader loader;
  private final Limiter parseLimit;

  /** A parser for one engine, with its options, its loader and its cache. */
  public Parser (Liquid liquid) {
    this.liquid = liquid;
    this.cache = liquid.options.cache;
    this.fs = liquid.options.fs;
    this.loader = new Loader(liquid.options);
    this.parseLimit = new Limiter("parse length", liquid.options.parseLimit);
  }

  /** Parse a template into the nodes that render it. */
  public List<Template> parse (String html) {
    return parse(html, null);
  }

  /** Parse a template that came from a file, so its errors name that file. */
  public List<Template> parse (String html, String filepath) {
    String text = html == null ? "null" : html;
    parseLimit.use(text.length());
    Tokenizer tokenizer = new Tokenizer(text, liquid.options.operators, filepath, null,
        liquid.options.groupedExpressions);
    List<Token> tokens = tokenizer.readTopLevelTokens(liquid.options);
    return parseTokens(tokens);
  }

  /** Parse tokens into nodes, collecting errors when the options ask for it. */
  public List<Template> parseTokens (List<Token> tokens) {
    List<Template> templates = new ArrayList<>();
    List<LiquidError> errors = new ArrayList<>();
    while (!tokens.isEmpty()) {
      Token token = tokens.remove(0);
      try {
        templates.add(parseToken(token, tokens));
      } catch (RuntimeException err) {
        if (liquid.options.catchAllErrors && err instanceof LiquidError) {
          errors.add((LiquidError) err);
        } else {
          throw err;
        }
      }
    }
    if (!errors.isEmpty()) throw new LiquidErrors(errors);
    return templates;
  }

  /** Parse one token into a node, wrapping any error with its position. */
  public Template parseToken (Token token, List<Token> remainTokens) {
    try {
      if (token.kind == com.liquidjs.parser.TokenKind.Tag) {
        TagToken tagToken = (TagToken) token;
        TagClass tagClass = liquid.tags.get(tagToken.name);
        Assertions.assertTrue(tagClass != null, "tag \"" + tagToken.name + "\" not found");
        return tagClass.create(tagToken, remainTokens, liquid, this);
      }
      if (token.kind == com.liquidjs.parser.TokenKind.Output) {
        return new Output((OutputToken) token, liquid);
      }
      return new Html((com.liquidjs.tokens.HTMLToken) token);
    } catch (RuntimeException e) {
      if (LiquidError.is(e)) throw e;
      throw new ParseError(e, token);
    }
  }

  /** A stream a tag drives to parse the tokens that belong to it. */
  public ParseStream parseStream (List<Token> tokens) {
    return new ParseStream(tokens, (token, remaining) -> parseToken(token, remaining));
  }

  /** Parse a template file, through the cache when there is one. */
  public List<Template> parseFile (String file, LookupType type, String currentFile) {
    if (cache != null) return parseFileCached(file, type, currentFile);
    return parseFileUncached(file, type, currentFile);
  }

  private List<Template> parseFileCached (String file, LookupType type, String currentFile) {
    String key = loader.shouldLoadRelative(file)
        ? currentFile + "," + file
        : type.key() + ":" + file;
    List<Template> tpls = cache.read(key);
    if (tpls != null) return tpls;
    try {
      List<Template> parsed = parseFileUncached(file, type, currentFile);
      cache.write(key, parsed);
      return parsed;
    } catch (RuntimeException err) {
      // the cache keeps no entry for a template that failed to parse
      cache.remove(key);
      throw err;
    }
  }

  private List<Template> parseFileUncached (String file, LookupType type, String currentFile) {
    String filepath = loader.lookup(file, type, currentFile);
    return parse(readFile(filepath), filepath);
  }

  private String readFile (String filepath) {
    String content = fs.readFileSync(filepath);
    return Js.isNil(content) ? "" : content;
  }
}
