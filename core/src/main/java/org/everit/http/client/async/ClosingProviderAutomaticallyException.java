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
 * Helper class to let the onError listener know that the exception was thrown when success listener
 * was called successfully from the closing procedure, so closing in onError should not be
 * reexecuted.
 */
public class ClosingProviderAutomaticallyException extends RuntimeException {

  private static final long serialVersionUID = -4455576940915160997L;

  ClosingProviderAutomaticallyException(Throwable cause) {
    super(cause);
  }

}
