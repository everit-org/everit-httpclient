/*
 * Copyright © 2011 Everit Kft. (http://www.everit.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.everit.http.client.async;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

import org.everit.http.client.MediaType;

/**
 * Implementation of {@link AsyncContentProvider} that gets the reads from an {@link InputStream}.
 */
public class InputStreamAsyncContentProvider extends AbstractAsyncContentProvider {

  private final byte[] buffer;

  private final Optional<Long> contentLength;

  private final Optional<MediaType> contentType;

  private final InputStream in;

  /**
   * Constructor.
   *
   * @param in
   *          The wrapped input stream where the data is consumed from.
   * @param contentLength
   *          The amount of data that can be read from the input stream if known.
   * @param contentType
   *          The content type of the data that can be read from the input stream if known.
   * @param bufferSize
   *          The size of the buffer that is used to read from the input stream.
   */
  public InputStreamAsyncContentProvider(InputStream in, Optional<Long> contentLength,
      Optional<MediaType> contentType, int bufferSize) {

    Objects.requireNonNull(in);
    Objects.requireNonNull(contentLength);
    Objects.requireNonNull(contentType);
    if (bufferSize <= 0) {
      throw new IllegalArgumentException("Buffer size must be greater than zero");
    }
    this.in = in;
    this.contentLength = contentLength;
    this.contentType = contentType;
    this.buffer = new byte[bufferSize];
  }

  @Override
  protected void doClose() {
    try {
      this.in.close();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  @Override
  public Optional<Long> getContentLength() {
    return this.contentLength;
  }

  @Override
  public Optional<MediaType> getContentType() {
    return this.contentType;
  }

  @Override
  protected void provideNextChunk(Consumer<ByteBuffer> callback) {
    Integer read = null;
    try {
      read = this.in.read(this.buffer);
    } catch (IOException e) {
      handleErrorFromInput(e);
      return;
    }

    if (read != null) {
      if (read >= 0) {
        callback.accept(ByteBuffer.wrap(Arrays.copyOf(this.buffer, read)));
      } else {
        handleSuccess();
      }
    }
  }

}
