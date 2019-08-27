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
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.util.Optional;
import java.util.function.Consumer;

import org.everit.http.client.MediaType;

/**
 * Implementation of {@link AsyncContentProvider} that gets the reads from a
 * {@link ReadableByteChannel}.
 */
public class ReadableByteChannelAsyncContentProvider extends AbstractAsyncContentProvider {

  private ByteBuffer buffer;

  private final ReadableByteChannel byteChannel;

  private Optional<Long> contentLength;

  private Optional<MediaType> contentType;

  /**
   * Constructor.
   *
   * @param byteChannel
   *          The wrapped byte channel where the data is consumed from.
   * @param contentLength
   *          The amount of data that can be read from the byte channel if known.
   * @param contentType
   *          The content type of the data that can be read from the byte channel if known.
   * @param bufferSize
   *          The size of the buffer that is used to read from the byte channel.
   */
  public ReadableByteChannelAsyncContentProvider(ReadableByteChannel byteChannel,
      Optional<Long> contentLength,
      Optional<MediaType> contentType, int bufferSize) {
    this.byteChannel = byteChannel;
    this.contentLength = contentLength;
    this.contentType = contentType;
    this.buffer = ByteBuffer.allocateDirect(bufferSize);
  }

  @Override
  protected void doClose() {
    try {
      this.byteChannel.close();
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
      this.buffer.clear();
      read = this.byteChannel.read(this.buffer);
    } catch (Throwable e) {
      handleErrorFromInput(e);
      return;
    }

    if (read != null) {
      if (read <= 0) {
        handleSuccess();
      } else {
        this.buffer.flip();
        callback.accept(this.buffer);
      }
    }
  }

}
