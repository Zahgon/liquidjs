/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 Jun Yang
 */
package com.liquidjs.cli;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The command line option parser: it reads the flags, reports the same errors and prints the same
 * help as the command the engine ships, including the suggestion a misspelt flag gets.
 */
public class Commander {
  /** One declared option. */
  static final class Option {
    final String shortFlag;
    final String longFlag;
    /** The option's key, in camel case. */
    final String key;
    final String argument;
    final boolean optionalArgument;
    final boolean variadic;
    final boolean negated;
    final String description;

    Option (String flags, String description) {
      this.description = description;
      String[] parts = flags.split(",");
      String shortFlag = null;
      String longFlag = null;
      String argument = null;
      for (String part : parts) {
        String trimmed = part.trim();
        if (trimmed.isEmpty()) continue;
        int space = trimmed.indexOf(' ');
        String flag = space < 0 ? trimmed : trimmed.substring(0, space);
        if (space >= 0) argument = trimmed.substring(space + 1).trim();
        if (flag.startsWith("--")) longFlag = flag;
        else if (flag.startsWith("-")) shortFlag = flag;
      }
      this.shortFlag = shortFlag;
      this.longFlag = longFlag;
      this.argument = argument;
      this.optionalArgument = argument != null && argument.startsWith("[");
      this.variadic = argument != null && argument.contains("...");
      this.negated = longFlag != null && longFlag.startsWith("--no-");
      String name = longFlag == null ? shortFlag : longFlag;
      String bare = this.negated ? name.substring(5) : name.replaceFirst("^--?", "");
      this.key = camelCase(bare);
    }

    String flags () {
      StringBuilder out = new StringBuilder();
      if (shortFlag != null) out.append(shortFlag).append(", ");
      if (longFlag != null) out.append(longFlag);
      if (argument != null) out.append(' ').append(argument);
      return out.toString();
    }

    String name () {
      return longFlag == null ? shortFlag : longFlag;
    }
  }

  /** Raised for every option error, carrying the message the command prints. */
  static class CommanderError extends RuntimeException {
    private static final long serialVersionUID = 1L;

    CommanderError (String message) {
      super(message);
    }
  }

  private final List<Option> options = new ArrayList<>();
  private final List<String> required = new ArrayList<>();
  private final Map<String, Object> values = new LinkedHashMap<>();
  private String name = "";
  private String description = "";
  private String helpAfterError = "";

  public Commander name (String name) {
    this.name = name;
    return this;
  }

  public Commander description (String description) {
    this.description = description;
    return this;
  }

  public Commander requiredOption (String flags, String description) {
    Option option = new Option(flags, description);
    options.add(option);
    required.add(option.key);
    return this;
  }

  public Commander option (String flags, String description) {
    Option option = new Option(flags, description);
    options.add(option);
    if (option.negated) values.put(option.key, Boolean.TRUE);
    return this;
  }

  public Commander showHelpAfterError (String message) {
    this.helpAfterError = message;
    return this;
  }

  /** The options the command line set, keyed by their camel case name. */
  public Map<String, Object> opts () {
    return values;
  }

  public void parse (String[] argv) {
    String unknown = null;
    int i = 0;
    while (i < argv.length) {
      String arg = argv[i];
      if (!arg.startsWith("-") || "-".equals(arg)) {
        i++;
        continue;
      }
      String flag = arg;
      String inlineValue = null;
      int equals = arg.indexOf('=');
      if (arg.startsWith("--") && equals > 0) {
        flag = arg.substring(0, equals);
        inlineValue = arg.substring(equals + 1);
      }
      Option option = find(flag);
      if (option == null) {
        if (unknown == null) unknown = flag;
        i++;
        continue;
      }
      i++;
      if (option.negated) {
        values.put(option.key, Boolean.FALSE);
        continue;
      }
      if (option.argument == null) {
        values.put(option.key, Boolean.TRUE);
        continue;
      }
      if (option.variadic) {
        List<Object> collected = new ArrayList<>();
        if (inlineValue != null) collected.add(inlineValue);
        while (i < argv.length && (!argv[i].startsWith("-") || "-".equals(argv[i]))) {
          collected.add(argv[i++]);
        }
        if (collected.isEmpty()) {
          throw new CommanderError("option '" + option.flags() + "' argument missing");
        }
        values.put(option.key, collected);
        continue;
      }
      if (inlineValue != null) {
        values.put(option.key, inlineValue);
        continue;
      }
      if (option.optionalArgument) {
        // an optional argument is not taken from something that looks like another flag
        if (i < argv.length && (!argv[i].startsWith("-") || "-".equals(argv[i]))) {
          values.put(option.key, argv[i++]);
        } else {
          values.put(option.key, Boolean.TRUE);
        }
        continue;
      }
      // a required argument is taken from whatever follows, flag or not
      if (i < argv.length) {
        values.put(option.key, argv[i++]);
        continue;
      }
      throw new CommanderError("option '" + option.flags() + "' argument missing");
    }
    for (String key : required) {
      if (!values.containsKey(key)) {
        Option option = findByKey(key);
        throw new CommanderError("required option '" + option.flags() + "' not specified");
      }
    }
    if (unknown != null) {
      throw new CommanderError("unknown option '" + unknown + "'" + suggestSimilar(unknown));
    }
  }

  private Option find (String flag) {
    for (Option option : options) {
      if (flag.equals(option.shortFlag) || flag.equals(option.longFlag)) return option;
    }
    return null;
  }

  private Option findByKey (String key) {
    for (Option option : options) {
      if (key.equals(option.key)) return option;
    }
    return null;
  }

  /** The help text, as the command prints it for -h. */
  public String helpInformation () {
    StringBuilder out = new StringBuilder();
    out.append("Usage: ").append(name).append(" [options]\n\n");
    out.append(description).append("\n\n");
    out.append("Options:\n");
    List<String[]> rows = new ArrayList<>();
    int width = 0;
    for (Option option : options) {
      String term = "  " + option.flags();
      rows.add(new String[] { term, option.description });
      width = Math.max(width, term.length());
    }
    rows.add(new String[] { "  -h, --help", "display help for command" });
    width = Math.max(width, "  -h, --help".length());
    int indent = width + 2;
    for (String[] row : rows) {
      out.append(row[0]);
      out.append(" ".repeat(indent - row[0].length()));
      out.append(wrap(row[1], indent)).append('\n');
    }
    return out.toString();
  }

  /** Wrap a description at the width the command uses, carrying the indent over. */
  private static String wrap (String text, int indent) {
    int width = 80 - indent - 1;
    StringBuilder out = new StringBuilder();
    int lineLength = 0;
    String[] words = text.split(" ");
    for (int i = 0; i < words.length; i++) {
      String word = words[i];
      if (lineLength > 0 && lineLength + 1 + word.length() > width) {
        out.append('\n').append(" ".repeat(indent));
        lineLength = 0;
      } else if (lineLength > 0) {
        out.append(' ');
        lineLength++;
      }
      out.append(word);
      lineLength += word.length();
    }
    return out.toString();
  }

  public String getHelpAfterError () {
    return helpAfterError;
  }

  /** The flag names a misspelt flag is compared against. */
  private List<String> candidates () {
    List<String> out = new ArrayList<>();
    for (Option option : options) {
      if (option.longFlag != null) out.add(option.longFlag.substring(2));
      if (option.negated) out.add(option.longFlag.substring(5));
    }
    out.add("help");
    return out;
  }

  /**
   * The suggestion a misspelt flag gets: the closest flag names by edit distance, as long as they
   * are similar enough to be worth printing.
   */
  String suggestSimilar (String flag) {
    if (!flag.startsWith("--")) return "";
    String word = flag.substring(2);
    double minSimilarity = 0.4;
    int bestDistance = Integer.MAX_VALUE;
    List<String> similar = new ArrayList<>();
    for (String candidate : candidates()) {
      if (candidate.length() <= 1) continue;
      int distance = editDistance(word, candidate);
      int length = Math.max(word.length(), candidate.length());
      double similarity = (length - distance) / (double) length;
      if (similarity > minSimilarity) {
        if (distance < bestDistance) {
          bestDistance = distance;
          similar.clear();
          similar.add(candidate);
        } else if (distance == bestDistance) {
          similar.add(candidate);
        }
      }
    }
    similar.sort(String::compareTo);
    List<String> flags = new ArrayList<>();
    for (String candidate : similar) flags.add("--" + candidate);
    if (flags.size() > 1) return "\n(Did you mean one of " + String.join(", ", flags) + "?)";
    if (flags.size() == 1) return "\n(Did you mean " + flags.get(0) + "?)";
    return "";
  }

  /** Edit distance that also counts a transposition of two neighbours as one edit. */
  static int editDistance (String a, String b) {
    if (Math.abs(a.length() - b.length()) > 3) return Math.max(a.length(), b.length());
    int[][] d = new int[a.length() + 1][b.length() + 1];
    for (int i = 0; i <= a.length(); i++) d[i][0] = i;
    for (int j = 0; j <= b.length(); j++) d[0][j] = j;
    for (int i = 1; i <= a.length(); i++) {
      for (int j = 1; j <= b.length(); j++) {
        int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
        d[i][j] = Math.min(Math.min(d[i - 1][j] + 1, d[i][j - 1] + 1), d[i - 1][j - 1] + cost);
        if (i > 1 && j > 1 && a.charAt(i - 1) == b.charAt(j - 2)
            && a.charAt(i - 2) == b.charAt(j - 1)) {
          d[i][j] = Math.min(d[i][j], d[i - 2][j - 2] + 1);
        }
      }
    }
    return d[a.length()][b.length()];
  }

  static String camelCase (String flag) {
    StringBuilder out = new StringBuilder();
    boolean upper = false;
    for (int i = 0; i < flag.length(); i++) {
      char ch = flag.charAt(i);
      if (ch == '-') {
        upper = true;
        continue;
      }
      out.append(upper ? Character.toUpperCase(ch) : ch);
      upper = false;
    }
    return out.toString();
  }
}
