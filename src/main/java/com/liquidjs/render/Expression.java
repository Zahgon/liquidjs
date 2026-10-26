/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 Jun Yang
 */
package com.liquidjs.render;

import com.liquidjs.context.Context;
import com.liquidjs.js.Js;
import com.liquidjs.template.Filter;
import com.liquidjs.tokens.FilterToken;
import com.liquidjs.tokens.FilteredValueToken;
import com.liquidjs.tokens.OperatorToken;
import com.liquidjs.tokens.PropertyAccessToken;
import com.liquidjs.tokens.RangeToken;
import com.liquidjs.tokens.Token;
import com.liquidjs.util.Assertions;
import com.liquidjs.util.InternalUndefinedVariableError;
import com.liquidjs.util.UndefinedVariableError;
import com.liquidjs.util.Underscore;

import java.util.ArrayList;
import java.util.List;

/** A conditional or value expression, kept in postfix order so evaluation is one pass. */
public class Expression {
  public final List<Token> postfix;

  /** An expression, kept in postfix order so evaluation is one pass. */
  public Expression (List<Token> tokens) {
    this.postfix = toPostfix(tokens);
  }

  /** Evaluate the expression. */
  public Object evaluate (Context ctx) {
    return evaluate(ctx, false);
  }

  /** Evaluate the expression, optionally letting an absent variable be null. */
  public Object evaluate (Context ctx, boolean lenient) {
    Assertions.assertTrue(ctx != null, "unable to evaluate: context not defined");
    List<Object> operands = new ArrayList<>();
    for (Token token : postfix) {
      if (token instanceof OperatorToken) {
        OperatorToken operator = (OperatorToken) token;
        Object r = operands.remove(operands.size() - 1);
        Object handler = ctx.opts.operators.get(operator.operator);
        Assertions.assertTrue(handler != null, () -> "undefined operator: " + operator.operator);
        Object result;
        Integer type = OperatorToken.TYPES.get(operator.operator);
        if (type != null && type == OperatorToken.UNARY) {
          result = ((Operators.Unary) handler).apply(r, ctx);
        } else {
          Object l = operands.isEmpty() ? Js.UNDEFINED : operands.remove(operands.size() - 1);
          result = ((Operators.Binary) handler).apply(l, r, ctx);
        }
        operands.add(result);
      } else {
        operands.add(evalToken(token, ctx, lenient));
      }
    }
    return operands.isEmpty() ? Js.UNDEFINED : operands.get(0);
  }

  /** Whether the expression holds anything to evaluate. */
  public boolean valid () {
    return !postfix.isEmpty();
  }

  /**
   * Evaluate a token, keeping drops as drops so a comparable drop such as empty, nil or blank is
   * compared as itself rather than as the value it stands for.
   */
  public static Object evalToken (Token token, Context ctx) {
    return evalToken(token, ctx, false);
  }

  public static Object evalToken (Token token, Context ctx, boolean lenient) {
    if (token == null) return Js.UNDEFINED;
    if (token.hasContent()) return token.getContent();
    if (token instanceof PropertyAccessToken) {
      return evalPropertyAccessToken((PropertyAccessToken) token, ctx, lenient);
    }
    if (token instanceof RangeToken) return evalRangeToken((RangeToken) token, ctx);
    if (token instanceof FilteredValueToken) {
      return evalFilteredValueToken((FilteredValueToken) token, ctx, lenient);
    }
    return Js.UNDEFINED;
  }

  /** Evaluate a token to the value filters and tags consume, unwrapping any drop. */
  public static Object evalTokenValue (Token token, Context ctx) {
    return evalTokenValue(token, ctx, false);
  }

  public static Object evalTokenValue (Token token, Context ctx, boolean lenient) {
    return Js.toValue(evalToken(token, ctx, lenient));
  }

  private static Object evalFilteredValueToken (FilteredValueToken token, Context ctx,
                                                boolean lenient) {
    Assertions.assertTrue(ctx.liquid != null,
        "FilteredValueToken evaluation requires liquid instance in context");
    boolean lenientValue = lenient || (ctx.opts.lenientIf && !token.filters.isEmpty()
        && "default".equals(token.filters.get(0).name));
    Object val = token.initial.evaluate(ctx, lenientValue);

    for (FilterToken filterToken : token.filters) {
      Object filterImpl = ctx.liquid.filters.get(filterToken.name);
      Assertions.assertTrue(filterImpl != null || !ctx.liquid.options.strictFilters,
          () -> "undefined filter: " + filterToken.name);
      Filter filter = new Filter(filterToken, filterImpl, ctx.liquid);
      val = filter.render(val, ctx);
    }
    return val;
  }

  private static Object evalPropertyAccessToken (PropertyAccessToken token, Context ctx,
                                                 boolean lenient) {
    List<Object> props = new ArrayList<>();
    for (Token prop : token.props) {
      props.add(evalTokenValue(prop, ctx, false));
    }
    try {
      if (token.variable != null) {
        Object variable = evalToken(token.variable, ctx, lenient);
        return ctx.getFromScope(variable, props);
      }
      return ctx.get(props);
    } catch (RuntimeException err) {
      if (lenient && err instanceof InternalUndefinedVariableError) return null;
      throw new UndefinedVariableError(err, token);
    }
  }

  /** The text a quoted token stands for. */
  public static Object evalQuotedToken (com.liquidjs.tokens.QuotedToken token) {
    return token.content;
  }

  private static Object evalRangeToken (RangeToken token, Context ctx) {
    double low = Js.toNumberValue(evalTokenValue(token.lhs, ctx));
    double high = Js.toNumberValue(evalTokenValue(token.rhs, ctx));
    ctx.memoryLimit.use(high - low + 1);
    return Underscore.range((int) low, (int) high + 1);
  }

  private static List<Token> toPostfix (List<Token> tokens) {
    List<Token> out = new ArrayList<>();
    List<OperatorToken> ops = new ArrayList<>();
    for (Token token : tokens) {
      if (token instanceof OperatorToken) {
        OperatorToken operator = (OperatorToken) token;
        while (!ops.isEmpty()
            && ops.get(ops.size() - 1).getPrecedence() > operator.getPrecedence()) {
          out.add(ops.remove(ops.size() - 1));
        }
        ops.add(operator);
      } else {
        out.add(token);
      }
    }
    while (!ops.isEmpty()) out.add(ops.remove(ops.size() - 1));
    return out;
  }
}
