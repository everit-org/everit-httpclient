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

/**
 * Listener interface that the programmer must implement in order to be able to receive data from an
 * {@link AsyncContentProvider}.
 */
public interface AsyncContentListener {

  /**
   * Called by the {@link AsyncContentProvider} when new chunk of data has arrived.
   *
   * @param content
   *          The chunk of data.
   * @param callback
   *          The implementation of the function must call {@link AsyncCallback#processed()} to
   *          notify the {@link AsyncContentProvider} that the next chunk of data can be pulled or
   *          {@link AsyncCallback#failed(Throwable)} to notify the {@link AsyncContentProvider}
   *          that the processing of the data is failed and no more data should be sent.
   */
  void onContent(ByteBuffer content, AsyncCallback callback);
}
