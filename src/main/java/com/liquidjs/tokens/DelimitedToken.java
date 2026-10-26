/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 Jun Yang
 */
package com.liquidjs.tokens;

import com.liquidjs.util.Characters;

/** A token written between delimiters, whose content is trimmed of blanks and of trim markers. */
public abstract class DelimitedToken extends Token {
  public boolean trimLeft = false;
  public boolean trimRight = false;
  public final int[] contentRange;

  protected DelimitedToken (int kind, int contentBegin, int contentEnd, String input, int begin,
                            int end, boolean trimLeft, boolean trimRight, String file) {
    super(kind, input, begin, end, file);
    boolean markedLeft = contentBegin < input.length() && input.charAt(contentBegin) == '-';
    boolean markedRight = contentEnd - 1 >= 0 && contentEnd - 1 < input.length()
        && input.charAt(contentEnd - 1) == '-';

    int l = markedLeft ? contentBegin + 1 : contentBegin;
    int r = markedRight ? contentEnd - 1 : contentEnd;
    while (l < r && (Characters.TYPES[input.charAt(l)] & Characters.BLANK) != 0) l++;
    while (r > l && (Characters.TYPES[input.charAt(r - 1)] & Characters.BLANK) != 0) r--;

    this.contentRange = new int[] { l, r };
    this.trimLeft = markedLeft || trimLeft;
    this.trimRight = markedRight || trimRight;
  }

  public String getContentText () {
    return slice(input, contentRange[0], contentRange[1]);
  }

  @Override
  public boolean hasContent () {
    return true;
  }

  @Override
  public Object getContent () {
    return getContentText();
  }
}
