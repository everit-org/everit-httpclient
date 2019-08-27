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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;

import io.reactivex.Single;

/**
 * Helper functions to use the interfaces of the Async package.
 */
public final class AsyncContentUtil {

  /**
   * Reads all chunks from an {@link AsyncContentProvider} and returns the result as one byte array.
   *
   * @param asyncContentProvider
   *          The provider that is consumed.
   * @return A single instance that is notified when processing all chunks are ready.
   */
  public static Single<byte[]> readAllBytes(AsyncContentProvider asyncContentProvider) {

    ByteArrayOutputStream bout = new ByteArrayOutputStream();

    return Single.create((subscriber) -> {
      asyncContentProvider
          .onContent((buffer, callback) -> {
            byte[] b = new byte[buffer.remaining()];
            buffer.get(b);
            try {
              bout.write(b);
            } catch (IOException e) {
              throw new UncheckedIOException(e);
            }

            callback.processed();
          })
          .onSuccess(() -> subscriber.onSuccess(bout.toByteArray()))
          .onError(subscriber::onError);
    });

  }

  /**
   * Reads all chunks from a provider and converts them to string.
   *
   * @param asyncContentProvider
   *          The provider that is consumed.
   * @param charset
   *          The character encoding that is used when the byte array is converted to a string.
   * @return A single that is notified when all data chunks are read and converted into one string.
   */
  public static Single<String> readString(AsyncContentProvider asyncContentProvider,
      Charset charset) {

    return AsyncContentUtil.readAllBytes(asyncContentProvider).map((content) -> {
      return new String(content, charset);
    });
  }

  private AsyncContentUtil() {
  }
}
