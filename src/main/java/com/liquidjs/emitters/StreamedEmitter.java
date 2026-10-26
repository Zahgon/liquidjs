/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 Jun Yang
 */
package com.liquidjs.emitters;

import com.liquidjs.util.Underscore;

import java.io.FilterReader;
import java.io.IOException;
import java.io.PipedReader;
import java.io.PipedWriter;
import java.io.Reader;

/**
 * Writes the output through a stream as it is produced, rather than collecting it.
 *
 * A render that fails part way through reports the failure to the reader: the stream ends and the
 * next read raises, carrying the error, so a caller reading the stream cannot mistake a failed
 * render for a short one.
 */
public class StreamedEmitter implements Emitter {
  public String buffer = "";
  private final PipedWriter writer = new PipedWriter();
  private final Reader reader;
  private volatile Throwable failure;

  public StreamedEmitter () {
    try {
      PipedReader pipe = new PipedReader(writer, 1 << 16);
      this.reader = new FailingReader(pipe);
    } catch (IOException err) {
      throw new IllegalStateException("the output stream could not be opened", err);
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

  /** Report a render failure to the reader of the stream, and end the stream. */
  public void error (Throwable err) {
    this.failure = err;
    end();
  }

  /** End the stream, so its reader sees the output it has. */
  public void end () {
    try {
      writer.close();
    } catch (IOException err) {
      if (this.failure == null) this.failure = err;
    }
  }

  @Override
  public Object getBuffer () {
    return buffer;
  }

  /** The failure the render reported, or null when it finished. */
  public Throwable getFailure () {
    return failure;
  }

  /** A reader that raises the render's failure once the output it did write has been read. */
  private final class FailingReader extends FilterReader {
    FailingReader (Reader in) {
      super(in);
    }

    @Override
    public int read () throws IOException {
      int value = super.read();
      if (value < 0) raise();
      return value;
    }

    @Override
    public int read (char[] buf, int off, int len) throws IOException {
      int count = super.read(buf, off, len);
      if (count < 0) raise();
      return count;
    }

    private void raise () throws IOException {
      Throwable err = failure;
      if (err == null) return;
      failure = null;
      throw new IOException(err.getMessage(), err);
    }
  }
}
