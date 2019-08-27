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
package org.everit.http.client.testbase;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import org.everit.http.client.MediaType;
import org.everit.http.client.async.AbstractAsyncContentProvider;
import org.everit.http.client.async.AsyncContentProvider;
import org.everit.http.client.async.ByteArrayAsyncContentProvider;
import org.everit.http.client.async.ConcatenatedAsyncContentProvider;

/**
 * Helper provider implementation for tests that will call the listener registered via
 * {@link AsyncContentProvider#onContent(org.everit.http.client.async.AsyncContentListener)} as many
 * times as many byte arrays are passed via the constructor.
 */
public class ChunkedAsyncContentProvider extends ConcatenatedAsyncContentProvider {

  private static AsyncContentProvider[] convertChunksToContentProviders(byte[][] chunks,
      boolean failOnEnd) {

    List<AsyncContentProvider> providers = new ArrayList<>();

    for (byte[] chunk : chunks) {
      providers.add(new ByteArrayAsyncContentProvider(chunk, Optional.empty()));
    }

    if (failOnEnd) {
      providers.add(new AbstractAsyncContentProvider() {

        @Override
        protected void doClose() {
          // Do nothing
        }

        @Override
        public Optional<Long> getContentLength() {
          return Optional.empty();
        }

        @Override
        public Optional<MediaType> getContentType() {
          return Optional.empty();
        }

        @Override
        protected void provideNextChunk(Consumer<ByteBuffer> callback) {
          throw new BodySendException();
        }
      });
    }

    return providers.toArray(new AsyncContentProvider[providers.size()]);
  }

  public ChunkedAsyncContentProvider(byte[][] chunks, Optional<MediaType> contentType,
      boolean failOnEnd) {
    super(contentType,
        ChunkedAsyncContentProvider.convertChunksToContentProviders(chunks, failOnEnd));
  }
}
