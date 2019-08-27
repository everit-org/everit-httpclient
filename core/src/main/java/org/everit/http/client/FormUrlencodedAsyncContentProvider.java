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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import org.everit.http.client.async.ByteArrayAsyncContentProvider;

/**
 * Helper class to send HTTP requests with application/x-www-form-urlencoded content type.
 */
public class FormUrlencodedAsyncContentProvider extends ByteArrayAsyncContentProvider {

  private static byte[] convertParametersToByteArray(Map<String, Collection<String>> parameters) {
    ByteArrayOutputStream bout = new ByteArrayOutputStream();
    for (Entry<String, Collection<String>> parameter : parameters.entrySet()) {
      Collection<String> values = parameter.getValue();

      try {
        String key = parameter.getKey();
        if (values == null || values.isEmpty()) {
          bout.write(
              (URLEncoder.encode(key, "UTF-8") + '=').getBytes(StandardCharsets.UTF_8));
        } else {
          for (String value : values) {
            bout.write(
                (URLEncoder.encode(key, "UTF-8") + '=' + URLEncoder.encode(value, "UTF-8"))
                    .getBytes(StandardCharsets.UTF_8));
          }
        }
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }
    }
    return bout.toByteArray();
  }

  /**
   * Constructor.
   *
   * @param parameters
   *          The parameters that are passed via the form body.
   */
  public FormUrlencodedAsyncContentProvider(Map<String, Collection<String>> parameters) {
    super(FormUrlencodedAsyncContentProvider.convertParametersToByteArray(parameters),
        Optional.of(MediaType.parse("application/x-www-form-urlencoded;charset=UTF-8")));
  }

}
