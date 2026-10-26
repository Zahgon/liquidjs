/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 Jun Yang
 */
package com.liquidjs.parser;

import com.liquidjs.LiquidOptions;
import com.liquidjs.js.JsJson;
import com.liquidjs.render.Expression;
import com.liquidjs.tokens.FilterToken;
import com.liquidjs.tokens.FilteredValueToken;
import com.liquidjs.tokens.HTMLToken;
import com.liquidjs.tokens.HashToken;
import com.liquidjs.tokens.IdentifierToken;
import com.liquidjs.tokens.LiquidTagToken;
import com.liquidjs.tokens.LiteralToken;
import com.liquidjs.tokens.NumberToken;
import com.liquidjs.tokens.OperatorToken;
import com.liquidjs.tokens.OutputToken;
import com.liquidjs.tokens.PropertyAccessToken;
import com.liquidjs.tokens.QuotedToken;
import com.liquidjs.tokens.RangeToken;
import com.liquidjs.tokens.TagToken;
import com.liquidjs.tokens.Token;
import com.liquidjs.util.Characters;
import com.liquidjs.util.Literals;
import com.liquidjs.util.OperatorTrie;
import com.liquidjs.util.TokenizationError;
import com.liquidjs.util.Underscore;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/** Reads a template, or a range of one, into tokens. */
public class Tokenizer {
  /** The position the next read starts at. */
  public int p;
  /** The position reading stops at. */
  public int N;
  public boolean groupedExpressions;
  public final String input;
  public final String file;

  private int rawBeginAt = -1;
  private final OperatorTrie.Node<Object> opTrie;
  private final OperatorTrie.Node<Object> literalTrie;

  public Tokenizer (String input) {
    this(input, LiquidOptions.defaults().operators, null, null, false);
  }

  public Tokenizer (String input, java.util.Map<String, Object> operators) {
    this(input, operators, null, null, false);
  }

  public Tokenizer (String input, java.util.Map<String, Object> operators, String file,
                    int[] range, boolean groupedExpressions) {
    this.input = input;
    this.file = file;
    this.p = range != null ? range[0] : 0;
    this.N = range != null ? range[1] : input.length();
    this.opTrie = OperatorTrie.create(operators == null
        ? LiquidOptions.defaults().operators : operators);
    this.literalTrie = OperatorTrie.create(Literals.VALUES);
    this.groupedExpressions = groupedExpressions;
  }

  public Expression readExpression () {
    return new Expression(readExpressionTokens());
  }

  public List<Token> readExpressionTokens () {
    List<Token> tokens = new ArrayList<>();
    while (this.p < this.N) {
      OperatorToken operator = readOperator();
      if (operator != null) {
        tokens.add(operator);
        continue;
      }
      Token operand = readValue();
      if (operand != null) {
        tokens.add(operand);
        continue;
      }
      return tokens;
    }
    return tokens;
  }

  public OperatorToken readOperator () {
    skipBlank();
    int end = matchTrie(opTrie);
    if (end == -1) return null;
    int begin = this.p;
    this.p = end;
    return new OperatorToken(input, begin, end, file);
  }

  public int matchTrie (OperatorTrie.Node<Object> trie) {
    OperatorTrie.Node<Object> node = trie;
    int i = this.p;
    OperatorTrie.Node<Object> info = null;
    while (i < this.N && node.children.containsKey(input.charAt(i))) {
      node = node.children.get(input.charAt(i++));
      if (node.end) info = node;
    }
    if (info == null) return -1;
    if (info.needBoundary && Characters.isWord(peek(i - this.p))) return -1;
    return i;
  }

  public FilteredValueToken readFilteredValue () {
    int begin = this.p;
    Expression initial = readExpression();
    assertTrue(initial.valid(), () -> "invalid value expression: " + snapshot());
    List<FilterToken> filters = readFilters();
    return new FilteredValueToken(initial, filters, input, begin, this.p, file);
  }

  public List<FilterToken> readFilters () {
    List<FilterToken> filters = new ArrayList<>();
    while (true) {
      FilterToken filter = readFilter();
      if (filter == null) return filters;
      filters.add(filter);
    }
  }

  public FilterToken readFilter () {
    skipBlank();
    if (end()) return null;
    if (")".equals(peek())) return null;
    assertTrue("|".equals(read()), "expected \"|\" before filter");
    IdentifierToken name = readIdentifier();
    if (name.size() == 0) {
      assertTrue(end(), "expected filter name");
      return null;
    }
    List<FilterArg> args = new ArrayList<>();
    skipBlank();
    if (":".equals(peek())) {
      do {
        ++this.p;
        FilterArg arg = readFilterArg();
        if (arg != null) args.add(arg);
        skipBlank();
        assertTrue(end() || ",".equals(peek()) || "|".equals(peek()) || ")".equals(peek()),
            () -> "unexpected character " + snapshot());
      } while (",".equals(peek()));
    } else if ("|".equals(peek()) || ")".equals(peek()) || end()) {
      // nothing follows the filter name
    } else {
      throw error("expected \":\" after filter name");
    }
    return new FilterToken(name.getText(), args, input, name.begin, this.p, file);
  }

  public FilterArg readFilterArg () {
    Token key = readValue();
    if (key == null) return null;
    skipBlank();
    if (!":".equals(peek())) return FilterArg.of(key);
    ++this.p;
    Token value = readValue();
    return FilterArg.of(key.getText(), value);
  }

  public List<Token> readTopLevelTokens () {
    return readTopLevelTokens(LiquidOptions.defaults());
  }

  public List<Token> readTopLevelTokens (LiquidOptions options) {
    List<Token> tokens = new ArrayList<>();
    while (this.p < this.N) {
      tokens.add(readTopLevelToken(options));
    }
    WhitespaceCtrl.whiteSpaceCtrl(tokens, options);
    return tokens;
  }

  public Token readTopLevelToken (LiquidOptions options) {
    String tagDelimiterLeft = options.tagDelimiterLeft;
    String outputDelimiterLeft = options.outputDelimiterLeft;
    if (rawBeginAt > -1) return readEndrawOrRawContent(options);
    if (match(tagDelimiterLeft)) return readTagToken(options);
    if (match(outputDelimiterLeft)) return readOutputToken(options);
    return readHTMLToken(new String[] { tagDelimiterLeft, outputDelimiterLeft });
  }

  public HTMLToken readHTMLToken (String[] stopStrings) {
    int begin = this.p;
    while (this.p < this.N) {
      boolean stop = false;
      for (String str : stopStrings) {
        if (match(str)) {
          stop = true;
          break;
        }
      }
      if (stop) break;
      ++this.p;
    }
    return new HTMLToken(input, begin, this.p, file);
  }

  public TagToken readTagToken (LiquidOptions options) {
    int begin = this.p;
    if (readToDelimiter(options.tagDelimiterRight) == -1) {
      throw error("tag " + snapshot(begin) + " not closed", begin);
    }
    TagToken token = new TagToken(input, begin, this.p, options, file);
    if ("raw".equals(token.name)) this.rawBeginAt = begin;
    return token;
  }

  public int readToDelimiter (String delimiter) {
    return readToDelimiter(delimiter, false);
  }

  public int readToDelimiter (String delimiter, boolean respectQuoted) {
    skipBlank();
    while (this.p < this.N) {
      if (respectQuoted && (peekType() & Characters.QUOTE) != 0) {
        readQuoted();
        continue;
      }
      ++this.p;
      if (rmatch(delimiter)) return this.p;
    }
    return -1;
  }

  public OutputToken readOutputToken (LiquidOptions options) {
    int begin = this.p;
    if (readToDelimiter(options.outputDelimiterRight, true) == -1) {
      throw error("output " + snapshot(begin) + " not closed", begin);
    }
    return new OutputToken(input, begin, this.p, options, file);
  }

  public Token readEndrawOrRawContent (LiquidOptions options) {
    String tagDelimiterLeft = options.tagDelimiterLeft;
    String tagDelimiterRight = options.tagDelimiterRight;
    int begin = this.p;
    int leftPos = readTo(tagDelimiterLeft) - tagDelimiterLeft.length();
    while (this.p < this.N) {
      if (!"endraw".equals(readIdentifier().getText())) {
        leftPos = readTo(tagDelimiterLeft) - tagDelimiterLeft.length();
        continue;
      }
      while (this.p <= this.N) {
        if (rmatch(tagDelimiterRight)) {
          int end = this.p;
          if (begin == leftPos) {
            this.rawBeginAt = -1;
            return new TagToken(input, begin, end, options, file);
          }
          this.p = leftPos;
          return new HTMLToken(input, begin, leftPos, file);
        }
        if (rmatch(tagDelimiterLeft)) break;
        this.p++;
      }
    }
    throw error("raw " + snapshot(this.rawBeginAt) + " not closed", begin);
  }

  public List<LiquidTagToken> readLiquidTagTokens (LiquidOptions options) {
    List<LiquidTagToken> tokens = new ArrayList<>();
    while (this.p < this.N) {
      LiquidTagToken token = readLiquidTagToken(options);
      if (token != null) tokens.add(token);
    }
    return tokens;
  }

  public LiquidTagToken readLiquidTagToken (LiquidOptions options) {
    skipBlank();
    if (end()) return null;
    int begin = this.p;
    readToDelimiter("\n");
    int end = this.p;
    return new LiquidTagToken(input, begin, end, options, file);
  }

  public TokenizationError error (String msg) {
    return error(msg, this.p);
  }

  public TokenizationError error (String msg, int pos) {
    return new TokenizationError(msg, new IdentifierToken(input, pos, this.N, file));
  }

  public void assertTrue (boolean predicate, String msg) {
    if (!predicate) throw error(msg);
  }

  public void assertTrue (boolean predicate, Supplier<String> msg) {
    if (!predicate) throw error(msg.get());
  }

  public void assertTrue (boolean predicate, String msg, int pos) {
    if (!predicate) throw error(msg, pos);
  }

  public String snapshot () {
    return snapshot(this.p);
  }

  public String snapshot (int begin) {
    return JsJson.stringify(Underscore.ellipsis(input.substring(begin, this.N), 32));
  }

  public IdentifierToken readWord () {
    return readIdentifier();
  }

  public IdentifierToken readIdentifier () {
    skipBlank();
    int begin = this.p;
    while (!end() && Characters.isWord(peek())) ++this.p;
    return new IdentifierToken(input, begin, this.p, file);
  }

  public IdentifierToken readNonEmptyIdentifier () {
    IdentifierToken id = readIdentifier();
    return id.size() > 0 ? id : null;
  }

  public String readTagName () {
    skipBlank();
    // an inline comment tag is named by one character
    if (this.p < this.N && input.charAt(this.p) == '#') {
      return input.substring(this.p, ++this.p);
    }
    return readIdentifier().getText();
  }

  public List<HashToken> readHashes (Object jekyllStyle) {
    List<HashToken> hashes = new ArrayList<>();
    while (true) {
      HashToken hash = readHash(jekyllStyle);
      if (hash == null) return hashes;
      hashes.add(hash);
    }
  }

  public HashToken readHash (Object jekyllStyle) {
    skipBlank();
    if (",".equals(peek())) ++this.p;
    int begin = this.p;
    IdentifierToken name = readNonEmptyIdentifier();
    if (name == null) return null;
    Token value = null;

    skipBlank();
    String sep = jekyllStyle instanceof String
        ? (String) jekyllStyle
        : (Boolean.TRUE.equals(jekyllStyle) ? "=" : ":");
    if (sep.equals(peek())) {
      ++this.p;
      value = readValue();
    }
    return new HashToken(input, begin, this.p, name, value, file);
  }

  public String remaining () {
    return input.substring(this.p, this.N);
  }

  public void advance () {
    advance(1);
  }

  public void advance (int step) {
    this.p += step;
  }

  public boolean end () {
    return this.p >= this.N;
  }

  public String read () {
    return this.p < input.length() ? String.valueOf(input.charAt(this.p++)) : null;
  }

  public int readTo (String end) {
    while (this.p < this.N) {
      ++this.p;
      if (rmatch(end)) return this.p;
    }
    return -1;
  }

  public Token readValue () {
    skipBlank();
    int begin = this.p;
    Token variable = readLiteral();
    if (variable == null) variable = readQuoted();
    if (variable == null) variable = readNumber();
    if (variable == null && "(".equals(peek())) {
      variable = readGroupOrRange();
    }
    List<Token> props = readProperties(variable == null);
    if (props.isEmpty()) return variable;
    return new PropertyAccessToken(variable, props, input, begin, this.p, null);
  }

  public Token readScopeValue () {
    skipBlank();
    int begin = this.p;
    List<Token> props = readProperties(true);
    if (props.isEmpty()) return null;
    return new PropertyAccessToken(null, props, input, begin, this.p, null);
  }

  private List<Token> readProperties (boolean isBegin) {
    List<Token> props = new ArrayList<>();
    while (true) {
      if ("[".equals(peek())) {
        this.p++;
        Token prop = readValue();
        if (prop == null) prop = new IdentifierToken(input, this.p, this.p, file);
        assertTrue(readTo("]") != -1, "[ not closed");
        props.add(prop);
        continue;
      }
      if (isBegin && props.isEmpty()) {
        IdentifierToken prop = readNonEmptyIdentifier();
        if (prop != null) {
          props.add(prop);
          continue;
        }
      }
      if (".".equals(peek()) && !".".equals(peek(1))) {
        // a range is written with two dots, so one dot alone is a property
        this.p++;
        IdentifierToken prop = readNonEmptyIdentifier();
        if (prop == null) break;
        props.add(prop);
        continue;
      }
      break;
    }
    return props;
  }

  public NumberToken readNumber () {
    skipBlank();
    boolean decimalFound = false;
    boolean digitFound = false;
    int n = 0;
    if ((peekType() & Characters.SIGN) != 0) n++;
    while (this.p + n <= this.N) {
      if ((peekType(n) & Characters.NUMBER) != 0) {
        digitFound = true;
        n++;
      } else if (".".equals(peek(n)) && !".".equals(peek(n + 1))) {
        if (decimalFound || !digitFound) return null;
        decimalFound = true;
        n++;
      } else {
        break;
      }
    }
    if (digitFound && !Characters.isWord(peek(n))) {
      NumberToken num = new NumberToken(input, this.p, this.p + n, file);
      advance(n);
      return num;
    }
    return null;
  }

  public LiteralToken readLiteral () {
    skipBlank();
    int end = matchTrie(literalTrie);
    if (end == -1) return null;
    LiteralToken literal = new LiteralToken(input, this.p, end, file);
    this.p = end;
    return literal;
  }

  public Token readGroupOrRange () {
    skipBlank();
    int begin = this.p;
    if (!"(".equals(peek())) return null;
    ++this.p;
    Token lhs = readValueOrThrow();
    skipBlank();

    if (".".equals(peek()) && ".".equals(peek(1))) {
      this.p += 2;
      Token rhs = readValueOrThrow();
      skipBlank();
      assertTrue(")".equals(read()), "invalid range syntax");
      return new RangeToken(input, begin, this.p, lhs, rhs, file);
    }

    if (groupedExpressions) {
      List<Token> tokens = new ArrayList<>();
      tokens.add(lhs);
      tokens.addAll(readExpressionTokens());
      Expression initial = new Expression(tokens);
      assertTrue(initial.valid(), () -> "invalid value expression: " + snapshot());
      List<FilterToken> filters = readFilters();
      skipBlank();
      assertTrue(")".equals(read()), "unbalanced parentheses");
      return new FilteredValueToken(initial, filters, input, begin, this.p, file);
    }

    throw error("invalid range syntax");
  }

  public Token readValueOrThrow () {
    Token value = readValue();
    assertTrue(value != null, () -> "unexpected token " + snapshot() + ", value expected");
    return value;
  }

  public QuotedToken readQuoted () {
    skipBlank();
    int begin = this.p;
    if ((peekType() & Characters.QUOTE) == 0) return null;
    ++this.p;
    boolean escaped = false;
    while (this.p < this.N) {
      ++this.p;
      if (input.charAt(this.p - 1) == input.charAt(begin) && !escaped) break;
      if (escaped) escaped = false;
      else if (input.charAt(this.p - 1) == '\\') escaped = true;
    }
    return new QuotedToken(input, begin, this.p, file);
  }

  /** Read a file name, which may itself hold outputs, stopping at a comma or a blank. */
  public List<Token> readFileNameTemplate (LiquidOptions options) {
    String outputDelimiterLeft = options.outputDelimiterLeft;
    String[] htmlStopStrings = { ",", " ", "\r", "\n", "\t", outputDelimiterLeft };
    List<Token> tokens = new ArrayList<>();
    while (this.p < this.N && !isStopString(htmlStopStrings, peek())) {
      tokens.add(match(outputDelimiterLeft)
          ? readOutputToken(options)
          : readHTMLToken(htmlStopStrings));
    }
    return tokens;
  }

  private static boolean isStopString (String[] stopStrings, String ch) {
    for (String stop : stopStrings) {
      if (stop.equals(ch)) return true;
    }
    return false;
  }

  public boolean match (String word) {
    for (int i = 0; i < word.length(); i++) {
      if (this.p + i >= input.length() || word.charAt(i) != input.charAt(this.p + i)) return false;
    }
    return true;
  }

  public boolean rmatch (String pattern) {
    for (int i = 0; i < pattern.length(); i++) {
      int at = this.p - 1 - i;
      if (at < 0 || at >= input.length()) return false;
      if (pattern.charAt(pattern.length() - 1 - i) != input.charAt(at)) return false;
    }
    return true;
  }

  public int peekType () {
    return peekType(0);
  }

  public int peekType (int n) {
    return this.p + n >= this.N ? 0 : Characters.TYPES[input.charAt(this.p + n)];
  }

  public String peek () {
    return peek(0);
  }

  public String peek (int n) {
    return this.p + n >= this.N ? "" : String.valueOf(input.charAt(this.p + n));
  }

  public void skipBlank () {
    while ((peekType() & Characters.BLANK) != 0) ++this.p;
  }
}
