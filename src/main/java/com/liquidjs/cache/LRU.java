/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 Jun Yang
 */
package com.liquidjs.cache;

import com.liquidjs.template.Template;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** A cache that drops the least recently read entry once it is over its limit. */
public class LRU implements LiquidCache {
  private static final class Node {
    final String key;
    List<Template> value;
    Node next;
    Node prev;

    Node (String key, List<Template> value, Node next, Node prev) {
      this.key = key;
      this.value = value;
      this.next = next;
      this.prev = prev;
    }
  }

  private final Map<String, Node> cache = new HashMap<>();
  private final Node head = new Node("HEAD", null, null, null);
  private final Node tail = new Node("TAIL", null, null, null);
  public final int limit;
  public int size;

  public LRU (int limit) {
    this(limit, 0);
  }

  public LRU (int limit, int size) {
    this.limit = limit;
    this.size = size;
    this.head.next = this.tail;
    this.tail.prev = this.head;
  }

  @Override
  public void write (String key, List<Template> value) {
    Node existing = cache.get(key);
    if (existing != null) {
      existing.value = value;
      return;
    }
    Node node = new Node(key, value, head.next, head);
    head.next.prev = node;
    head.next = node;
    cache.put(key, node);
    size++;
    ensureLimit();
  }

  @Override
  public List<Template> read (String key) {
    Node node = cache.get(key);
    if (node == null) return null;
    List<Template> value = node.value;
    remove(key);
    write(key, value);
    return value;
  }

  @Override
  public void remove (String key) {
    Node node = cache.get(key);
    if (node == null) return;
    node.prev.next = node.next;
    node.next.prev = node.prev;
    cache.remove(key);
    size--;
  }

  public void clear () {
    head.next = tail;
    tail.prev = head;
    size = 0;
    cache.clear();
  }

  private void ensureLimit () {
    if (size > limit) remove(tail.prev.key);
  }
}
