/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 Jun Yang
 */
package com.liquidjs.tags;

import com.liquidjs.js.Js;
import com.liquidjs.template.TagClass;

import java.util.Map;

/** Every tag the engine registers by default. */
public final class Tags {
  private Tags () {}

  public static Map<String, Object> all () {
    Map<String, Object> tags = Js.newObject();
    tags.put("assign", AssignTag.CLASS);
    tags.put("for", ForTag.CLASS);
    tags.put("capture", CaptureTag.CLASS);
    tags.put("case", CaseTag.CLASS);
    tags.put("comment", CommentTag.CLASS);
    tags.put("include", IncludeTag.CLASS);
    tags.put("render", RenderTag.CLASS);
    tags.put("decrement", DecrementTag.CLASS);
    tags.put("increment", IncrementTag.CLASS);
    tags.put("cycle", CycleTag.CLASS);
    tags.put("if", IfTag.CLASS);
    tags.put("layout", LayoutTag.CLASS);
    tags.put("block", BlockTag.CLASS);
    tags.put("raw", RawTag.CLASS);
    tags.put("tablerow", TablerowTag.CLASS);
    tags.put("unless", UnlessTag.CLASS);
    tags.put("break", BreakTag.CLASS);
    tags.put("continue", ContinueTag.CLASS);
    tags.put("echo", EchoTag.CLASS);
    tags.put("liquid", LiquidTag.CLASS);
    tags.put("#", InlineCommentTag.CLASS);
    return tags;
  }

  /** The tag classes, typed, for a caller that registers them itself. */
  public static TagClass tagClass (String name) {
    Object tag = all().get(name);
    return tag instanceof TagClass ? (TagClass) tag : null;
  }
}
