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
package org.everit.http.client;

import java.io.Closeable;

import io.reactivex.Single;

/**
 * API of sending asynchronous HTTP calls.
 */
public interface HttpClient extends Closeable {

  @Override
  void close();

  /**
   * Sends an HTTP call.
   *
   * @param request
   *          The HTTP request to be sent somewhen in the future.
   * @return The HTTP response {@link Single}.
   */
  Single<HttpResponse> send(HttpRequest request);
}
