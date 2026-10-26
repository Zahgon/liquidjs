/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 Jun Yang
 */
package com.liquidjs.fs;

import com.liquidjs.js.Js;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Templates held in memory, keyed by their name. */
public class MapFS implements FS {
  private final Map<String, String> mapping;

  /** A file system over templates held in memory, keyed by name. */
  public MapFS (Map<String, String> mapping) {
    this.mapping = mapping;
  }

  @Override
  public boolean existsSync (String filepath) {
    return !Js.isNil(mapping.get(filepath));
  }

  @Override
  public String readFileSync (String filepath) {
    String content = mapping.get(filepath);
    if (Js.isNil(content)) throw new RuntimeException("ENOENT: " + filepath);
    return content;
  }

  @Override
  public String sep () {
    return "/";
  }

  @Override
  public String dirname (String filepath) {
    List<String> segments = new ArrayList<>(java.util.Arrays.asList(filepath.split("/", -1)));
    segments.remove(segments.size() - 1);
    return String.join("/", segments);
  }

  @Override
  public String resolve (String dir, String file, String ext) {
    String name = file + ext;
    if (".".equals(dir)) return name;
    List<String> segments = new ArrayList<>(java.util.Arrays.asList(dir.split("/+", -1)));
    for (String segment : name.split("/", -1)) {
      if (".".equals(segment) || segment.isEmpty()) continue;
      if ("..".equals(segment)) {
        if (segments.size() > 1 || !segments.get(0).isEmpty()) {
          segments.remove(segments.size() - 1);
        }
      } else {
        segments.add(segment);
      }
    }
    return String.join("/", segments);
  }
}
