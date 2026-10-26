/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 Jun Yang
 */
package com.liquidjs.drop;

import com.liquidjs.emitters.Emitter;
import com.liquidjs.emitters.SimpleEmitter;
import com.liquidjs.js.Js;

import java.util.function.Consumer;

/** The block a layout's block tag exposes, so a child block can reach its parent. */
public class BlockDrop extends Drop {
  private final Consumer<Emitter> superBlockRender;

  /** A block with no parent block to reach. */
  public BlockDrop () {
    this(emitter -> { });
  }

  /** A block whose parent block renders into the emitter it is given. */
  public BlockDrop (Consumer<Emitter> superBlockRender) {
    this.superBlockRender = superBlockRender;
  }

  /** The parent block's output, reachable in a child block as block.super */
  public Object superBlock () {
    SimpleEmitter emitter = new SimpleEmitter();
    superBlockRender.accept(emitter);
    return emitter.buffer;
  }

  @Override
  public Object getProperty (String key) {
    if ("super".equals(key)) return superBlock();
    return Js.UNDEFINED;
  }
}
