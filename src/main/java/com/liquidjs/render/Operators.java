/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 Jun Yang
 */
package com.liquidjs.render;

import com.liquidjs.context.Context;
import com.liquidjs.drop.Comparable;
import com.liquidjs.js.Js;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** The operators a conditional expression may use, and the equality they are built on. */
public final class Operators {
  private Operators () {}

  /** An operator over a left and a right operand. */
  public interface Binary {
    /** Apply the operator to both operands. */
    boolean apply (Object lhs, Object rhs, Context ctx);
  }

  /** An operator over one operand. */
  public interface Unary {
    /** Apply the operator to its one operand. */
    boolean apply (Object operand, Context ctx);
  }

  /** The regular Liquid operators. */
  public static final Map<String, Object> DEFAULT_OPERATORS = defaultOperators();

  private static Map<String, Object> defaultOperators () {
    Map<String, Object> operators = new LinkedHashMap<>();
    operators.put("==", (Binary) (l, r, ctx) -> equals(l, r));
    operators.put("!=", (Binary) (l, r, ctx) -> !equals(l, r));
    operators.put(">", (Binary) (l, r, ctx) -> {
      if (l instanceof Comparable) return ((Comparable) l).gt(r);
      if (r instanceof Comparable) return ((Comparable) r).lt(l);
      return Js.greaterThan(Js.toValue(l), Js.toValue(r));
    });
    operators.put("<", (Binary) (l, r, ctx) -> {
      if (l instanceof Comparable) return ((Comparable) l).lt(r);
      if (r instanceof Comparable) return ((Comparable) r).gt(l);
      return Js.lessThan(Js.toValue(l), Js.toValue(r));
    });
    operators.put(">=", (Binary) (l, r, ctx) -> {
      if (l instanceof Comparable) return ((Comparable) l).geq(r);
      if (r instanceof Comparable) return ((Comparable) r).leq(l);
      return Js.greaterOrEqual(Js.toValue(l), Js.toValue(r));
    });
    operators.put("<=", (Binary) (l, r, ctx) -> {
      if (l instanceof Comparable) return ((Comparable) l).leq(r);
      if (r instanceof Comparable) return ((Comparable) r).geq(l);
      return Js.lessOrEqual(Js.toValue(l), Js.toValue(r));
    });
    operators.put("contains", (Binary) (lhs, r, ctx) -> {
      Object l = Js.toValue(lhs);
      if (Js.isArray(l)) return arrayIncludes(Js.asArray(l), r);
      if (Js.isString(l)) return ((String) l).contains(Js.toStringValue(Js.toValue(r)));
      return false;
    });
    operators.put("not", (Unary) (v, ctx) -> Booleans.isFalsy(Js.toValue(v), ctx));
    operators.put("and", (Binary) (l, r, ctx) ->
        Booleans.isTruthy(Js.toValue(l), ctx) && Booleans.isTruthy(Js.toValue(r), ctx));
    operators.put("or", (Binary) (l, r, ctx) ->
        Booleans.isTruthy(Js.toValue(l), ctx) || Booleans.isTruthy(Js.toValue(r), ctx));
    return operators;
  }

  /** Equality as the == operator defines it: a comparable value decides for itself. */
  public static boolean equals (Object lhs, Object rhs) {
    if (lhs instanceof Comparable) return ((Comparable) lhs).equalsValue(rhs);
    if (rhs instanceof Comparable) return ((Comparable) rhs).equalsValue(lhs);
    Object left = Js.toValue(lhs);
    Object right = Js.toValue(rhs);
    if (Js.isArray(left)) {
      return Js.isArray(right) && arrayEquals(Js.asArray(left), Js.asArray(right));
    }
    return Js.strictEquals(left, right);
  }

  private static boolean arrayEquals (List<Object> lhs, List<Object> rhs) {
    if (lhs.size() != rhs.size()) return false;
    for (int i = 0; i < lhs.size(); i++) {
      if (!equals(lhs.get(i), rhs.get(i))) return false;
    }
    return true;
  }

  /** Whether an array holds a value, compared as the equality operator compares. */
  public static boolean arrayIncludes (List<Object> array, Object item) {
    for (Object value : array) {
      if (equals(value, item)) return true;
    }
    return false;
  }
}
