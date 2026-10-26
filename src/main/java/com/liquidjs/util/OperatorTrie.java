/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 Jun Yang
 */
package com.liquidjs.util;

import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * A trie over operator and literal names, built once per input mapping and reused: matching walks
 * it one code unit at a time and a name that ends in a word character needs a boundary after it.
 */
public final class OperatorTrie {
  private OperatorTrie () {}

  /** One node of the trie. */
  public static final class Node<T> {
    /** The nodes reachable by one more code unit. */
    public final Map<Character, Node<T>> children = new HashMap<>();
    /** What the name ending here stands for. */
    public T data;
    /** Whether a name ends here. */
    public boolean end;
    /** Whether the name ending here must be followed by a boundary. */
    public boolean needBoundary;
  }

  private static final Map<Object, Node<?>> CACHE = new WeakHashMap<>();

  /** The trie over a mapping of names, built once per mapping and reused. */
  @SuppressWarnings("unchecked")
  public static <T> Node<T> create (Map<String, T> input) {
    synchronized (CACHE) {
      Node<?> cached = CACHE.get(input);
      if (cached != null) return (Node<T>) cached;
    }
    Node<T> trie = new Node<>();
    for (Map.Entry<String, T> entry : input.entrySet()) {
      String name = entry.getKey();
      Node<T> node = trie;
      for (int i = 0; i < name.length(); i++) {
        char ch = name.charAt(i);
        Node<T> next = node.children.get(ch);
        if (next == null) {
          next = new Node<>();
          node.children.put(ch, next);
        }
        if (i == name.length() - 1 && Characters.isWord(name.charAt(i))) next.needBoundary = true;
        node = next;
      }
      node.data = entry.getValue();
      node.end = true;
    }
    synchronized (CACHE) {
      CACHE.put(input, trie);
    }
    return trie;
  }
}
