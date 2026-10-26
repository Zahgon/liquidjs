/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 Jun Yang
 */
package com.liquidjs.js;

import com.liquidjs.drop.Drop;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** JSON serialisation and parsing with the exact escapes, key order and spacing of the source. */
public final class JsJson {
  private JsJson () {}

  /** The replacer a caller passes to serialisation: it sees every value, in document order. */
  public interface Visitor {
    Object visit (Object holder, String key, Object value);
  }

  /** Serialise a value with no indentation and no replacer. */
  public static String stringify (Object value) {
    return stringify(value, "", null);
  }

  /**
   * Serialise a value. Returns null where the source returns undefined, which happens for an
   * absent value, a function or a value the replacer drops.
   */
  public static String stringify (Object value, String indent, Visitor visitor) {
    Object root = visitor == null ? value : visitor.visit(null, "", value);
    StringBuilder out = new StringBuilder();
    boolean written = write(out, root, indent, "", visitor);
    return written ? out.toString() : null;
  }

  private static boolean write (StringBuilder out, Object value, String indent, String current,
                                Visitor visitor) {
    if (value == Js.UNDEFINED) return false;
    if (value == null) {
      out.append("null");
      return true;
    }
    if (value instanceof Boolean) {
      out.append(value.toString());
      return true;
    }
    if (value instanceof Double) {
      double number = (Double) value;
      out.append(Double.isNaN(number) || Double.isInfinite(number) ? "null" : JsNumber.toString(number));
      return true;
    }
    if (value instanceof String) {
      quote(out, (String) value);
      return true;
    }
    if (value instanceof List) {
      List<Object> array = Js.asArray(value);
      String inner = current + indent;
      out.append('[');
      for (int i = 0; i < array.size(); i++) {
        if (i > 0) out.append(',');
        if (!indent.isEmpty()) out.append('\n').append(inner);
        Object item = array.get(i);
        if (visitor != null) item = visitor.visit(value, String.valueOf(i), item);
        if (!write(out, item, indent, inner, visitor)) out.append("null");
      }
      if (!indent.isEmpty() && !array.isEmpty()) out.append('\n').append(current);
      out.append(']');
      return true;
    }
    Map<String, Object> object = properties(value);
    String inner = current + indent;
    out.append('{');
    boolean first = true;
    for (String key : Js.keys(object)) {
      Object member = object.get(key);
      if (visitor != null) member = visitor.visit(value, key, member);
      StringBuilder member_out = new StringBuilder();
      if (!write(member_out, member, indent, inner, visitor)) continue;
      if (!first) out.append(',');
      if (!indent.isEmpty()) out.append('\n').append(inner);
      quote(out, key);
      out.append(':');
      if (!indent.isEmpty()) out.append(' ');
      out.append(member_out);
      first = false;
    }
    if (!indent.isEmpty() && !first) out.append('\n').append(current);
    out.append('}');
    return true;
  }

  /** The own enumerable properties of a value, which for a drop are the fields it declares. */
  public static Map<String, Object> properties (Object value) {
    if (value instanceof Drop) return ((Drop) value).ownProperties();
    if (value instanceof Map) return Js.asObject(value);
    return Js.newObject();
  }

  /** The indent a space argument stands for: a count of spaces, or a string clamped to 10. */
  public static String indentOf (Object space) {
    if (Js.isNumber(space)) {
      int count = (int) Math.min(10, Math.max(0, (double) (Double) space));
      return " ".repeat(count);
    }
    if (Js.isString(space)) {
      String str = (String) space;
      return str.length() > 10 ? str.substring(0, 10) : str;
    }
    return "";
  }

  private static void quote (StringBuilder out, String str) {
    out.append('"');
    for (int i = 0; i < str.length(); i++) {
      char ch = str.charAt(i);
      switch (ch) {
        case '"': out.append("\\\""); break;
        case '\\': out.append("\\\\"); break;
        case '\b': out.append("\\b"); break;
        case '\f': out.append("\\f"); break;
        case '\n': out.append("\\n"); break;
        case '\r': out.append("\\r"); break;
        case '\t': out.append("\\t"); break;
        default:
          if (ch < 0x20) {
            out.append(String.format("\\u%04x", (int) ch));
          } else if (Character.isSurrogate(ch)) {
            boolean paired = Character.isHighSurrogate(ch) && i + 1 < str.length()
                && Character.isLowSurrogate(str.charAt(i + 1));
            if (paired) {
              out.append(ch).append(str.charAt(++i));
            } else {
              out.append(String.format("\\u%04x", (int) ch));
            }
          } else {
            out.append(ch);
          }
      }
    }
    out.append('"');
  }

  /** Parse JSON text into the value model: an object keeps its key order. */
  public static Object parse (String text) {
    Parser parser = new Parser(text);
    parser.skipBlank();
    Object value = parser.value();
    parser.skipBlank();
    if (parser.at < text.length()) {
      throw parser.positioned("Unexpected non-whitespace character after JSON", parser.at);
    }
    return value;
  }

  /** Raised by parsing, with the message the source runtime reports. */
  public static class JsonError extends RuntimeException {
    private static final long serialVersionUID = 1L;

    /** A parse failure, carrying the message the source runtime reports. */
    public JsonError (String message) {
      super(message);
    }
  }

  private static final class Parser {
    private final String text;
    private int at;

    Parser (String text) {
      this.text = text;
    }

    /** "<what> in JSON at position <n> (line <l> column <c>)" */
    JsonError error (String what, int at) {
      return positioned(what + " in JSON", at);
    }

    /** A message that names the position it was raised at. */
    JsonError positioned (String what, int at) {
      int line = 1;
      int column = 1;
      for (int i = 0; i < at && i < text.length(); i++) {
        if (text.charAt(i) == '\n') {
          line++;
          column = 1;
        } else {
          column++;
        }
      }
      return new JsonError(what + " at position " + at
          + " (line " + line + " column " + column + ")");
    }

    JsonError endOfInput () {
      return new JsonError("Unexpected end of JSON input");
    }

    /** "Unexpected token '<c>', "<text>" is not valid JSON" */
    JsonError unexpectedToken (int at) {
      String snippet = text.length() <= 30
          ? text
          : "..." + text.substring(Math.max(0, at - 14), Math.min(text.length(), at + 15)) + "...";
      return new JsonError("Unexpected token '" + text.charAt(at) + "', \"" + snippet
          + "\" is not valid JSON");
    }

    void skipBlank () {
      while (at < text.length() && (text.charAt(at) == ' ' || text.charAt(at) == '\t'
          || text.charAt(at) == '\n' || text.charAt(at) == '\r')) at++;
    }

    Object value () {
      if (at >= text.length()) throw endOfInput();
      char ch = text.charAt(at);
      switch (ch) {
        case '{': return object();
        case '[': return array();
        case '"': return string();
        case 't': return literal("true", Boolean.TRUE);
        case 'f': return literal("false", Boolean.FALSE);
        case 'n': return literal("null", null);
        default: return number();
      }
    }

    private Object literal (String word, Object value) {
      for (int i = 0; i < word.length(); i++) {
        if (at + i >= text.length()) throw endOfInput();
        if (text.charAt(at + i) != word.charAt(i)) throw unexpectedToken(at + i);
      }
      at += word.length();
      return value;
    }

    private Map<String, Object> object () {
      Map<String, Object> map = Js.newObject();
      at++;
      skipBlank();
      if (at >= text.length() || text.charAt(at) != '"') {
        if (at < text.length() && text.charAt(at) == '}') {
          at++;
          return map;
        }
        throw error("Expected property name or '}'", at);
      }
      while (true) {
        skipBlank();
        if (at >= text.length() || text.charAt(at) != '"') {
          throw error("Expected double-quoted property name", at);
        }
        String key = string();
        skipBlank();
        if (at >= text.length() || text.charAt(at) != ':') {
          throw error("Expected ':' after property name", at);
        }
        at++;
        skipBlank();
        map.put(key, value());
        skipBlank();
        if (at < text.length() && text.charAt(at) == ',') {
          at++;
          continue;
        }
        if (at >= text.length() || text.charAt(at) != '}') {
          throw error("Expected ',' or '}' after property value", at);
        }
        at++;
        return map;
      }
    }

    private List<Object> array () {
      List<Object> list = new ArrayList<>();
      at++;
      skipBlank();
      if (at < text.length() && text.charAt(at) == ']') {
        at++;
        return list;
      }
      while (true) {
        skipBlank();
        list.add(value());
        skipBlank();
        if (at < text.length() && text.charAt(at) == ',') {
          at++;
          continue;
        }
        if (at >= text.length() || text.charAt(at) != ']') {
          throw error("Expected ',' or ']' after array element", at);
        }
        at++;
        return list;
      }
    }

    private String string () {
      at++;
      StringBuilder out = new StringBuilder();
      while (at < text.length()) {
        char ch = text.charAt(at++);
        if (ch == '"') return out.toString();
        if (ch != '\\') {
          out.append(ch);
          continue;
        }
        if (at >= text.length()) throw endOfInput();
        char escape = text.charAt(at++);
        switch (escape) {
          case '"': out.append('"'); break;
          case '\\': out.append('\\'); break;
          case '/': out.append('/'); break;
          case 'b': out.append('\b'); break;
          case 'f': out.append('\f'); break;
          case 'n': out.append('\n'); break;
          case 'r': out.append('\r'); break;
          case 't': out.append('\t'); break;
          case 'u':
            if (at + 4 > text.length()) throw endOfInput();
            out.append((char) Integer.parseInt(text.substring(at, at + 4), 16));
            at += 4;
            break;
          default: throw unexpectedToken(at - 1);
        }
      }
      throw endOfInput();
    }

    private Double number () {
      int begin = at;
      if (at < text.length() && text.charAt(at) == '-') at++;
      while (at < text.length() && (Character.isDigit(text.charAt(at)) || text.charAt(at) == '.'
          || text.charAt(at) == 'e' || text.charAt(at) == 'E'
          || ((text.charAt(at) == '-' || text.charAt(at) == '+')
              && (text.charAt(at - 1) == 'e' || text.charAt(at - 1) == 'E')))) at++;
      if (begin == at) throw unexpectedToken(at);
      try {
        return Double.parseDouble(text.substring(begin, at));
      } catch (NumberFormatException err) {
        throw unexpectedToken(begin);
      }
    }
  }
}
