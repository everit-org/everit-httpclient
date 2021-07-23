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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import org.everit.http.client.MediaType;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * <p>
 * A wrapper around another {@link AsyncContentProvider}s that closes the provider instance and the
 * optionally specified additional {@link Closeable} objects when the reading from the provider is
 * completed. Completion means either {@link AsyncContentProvider#onSuccess(Runnable)} or
 * {@link AsyncContentProvider#onError(java.util.function.Consumer)} execution.
 * </p>
 *
 * <p>
 * Please note that if there an exception during closing the provider in onSuccess, an
 * {@link ClosingProviderAutomaticallyException} is received by the onError listener that the
 * programmer might only want to log as the success and its listener ran successfully.
 * </p>
 *
 * <p>
 * In case an exception is thrown during closing the provider and other resources in the onError
 * listener, it is only logged out.
 * </p>
 */
public class AutoCloseAsyncContentProvider implements AsyncContentProvider {

  private final Closeable[] additionalCloseables;

  private final AsyncContentProvider wrapped;

  /**
   * Constructor.
   *
   * @param wrapped
   *          The instance of this wrapper copies the functionality of the wrapped provider and in
   *          the end of process (success or fail) closes the wrapped provider automatically.
   * @param additionalCloseables
   *          Zero or more additional objects that implement {@link Closeable} interface and that
   *          should be closed in the end of processing the data of the wrapped provider (fail or
   *          success). In case of calling {@link #close()} on the
   *          {@link AutoCloseAsyncContentProvider} also closes all passed additiona closeable
   *          objects.
   */
  public AutoCloseAsyncContentProvider(AsyncContentProvider wrapped,
      Closeable... additionalCloseables) {

    this.wrapped = wrapped;
    this.additionalCloseables = additionalCloseables.clone();
  }

  @Override
  public void close() {
    List<Closeable> allCloseables = new ArrayList<>(this.additionalCloseables.length + 1);
    allCloseables.add(this.wrapped);
    allCloseables.addAll(Arrays.asList(this.additionalCloseables));

    Throwable error = null;

    for (Closeable closeable : allCloseables) {
      try {
        closeable.close();
      } catch (Throwable e) {
        if (error == null) {
          error = e;
        } else {
          error.addSuppressed(e);
        }
      }
    }
    if (error != null) {
      throwAsUnchecked(error);
    }
  }

  @Override
  public Optional<Long> getContentLength() {
    return this.wrapped.getContentLength();
  }

  @Override
  public Optional<MediaType> getContentType() {
    return this.wrapped.getContentType();
  }

  @Override
  public Optional<Throwable> getFailure() {
    return this.wrapped.getFailure();
  }

  @Override
  public boolean isClosed() {
    return this.wrapped.isClosed();
  }

  @Override
  @SuppressFBWarnings("RV_RETURN_VALUE_IGNORED_NO_SIDE_EFFECT")
  public AsyncContentProvider onContent(AsyncContentListener listener) {
    this.wrapped.onContent(listener);
    return this;
  }

  @Override
  public AsyncContentProvider onError(Consumer<Throwable> action) {
    this.wrapped.onError((error) -> {

      Throwable newError = null;
      try {
        action.accept(error);
      } catch (Throwable e) {
        newError = e;
      } finally {
        try {
          boolean alreadyClosed = error instanceof ClosingProviderAutomaticallyException;
          if (!alreadyClosed) {
            close();
          }
        } catch (Throwable e2) {
          if (newError == null) {
            newError = e2;
          } else {
            newError.addSuppressed(e2);
          }
        } finally {
          if (newError != null) {
            throwAsUnchecked(newError);
          }
        }
      }
    });
    return this;
  }

  @Override
  @SuppressFBWarnings("RV_RETURN_VALUE_IGNORED_NO_SIDE_EFFECT")
  public AsyncContentProvider onSuccess(Runnable action) {
    this.wrapped.onSuccess(() -> {
      action.run();
      try {
        close();
      } catch (Throwable e) {
        throw new ClosingProviderAutomaticallyException(e);
      }
    });
    return this;
  }

  private void throwAsUnchecked(Throwable error) {
    if (error instanceof RuntimeException) {
      throw (RuntimeException) error;
    }
    if (error instanceof Error) {
      throw (Error) error;
    }
    throw new RuntimeException(error);
  }

}
