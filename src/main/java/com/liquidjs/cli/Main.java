/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 Jun Yang
 */
package com.liquidjs.cli;

import com.liquidjs.Liquid;
import com.liquidjs.js.Js;
import com.liquidjs.js.JsJson;
import com.liquidjs.util.Underscore;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;

/** The command that renders a Liquid template. */
public final class Main {
  private Main () {}

  /** Render the template the arguments name, and exit 1 on any failure. */
  public static void main (String[] args) {
    PrintStream out = new PrintStream(new java.io.FileOutputStream(java.io.FileDescriptor.out),
        true, StandardCharsets.UTF_8);
    PrintStream err = new PrintStream(new java.io.FileOutputStream(java.io.FileDescriptor.err),
        true, StandardCharsets.UTF_8);
    System.exit(run(args, System.in, out, err));
  }

  /**
   * Run the command.
   *
   * The legacy mode, which reads the template from standard input, is taken when standard input
   * is not a terminal and there are either no arguments or one argument that is not a flag.
   */
  static int run (String[] args, InputStream stdin, PrintStream out, PrintStream err) {
    boolean stdinIsTerminal = System.console() != null;
    try {
      if (!stdinIsTerminal && (args.length == 0
          || (args.length == 1 && !args[0].startsWith("-")))) {
        renderLegacy(args, stdin, out, err);
      } else {
        render(args, stdin, out, err);
      }
      return 0;
    } catch (Commander.CommanderError error) {
      err.print("error: " + error.getMessage() + "\n");
      String after = helpAfterError();
      if (!after.isEmpty()) err.print(after + "\n");
      return 1;
    } catch (RuntimeException | IOException error) {
      String message = error.getMessage();
      err.print((message == null ? error.toString() : message) + "\n");
      return 1;
    }
  }

  private static String helpAfterError () {
    return "Use -h or --help for additional information.";
  }

  private static Commander program () {
    return new Commander()
        .name("liquidjs")
        .description("Render a Liquid template")
        .requiredOption("-t, --template <liquid | @path>",
            "liquid template to render (@- to read from stdin)")
        .option("-c, --context <json | @path>",
            "input context in JSON format (@- to read from stdin)")
        .option("-o, --output <path>",
            "write rendered output to file (omit to write to stdout)")
        .option("--cache [size]",
            "cache previously parsed template structures (default cache size: 1024)")
        .option("--extname <string>",
            "use a default filename extension when resolving partials and layouts")
        .option("--jekyll-include",
            "use jekyll-style include (pass parameters to include variable of current scope)")
        .option("--js-truthy", "use JavaScript-style truthiness")
        .option("--layouts <path...>",
            "directories from where to resolve layouts (defaults to --root)")
        .option("--lenient-if",
            "do not throw on undefined variables in conditional expressions (when using "
            + "--strict-variables)")
        .option("--no-dynamic-partials",
            "always treat file paths for partials and layouts as a literal value")
        .option("--no-greedy", "disable greedy matching for --trim* options")
        .option("--no-relative-reference",
            "require absolute file paths for partials and layouts")
        .option("--ordered-filter-parameters",
            "respect parameter order when using filters")
        .option("--output-delimiter-left <string>",
            "left delimiter to use for liquid outputs")
        .option("--output-delimiter-right <string>",
            "right delimiter to use for liquid outputs")
        .option("--partials <path...>",
            "directories from where to resolve partials (defaults to --root)")
        .option("--preserve-timezones", "preserve input timezone in date filter")
        .option("--root <path...>",
            "directories from where to resolve partials and layouts (defaults to \".\")")
        .option("--strict-filters", "throw on undefined filters instead of skipping them")
        .option("--strict-variables",
            "throw on undefined variables instead of rendering them as empty string")
        .option("--tag-delimiter-left", "left delimiter to use for liquid tags")
        .option("--tag-delimiter-right", "right delimiter to use for liquid tags")
        .option("--timezone-offset <value>",
            "JavaScript timezone name or timezoneOffset value to use in date filter (defaults "
            + "to local timezone)")
        .option("--trim-output-left", "trim whitespace from left of liquid outputs")
        .option("--trim-output-right", "trim whitespace from right of liquid outputs")
        .option("--trim-tag-left", "trim whitespace from left of liquid tags")
        .option("--trim-tag-right", "trim whitespace from right of liquid tags")
        .showHelpAfterError("Use -h or --help for additional information.");
  }

  private static void render (String[] args, InputStream stdin, PrintStream out, PrintStream err)
      throws IOException {
    Commander program = program();
    for (String arg : args) {
      if ("-h".equals(arg) || "--help".equals(arg)) {
        out.print(program.helpInformation());
        return;
      }
    }
    program.parse(args);
    Map<String, Object> options = program.opts();

    int stdinSpecifiers = 0;
    for (Object value : options.values()) {
      if ("@-".equals(value)) stdinSpecifiers++;
    }
    if (stdinSpecifiers > 1) {
      throw new RuntimeException("The stdin input specifier '@-' must only be used once.");
    }

    String template = resolveInputOption(options.get("template"), stdin);
    Object context = resolveContext(options.get("context"), stdin);
    Liquid liquid = new Liquid(liquidOptions(options));
    Object output = liquid.parseAndRenderSync(template, context);
    Object outputPath = options.get("output");
    if (Js.isString(outputPath)) {
      Files.write(Paths.get((String) outputPath),
          Underscore.stringify(output).getBytes(StandardCharsets.UTF_8));
    } else {
      out.print(Underscore.stringify(output));
    }
  }

  /** The options the engine takes, under the names it knows them by. */
  private static Map<String, Object> liquidOptions (Map<String, Object> options) {
    Map<String, Object> out = new LinkedHashMap<>(options);
    out.remove("template");
    out.remove("context");
    out.remove("output");
    return out;
  }

  private static Object resolveContext (Object contextOption, InputStream stdin)
      throws IOException {
    String contextJson = "{}";
    if (Js.truthy(contextOption)) {
      contextJson = resolveInputOption(contextOption, stdin);
    }
    return JsJson.parse(contextJson);
  }

  private static String resolveInputOption (Object option, InputStream stdin) throws IOException {
    if (!Js.truthy(option) || !Js.isString(option)) return null;
    String value = (String) option;
    if ("@-".equals(value)) return readStream(stdin);
    if (value.startsWith("@")) {
      String filePath = value.substring(1);
      Path path = Paths.get(filePath);
      if (!Files.exists(path)) {
        throw new RuntimeException("ENOENT: no such file or directory, stat '" + filePath + "'");
      }
      if (!Files.isRegularFile(path)) {
        throw new RuntimeException("'" + filePath + "' does not exist or is not a file");
      }
      return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
    }
    return value;
  }

  private static String readStream (InputStream stream) throws IOException {
    return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
  }

  private static void renderLegacy (String[] args, InputStream stdin, PrintStream out,
                                    PrintStream err) throws IOException {
    err.print("Reading template from stdin. This mode will be removed in next major version, "
        + "use --template option instead.\n");
    String contextArg = args.length > 0 ? args[0] : null;
    Object context = Js.newObject();
    if (contextArg != null && !contextArg.isEmpty()) {
      String contextJson = resolveInputOptionLegacy(contextArg);
      context = JsJson.parse(contextJson);
    }
    String template = readStream(stdin);
    Liquid liquid = new Liquid();
    Object output = liquid.parseAndRenderSync(template, context);
    out.print(Underscore.stringify(output));
  }

  private static String resolveInputOptionLegacy (String option) throws IOException {
    Path path = Paths.get(option);
    if (Files.exists(path) && Files.isRegularFile(path)) {
      return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
    }
    return option;
  }
}
