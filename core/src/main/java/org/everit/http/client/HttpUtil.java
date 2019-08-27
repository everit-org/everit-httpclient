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

import org.everit.http.client.async.AsyncContentProvider;

/**
 * Helper functions to use the library.
 */
public final class HttpUtil {

  /**
   * Closes the HTTP response gracefully by reading any body content that has not been read yet.
   *
   * @param response
   *          The HTTP response instance that should be closed.
   */
  public static void closeResponseGracefully(HttpResponse response) {
    AsyncContentProvider content = response.getBody();
    content.onSuccess(response::close);
    content.onContent((chunk, callback) -> callback.processed());
  }

  private HttpUtil() {
  }
}
