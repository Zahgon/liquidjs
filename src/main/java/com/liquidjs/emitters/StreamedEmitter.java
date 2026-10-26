/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 Jun Yang
 */
package com.liquidjs.emitters;

import com.liquidjs.util.Underscore;

import java.io.IOException;
import java.io.PipedReader;
import java.io.PipedWriter;
import java.io.Reader;

/** Writes the output through a stream as it is produced, rather than collecting it. */
public class StreamedEmitter implements Emitter {
  public String buffer = "";
  private final PipedWriter writer = new PipedWriter();
  private final PipedReader reader;
  private IOException failure;

  public StreamedEmitter () {
    try {
      this.reader = new PipedReader(writer, 1 << 16);
    } catch (IOException err) {
      throw new IllegalStateException(err);
    }
  }

  /** The stream the output is written to. */
  public Reader getStream () {
    return reader;
  }

  @Override
  public void write (Object html) {
    try {
      writer.write(Underscore.stringify(html));
    } catch (IOException err) {
      this.failure = err;
    }
  }

  public void error (Throwable err) {
    end();
  }

  public void end () {
    try {
      writer.close();
    } catch (IOException err) {
      this.failure = err;
    }
  }

  @Override
  public Object getBuffer () {
    return buffer;
  }

  public IOException getFailure () {
    return failure;
  }
}
