/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 Jun Yang
 */
package com.liquidjs.tags;

import com.liquidjs.Liquid;
import com.liquidjs.context.Context;
import com.liquidjs.js.Js;
import com.liquidjs.parser.Parser;
import com.liquidjs.parser.Tokenizer;
import com.liquidjs.render.Expression;
import com.liquidjs.template.Html;
import com.liquidjs.template.Template;
import com.liquidjs.tokens.QuotedToken;
import com.liquidjs.tokens.Token;

import java.util.ArrayList;
import java.util.List;

/**
 * The file name of a partial or a layout, which may be a literal, an expression, or a template of
 * its own when it holds outputs.
 */
public final class FilePath {
  private FilePath () {}

  /**
   * Reads a file name.
   *
   * @return null for "none", a list of templates for a quoted name with tags or filters in it,
   *     a token for an expression, or a string for a plain name.
   */
  public static Object parseFilePath (Tokenizer tokenizer, Liquid liquid, Parser parser) {
    if (liquid.options.dynamicPartials) {
      Token file = tokenizer.readValue();
      tokenizer.assertTrue(file != null, "illegal file path");
      if ("none".equals(file.getText())) return null;
      if (file instanceof QuotedToken) {
        // a name such as "files/{{file}}" is read as a template of its own
        List<Template> templates = parser.parse(
            Js.toStringValue(Expression.evalQuotedToken((QuotedToken) file)));
        return optimize(templates);
      }
      return file;
    }
    List<Token> tokens = new ArrayList<>(tokenizer.readFileNameTemplate(liquid.options));
    Object templates = optimize(parser.parseTokens(tokens));
    return "none".equals(templates) ? null : templates;
  }

  private static Object optimize (List<Template> templates) {
    // a name such as "files/file.liquid" is taken as the string it is
    if (templates.size() == 1 && templates.get(0) instanceof Html) {
      return ((Html) templates.get(0)).token.getContentText();
    }
    return templates;
  }

  @SuppressWarnings("unchecked")
  public static Object renderFilePath (Object file, Context ctx, Liquid liquid) {
    if (Js.isString(file)) return file;
    if (file instanceof List) {
      return liquid.renderer.renderTemplates((List<Template>) file, ctx);
    }
    return Expression.evalToken((Token) file, ctx);
  }
}
