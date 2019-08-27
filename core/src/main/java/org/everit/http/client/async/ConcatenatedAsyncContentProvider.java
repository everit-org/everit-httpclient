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

import org.everit.http.client.MediaType;

/**
 * An {@link AsyncContentProvider} implementation that concatenates multiple
 * {@link AsyncContentProvider} instances. The success listener is called when the content of the
 * last wrapped provider is processed. In case the instance of this class is closed, all wrapped
 * content providers will be closed.
 */
public class ConcatenatedAsyncContentProvider extends AbstractAsyncContentProvider {

  private static void callTwoFunctionsWithErrorHandling(Runnable action1, Runnable action2) {
    Throwable error = null;
    try {
      action1.run();
    } catch (Throwable e) {
      error = e;
    } finally {
      try {
        action2.run();
      } catch (Throwable e) {
        if (error != null) {
          error.addSuppressed(e);
        } else {
          error = e;
        }

        if (error instanceof RuntimeException) {
          throw (RuntimeException) error;
        }
        if (error instanceof Error) {
          throw (Error) error;
        }
        throw new RuntimeException(error);
      }
    }
  }

  private final AsyncContentProvider[] contentProviders;

  private final Optional<MediaType> contentType;

  private AsyncCallback lastCallback = null;

  private ByteBuffer lastChunk = null;

  private Consumer<ByteBuffer> lastProvideCallback = null;

  private final Optional<Long> length;

  private final Object mutex = new Object();

  private int positionOfNextProvider = 0;

  /**
   * Constructor.
   *
   * @param contentType
   *          The optional content type that this instance provides. See {@link #getContentType()}.
   * @param contentProviders
   *          The providers whose processing are concatenated with each other.
   */
  public ConcatenatedAsyncContentProvider(Optional<MediaType> contentType,
      AsyncContentProvider... contentProviders) {
    this.contentType = contentType;
    this.contentProviders = contentProviders.clone();
    this.length = calculateLength();

    registerNextContentProvider();
  }

  private Optional<Long> calculateLength() {
    long sumLength = 0;
    for (AsyncContentProvider contentProvider : this.contentProviders) {
      Optional<Long> contentLength = contentProvider.getContentLength();
      if (!contentLength.isPresent()) {
        return Optional.empty();
      }
      sumLength += contentLength.get();
    }

    return Optional.of(sumLength);
  }

  @Override
  protected void doClose() {
    Throwable error = null;
    for (AsyncContentProvider contentProvider : this.contentProviders) {
      try {
        contentProvider.close();
      } catch (Throwable e) {
        if (error == null) {
          error = e;
        } else {
          error.addSuppressed(e);
        }
      }
    }
    if (error != null) {
      if (error instanceof RuntimeException) {
        throw (RuntimeException) error;
      }
      if (error instanceof Error) {
        throw (Error) error;
      }
      throw new RuntimeException(error);
    }
  }

  @Override
  public Optional<Long> getContentLength() {
    return this.length;
  }

  @Override
  public Optional<MediaType> getContentType() {
    return this.contentType;
  }

  @Override
  protected void provideNextChunk(Consumer<ByteBuffer> provideCallback) {
    ByteBuffer tmpLastChunk;
    AsyncCallback tmpCallback;

    synchronized (this.mutex) {
      tmpLastChunk = this.lastChunk;
      if (tmpLastChunk == null) {
        this.lastProvideCallback = provideCallback;
        return;
      }
      this.lastChunk = null;

      tmpCallback = this.lastCallback;
      this.lastCallback = null;
    }

    ConcatenatedAsyncContentProvider.callTwoFunctionsWithErrorHandling(
        () -> provideCallback.accept(tmpLastChunk),
        tmpCallback::processed);
  }

  private void registerNextContentProvider() {
    if (this.positionOfNextProvider == this.contentProviders.length) {
      handleSuccess();
      return;
    }

    AsyncContentProvider contentProvider = this.contentProviders[this.positionOfNextProvider];
    this.positionOfNextProvider++;

    contentProvider.onSuccess(this::registerNextContentProvider).onError(this::handleErrorFromInput)
        .onContent(new AsyncContentListener() {

          @Override
          public void onContent(ByteBuffer content, AsyncCallback callback) {
            Consumer<ByteBuffer> tmpLastProvideCallback;

            synchronized (ConcatenatedAsyncContentProvider.this.mutex) {
              tmpLastProvideCallback =
                  ConcatenatedAsyncContentProvider.this.lastProvideCallback;

              if (tmpLastProvideCallback == null) {
                ConcatenatedAsyncContentProvider.this.lastCallback = callback;
                ConcatenatedAsyncContentProvider.this.lastChunk = content;
                return;
              }

              ConcatenatedAsyncContentProvider.this.lastProvideCallback = null;
            }

            ConcatenatedAsyncContentProvider.callTwoFunctionsWithErrorHandling(
                () -> tmpLastProvideCallback.accept(content), callback::processed);
          }
        });
  }

}
