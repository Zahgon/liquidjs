/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 Jun Yang
 */
package com.liquidjs.fs;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/** The default file system: templates are read from the host's files. */
public class FsImpl implements FS {
  /** The separator between the parts of a path on this host. */
  public static final String SEPARATOR = java.io.File.separator;
  /** The shared default file system. */
  public static final FsImpl INSTANCE = new FsImpl();

  @Override
  public boolean existsSync (String filepath) {
    try {
      return Files.exists(Paths.get(filepath));
    } catch (RuntimeException err) {
      return false;
    }
  }

  @Override
  public String readFileSync (String filepath) {
    try {
      return new String(Files.readAllBytes(Paths.get(filepath)), StandardCharsets.UTF_8);
    } catch (IOException err) {
      throw new UncheckedIOException("ENOENT: no such file or directory, open '" + filepath + "'", err);
    }
  }

  @Override
  public String resolve (String root, String file, String ext) {
    String name = file;
    if (extname(name).isEmpty()) name += ext;
    Path resolved = Paths.get(root).toAbsolutePath().resolve(name).normalize();
    return resolved.toString();
  }

  @Override
  public Boolean containsSync (String root, String file) {
    try {
      Path realRoot = Paths.get(root).toRealPath();
      Path realFile = Paths.get(file).toRealPath();
      String prefix = realRoot.toString().endsWith(SEPARATOR)
          ? realRoot.toString()
          : realRoot + SEPARATOR;
      return realFile.toString().startsWith(prefix);
    } catch (IOException | RuntimeException err) {
      return false;
    }
  }

  @Override
  public String sep () {
    return SEPARATOR;
  }

  @Override
  public String dirname (String filepath) {
    Path parent = Paths.get(filepath).getParent();
    return parent == null ? "." : parent.toString();
  }

  /** The extension of a path, including its dot, or an empty string when it has none. */
  public static String extname (String filepath) {
    int slash = Math.max(filepath.lastIndexOf('/'), filepath.lastIndexOf('\\'));
    String base = filepath.substring(slash + 1);
    int dot = base.lastIndexOf('.');
    if (dot <= 0) return "";
    return base.substring(dot);
  }
}
