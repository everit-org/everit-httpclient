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

import java.io.Closeable;
import java.util.Optional;
import java.util.function.Consumer;

import org.everit.http.client.MediaType;

/**
 * Provides content asynchronously. See the functions of {@link AsyncContentUtil} to create standard
 * providers.
 */
public interface AsyncContentProvider extends Closeable {

  /**
   * Closing the content provider. If already closed, nothing happens.
   */
  @Override
  void close();

  /**
   * The size of the data that is provided or {@link Optional#empty()} if the length is unknown in
   * advance.
   */
  Optional<Long> getContentLength();

  /**
   * The media type of the content or {@link Optional#empty()} if the media type is unknown.
   */
  Optional<MediaType> getContentType();

  /**
   * Returns the error that has happened during processing the content of this provider or
   * {@link Optional#empty()} if there was no failure.
   */
  Optional<Throwable> getFailure();

  /**
   * Returns whether this provider is closed or not.
   */
  boolean isClosed();

  /**
   * Registering a listener that will be notified if new content is available.
   *
   * @param listener
   *          The listener that is called when new data chunk is available.
   * @return The current {@link AsyncContentProvider}.
   */
  AsyncContentProvider onContent(AsyncContentListener listener);

  /**
   * Registering a listener that is called if there is an error delivering the data or processing of
   * the data is failed either via {@link AsyncCallback#failed(Throwable)} call or via an error
   * during calling the registered listener of {@link #onContent(AsyncContentListener)} or
   * {@link #onSuccess(Runnable)} functions.
   *
   * @param action
   *          The action that must be executed if there is an error.
   * @return The current {@link AsyncContentProvider}.
   */
  AsyncContentProvider onError(Consumer<Throwable> action);

  /**
   * Registering an action that will be called when all of the chunks are delivered. In case of
   * error, this method is not called. If the action throws an error, the listener registered via
   * {@link #onError(Consumer)} is called with that exception.
   *
   * @param action
   *          The action that is called when all of the data chunks are delivered.
   * @return The current {@link AsyncContentProvider}.
   */
  AsyncContentProvider onSuccess(Runnable action);
}
