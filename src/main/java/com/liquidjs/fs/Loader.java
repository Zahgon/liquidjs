/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 Jun Yang
 */
package com.liquidjs.fs;

import com.liquidjs.LiquidOptions;
import com.liquidjs.util.Assertions;

import java.util.ArrayList;
import java.util.List;

/** Finds a template file in the directories an option names, in the order they were given. */
public class Loader {
  private final LiquidOptions options;
  private final boolean relative;

  /** A loader over the directories the options name. */
  public Loader (LiquidOptions options) {
    this.options = options;
    if (options.relativeReference) {
      String sep = options.fs.sep();
      Assertions.assertTrue(sep != null && !sep.isEmpty(), "`fs.sep` is required for relative reference");
      this.relative = true;
    } else {
      this.relative = false;
    }
  }

  /** True when a referenced file is written as a relative path. */
  public boolean shouldLoadRelative (String referencedFile) {
    if (!relative) return false;
    String sep = options.fs.sep();
    String[] prefixes = { "." + sep, ".." + sep, "./", "../" };
    for (String prefix : prefixes) {
      if (referencedFile.startsWith(prefix)) return true;
    }
    return false;
  }

  /** The path of a template, raising when no directory holds it. */
  public String lookup (String file, LookupType type, String currentFile) {
    List<String> dirs = options.directories(type);
    for (String filepath : candidates(file, dirs, currentFile)) {
      boolean allowed = false;
      for (String dir : dirs) {
        Boolean contained = options.fs.containsSync(dir, filepath);
        if (contained == null || contained) {
          allowed = true;
          break;
        }
      }
      if (!allowed) continue;
      if (options.fs.existsSync(filepath)) return filepath;
    }
    throw lookupError(file, dirs);
  }

  /** Every path a template could be at, in the order they are tried. */
  public List<String> candidates (String file, List<String> dirs, String currentFile) {
    List<String> out = new ArrayList<>();
    FS fs = options.fs;
    if (shouldLoadRelative(file) && currentFile != null) {
      out.add(fs.resolve(dirname(currentFile), file, options.extname));
    }
    for (String dir : dirs) {
      out.add(fs.resolve(dir, file, options.extname));
    }
    String fallback = fs.fallback(file);
    if (fallback != null) out.add(fallback);
    return out;
  }

  private String dirname (String path) {
    String dir = options.fs.dirname(path);
    Assertions.assertTrue(dir != null, "`fs.dirname` is required for relative reference");
    return dir;
  }

  private RuntimeException lookupError (String file, List<String> roots) {
    return new RuntimeException("ENOENT: Failed to lookup \"" + file + "\" in \""
        + String.join(",", roots) + "\"");
  }
}
