/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 Jun Yang
 */
package com.liquidjs.fs;

/**
 * Where templates are read from. A custom implementation may leave the optional members out, in
 * which case the loader assumes a file is contained in its root and resolves no relative paths.
 */
public interface FS {
  /** Check whether a file exists. */
  boolean existsSync (String filepath);

  /** Read a file. */
  String readFileSync (String filepath);

  /** Resolve a file against a directory, honouring the extname option. */
  String resolve (String dir, String file, String ext);

  /** Check whether a file is contained in a root; null when the implementation cannot tell. */
  default Boolean containsSync (String root, String file) {
    return null;
  }

  /** The path separator; defaults to a forward slash. */
  default String sep () {
    return null;
  }

  /** The directory part of a path, required for relative references. */
  default String dirname (String file) {
    return null;
  }

  /** A last resort lookup for a file none of the roots hold. */
  default String fallback (String file) {
    return null;
  }
}
