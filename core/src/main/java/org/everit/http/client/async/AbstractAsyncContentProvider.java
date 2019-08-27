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
import java.util.Optional;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Helper class that makes it easier to implement Async content providers by having the complete and
 * error handling functionality.
 *
 * Subclasses of this class should call {@link #handleErrorFromInput(Throwable)} in case an error
 * happened during reading from the dataSource and call {@link #handleSuccess()} when the end of the
 * stream is reached successfully.
 */
public abstract class AbstractAsyncContentProvider implements AsyncContentProvider {

  /**
   * Helper interface to ease the implementation of an {@link AsyncContentProvider}. Subclasses of
   * {@link AbstractAsyncContentProvider} must not handle failures via the callback of
   * {@link AsyncContentListener#onContent(ByteBuffer, AsyncCallback)}, instead they should call
   * {@link AbstractAsyncContentProvider#handleErrorFromInput(Throwable)}.
   */
  protected interface OnlyProcessedHandlingAsyncContentListener {

    void onContent(ByteBuffer content, Runnable processedCallback);
  }

  private static final Logger LOGGER =
      Logger.getLogger(AbstractAsyncContentProvider.class.getName());

  private boolean closed = false;

  private AsyncContentListener contentListener;

  private Throwable error;

  private Consumer<Throwable> errorAction;

  private boolean finished = false;

  private Runnable successAction;

  private void callErrorListenerIfNecessary() {
    if (this.error != null && this.errorAction != null) {
      try {
        this.errorAction.accept(this.error);
      } catch (Throwable e) {
        AbstractAsyncContentProvider.LOGGER.log(Level.SEVERE, "Error during handling exception", e);
      }
    }
  }

  private void callSuccessListenerIfNecessary() {
    if (this.error == null && this.finished && this.successAction != null) {
      try {
        this.successAction.run();
      } catch (Throwable e) {
        this.error = e;
        callErrorListenerIfNecessary();
      }
    }

  }

  @Override
  public void close() {
    if (this.closed) {
      return;
    }

    try {
      doClose();
    } finally {
      this.closed = true;
    }
  }

  protected abstract void doClose();

  @Override
  public final Optional<Throwable> getFailure() {
    return Optional.ofNullable(this.error);
  }

  protected void handleErrorFromInput(Throwable failure) {
    this.error = failure;
    callErrorListenerIfNecessary();
  }

  protected void handleSuccess() {
    this.finished = true;
    callSuccessListenerIfNecessary();
  }

  @Override
  public final boolean isClosed() {
    return this.closed;
  }

  private void nextChunkCallback(ByteBuffer chunk) {
    this.contentListener.onContent(chunk, new AsyncCallback() {

      @Override
      public void failed(Throwable e) {
        AbstractAsyncContentProvider.this.error = e;
        callErrorListenerIfNecessary();
      }

      @Override
      public void processed() {
        provideNextChunkHandleError(AbstractAsyncContentProvider.this::nextChunkCallback);
      }
    });
  }

  @Override
  public final AsyncContentProvider onContent(AsyncContentListener listener) {
    if (this.closed) {
      throw new IllegalStateException("This provider is already closed");
    }

    this.contentListener = listener;

    provideNextChunkHandleError(this::nextChunkCallback);

    return this;
  }

  @Override
  public final AsyncContentProvider onError(Consumer<Throwable> action) {
    if (this.closed) {
      throw new IllegalStateException("This provider is already closed");
    }
    this.errorAction = action;
    callErrorListenerIfNecessary();
    return this;
  }

  @Override
  public final AsyncContentProvider onSuccess(Runnable action) {
    if (this.closed) {
      throw new IllegalStateException("This provider is already closed");
    }
    this.successAction = action;
    callSuccessListenerIfNecessary();
    return this;
  }

  protected abstract void provideNextChunk(Consumer<ByteBuffer> callback);

  private void provideNextChunkHandleError(Consumer<ByteBuffer> callback) {
    try {
      provideNextChunk(callback);
    } catch (Throwable e) {
      handleErrorFromInput(e);
    }
  }
}
