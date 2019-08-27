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

/**
 * Allows the notification of an {@link AsyncContentProvider} to send the next chunk of data or if
 * processing the data failed.
 */
public interface AsyncCallback {

  /**
   * Notifies the {@link AsyncContentProvider} that the processing of the last chunk of data failed.
   * This also calls the listener registered via
   * {@link AsyncContentProvider#onError(java.util.function.Consumer)}.
   *
   * @param e
   *          The error that caused the failure.
   */
  void failed(Throwable e);

  /**
   * Notifies the {@link AsyncContentProvider} that the last chunk of data is processed, so the next
   * chunk can be delivered.
   */
  void processed();
}
