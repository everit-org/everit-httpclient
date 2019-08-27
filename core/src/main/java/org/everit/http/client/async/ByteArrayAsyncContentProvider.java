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

import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import org.everit.http.client.MediaType;

/**
 * Implementation of {@link AsyncContentProvider} that wraps a byte array and provides it as one
 * chunk.
 */
public class ByteArrayAsyncContentProvider extends AbstractAsyncContentProvider {

  private byte[] content;

  private AtomicBoolean contentProvided = new AtomicBoolean();

  private Optional<MediaType> contentType;

  public ByteArrayAsyncContentProvider(byte[] content, Optional<MediaType> contentType) {
    this.contentType = contentType;
    this.content = Objects.requireNonNull(content);
  }

  @Override
  protected void doClose() {
    // Do nothing
  }

  @Override
  public Optional<Long> getContentLength() {
    return Optional.of((long) this.content.length);
  }

  @Override
  public Optional<MediaType> getContentType() {
    return this.contentType;
  }

  @Override
  protected void provideNextChunk(Consumer<ByteBuffer> callback) {
    if (this.contentProvided.getAndSet(true)) {
      handleSuccess();
      return;
    }

    callback.accept(ByteBuffer.wrap(this.content));
  }

}
