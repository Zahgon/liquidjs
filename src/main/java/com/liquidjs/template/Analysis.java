/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 Jun Yang
 */
package com.liquidjs.template;

import com.liquidjs.js.Js;
import com.liquidjs.parser.FilterArg;
import com.liquidjs.tokens.IdentifierToken;
import com.liquidjs.tokens.NumberToken;
import com.liquidjs.tokens.PropertyAccessToken;
import com.liquidjs.tokens.QuotedToken;
import com.liquidjs.tokens.RangeToken;
import com.liquidjs.tokens.FilteredValueToken;
import com.liquidjs.tokens.Token;
import com.liquidjs.tokens.ValueToken;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Reports which variables a template reads and which of them it is expected to be given. */
public final class Analysis {
  private Analysis () {}

  /** Variables grouped by the string their root segment reads as. */
  static final class VariableMap {
    private final Map<String, List<Variable>> map = new LinkedHashMap<>();

    List<Variable> get (Variable key) {
      String k = Variable.segmentsString(firstSegment(key));
      return map.computeIfAbsent(k, name -> new ArrayList<>());
    }

    boolean has (Variable key) {
      return map.containsKey(Variable.segmentsString(firstSegment(key)));
    }

    void push (Variable variable) {
      get(variable).add(variable);
    }

    Map<String, List<Variable>> asObject () {
      return map;
    }

    private static List<Object> firstSegment (Variable variable) {
      List<Object> segments = new ArrayList<>();
      if (!variable.segments.isEmpty()) segments.add(variable.segments.get(0));
      return segments;
    }
  }

  public static StaticAnalysis analyze (List<Template> templates, StaticAnalysisOptions options) {
    StaticAnalysisOptions opts = options == null ? new StaticAnalysisOptions() : options;
    return analyzeTemplates(templates, opts.partials, false);
  }

  public static StaticAnalysis analyzeSync (List<Template> templates,
                                            StaticAnalysisOptions options) {
    StaticAnalysisOptions opts = options == null ? new StaticAnalysisOptions() : options;
    return analyzeTemplates(templates, opts.partials, true);
  }

  private static StaticAnalysis analyzeTemplates (List<Template> templates, boolean partials,
                                                  boolean sync) {
    VariableMap variables = new VariableMap();
    VariableMap globals = new VariableMap();
    VariableMap locals = new VariableMap();
    DummyScope rootScope = new DummyScope(new LinkedHashSet<>());
    Set<String> seen = new LinkedHashSet<>();

    for (Template template : templates) {
      visit(template, rootScope, rootScope, variables, globals, locals, seen, partials, sync);
    }
    return new StaticAnalysis(variables.asObject(), globals.asObject(), locals.asObject());
  }

  private static void updateVariables (Variable variable, DummyScope scope, DummyScope rootScope,
                                       VariableMap variables, VariableMap globals) {
    variables.push(variable);
    Variable aliased = scope.alias(variable);

    if (aliased != null) {
      Object root = aliased.segments.isEmpty() ? null : aliased.segments.get(0);
      if (Js.isString(root) && !rootScope.has((String) root)) {
        globals.push(aliased);
      }
    } else {
      Object root = variable.segments.isEmpty() ? null : variable.segments.get(0);
      if (Js.isString(root) && !scope.has((String) root)) {
        globals.push(variable);
      }
    }

    for (Object segment : variable.segments) {
      if (segment instanceof Variable) {
        updateVariables((Variable) segment, scope, rootScope, variables, globals);
      }
    }
  }

  private static void visit (Template template, DummyScope scope, DummyScope rootScope,
                             VariableMap variables, VariableMap globals, VariableMap locals,
                             Set<String> seen, boolean partials, boolean sync) {
    List<Object> arguments = template.arguments();
    if (arguments != null) {
      for (Object arg : arguments) {
        for (Variable variable : extractVariables(arg)) {
          updateVariables(variable, scope, rootScope, variables, globals);
        }
      }
    }

    List<Token> localScope = template.localScope();
    if (localScope != null) {
      for (Token ident : localScope) {
        String content = Js.toStringValue(ident.getContent());
        scope.add(content);
        scope.deleteAlias(content);
        int[] position = ident.getPosition();
        List<Object> segments = new ArrayList<>();
        segments.add(content);
        locals.push(new Variable(segments,
            new VariableLocation(position[0], position[1], ident.file)));
      }
    }

    if (!template.hasChildren()) return;

    PartialScope partial = template.partialScope();
    if (template.hasPartialScope()) {
      if (partial == null) {
        // a layout, for one, has children that are not partials
        for (Template child : childrenOf(template, partials, sync)) {
          visit(child, scope, rootScope, variables, globals, locals, seen, partials, sync);
        }
        return;
      }

      if (seen.contains(partial.name)) return;

      Set<String> partialScopeNames = new LinkedHashSet<>();
      DummyScope partialScope = partial.isolated
          ? new DummyScope(partialScopeNames)
          : scope.push(partialScopeNames);

      for (Object name : partial.scope) {
        if (Js.isString(name)) {
          partialScopeNames.add((String) name);
        } else {
          Object[] pair = (Object[]) name;
          String alias = (String) pair[0];
          partialScopeNames.add(alias);
          List<Variable> extracted = extractVariables(pair[1]);
          if (!extracted.isEmpty()) {
            partialScope.setAlias(alias, extracted.get(0).segments);
          }
        }
      }

      for (Template child : childrenOf(template, partials, sync)) {
        visit(child, partialScope, rootScope, variables, globals, locals, seen, partials, sync);
        seen.add(partial.name);
      }

      partialScope.pop();
      return;
    }

    List<String> blockScope = template.blockScope();
    if (blockScope != null) scope.push(new LinkedHashSet<>(blockScope));

    for (Template child : childrenOf(template, partials, sync)) {
      visit(child, scope, rootScope, variables, globals, locals, seen, partials, sync);
    }

    if (blockScope != null) scope.pop();
  }

  private static List<Template> childrenOf (Template template, boolean partials, boolean sync) {
    List<Template> children = template.children(partials, sync);
    return children == null ? new ArrayList<>() : children;
  }

  /** A stack of scopes, used while walking a template. */
  static final class DummyScope {
    private static final class Item {
      final Set<String> names;
      final Map<String, List<Object>> aliases = new LinkedHashMap<>();

      Item (Set<String> names) {
        this.names = names;
      }
    }

    private final List<Item> stack = new ArrayList<>();

    DummyScope (Set<String> globals) {
      stack.add(new Item(globals));
    }

    /** True when a name is in scope. */
    boolean has (String name) {
      for (Item scope : stack) {
        if (scope.names.contains(name)) return true;
      }
      return false;
    }

    DummyScope push (Set<String> scope) {
      stack.add(new Item(scope));
      return this;
    }

    Set<String> pop () {
      return stack.isEmpty() ? null : stack.remove(stack.size() - 1).names;
    }

    void add (String name) {
      stack.get(0).names.add(name);
    }

    /** The variable a variable aliases, or null when it aliases nothing. */
    Variable alias (Variable variable) {
      Object root = variable.segments.isEmpty() ? null : variable.segments.get(0);
      if (!Js.isString(root)) return null;
      List<Object> alias = getAlias((String) root);
      if (alias == null) return null;
      List<Object> segments = new ArrayList<>(alias);
      segments.addAll(variable.segments.subList(1, variable.segments.size()));
      return new Variable(segments, variable.location);
    }

    void setAlias (String from, List<Object> to) {
      stack.get(stack.size() - 1).aliases.put(from, to);
    }

    void deleteAlias (String name) {
      stack.get(stack.size() - 1).aliases.remove(name);
    }

    private List<Object> getAlias (String name) {
      for (Item scope : stack) {
        if (scope.aliases.containsKey(name)) return scope.aliases.get(name);
        // a scope that defines the name masks an alias of a parent scope
        if (scope.names.contains(name)) return null;
      }
      return null;
    }
  }

  static List<Variable> extractVariables (Object value) {
    List<Variable> out = new ArrayList<>();
    if (value instanceof Value) {
      extractFilteredValueVariables((Value) value, out);
    } else if (value instanceof Token && value instanceof ValueToken) {
      extractValueTokenVariables((Token) value, out);
    }
    return out;
  }

  private static void extractFilteredValueVariables (Value value, List<Variable> out) {
    for (Token token : value.initial.postfix) {
      if (token instanceof ValueToken) extractValueTokenVariables(token, out);
    }
    for (Filter filter : value.filters) {
      for (FilterArg arg : filter.args) {
        if (arg.value instanceof ValueToken) extractValueTokenVariables(arg.value, out);
      }
    }
  }

  private static void extractValueTokenVariables (Token token, List<Variable> out) {
    if (token instanceof RangeToken) {
      extractValueTokenVariables(((RangeToken) token).lhs, out);
      extractValueTokenVariables(((RangeToken) token).rhs, out);
    } else if (token instanceof FilteredValueToken) {
      FilteredValueToken filtered = (FilteredValueToken) token;
      for (Token t : filtered.initial.postfix) {
        if (t instanceof ValueToken) extractValueTokenVariables(t, out);
      }
      for (com.liquidjs.tokens.FilterToken filter : filtered.filters) {
        for (FilterArg arg : filter.args) {
          if (arg.value instanceof ValueToken) extractValueTokenVariables(arg.value, out);
        }
      }
    } else if (token instanceof PropertyAccessToken) {
      out.add(extractPropertyAccessVariable((PropertyAccessToken) token));
    }
  }

  private static Variable extractPropertyAccessVariable (PropertyAccessToken token) {
    List<Object> segments = new ArrayList<>();
    String file = token.file;

    // the first segment of a nested path is flattened into this one
    Token root = token.props.get(0);
    if (file == null) file = root.file;
    if (root instanceof QuotedToken || root instanceof NumberToken
        || root instanceof IdentifierToken) {
      segments.add(root.getContent());
    } else if (root instanceof PropertyAccessToken) {
      segments.addAll(extractPropertyAccessVariable((PropertyAccessToken) root).segments);
    }

    for (int i = 1; i < token.props.size(); i++) {
      Token prop = token.props.get(i);
      if (file == null) file = prop.file;
      if (prop instanceof QuotedToken || prop instanceof NumberToken
          || prop instanceof IdentifierToken) {
        segments.add(prop.getContent());
      } else if (prop instanceof PropertyAccessToken) {
        segments.add(extractPropertyAccessVariable((PropertyAccessToken) prop));
      }
    }

    int[] position = token.getPosition();
    return new Variable(segments, new VariableLocation(position[0], position[1], file));
  }
}
