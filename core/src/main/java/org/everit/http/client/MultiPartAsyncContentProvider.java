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

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;

import org.everit.http.client.async.AsyncContentProvider;
import org.everit.http.client.async.ByteArrayAsyncContentProvider;
import org.everit.http.client.async.ConcatenatedAsyncContentProvider;

/**
 * Helper class to send HTTP requests with <i>multipart/form-data</i> content type.
 */
public final class MultiPartAsyncContentProvider extends ConcatenatedAsyncContentProvider {

  /**
   * Constructor.
   *
   * @param boundary
   *          An optional boundary that is used in the HTTP request body to separate the different
   *          parts. If not specified, a random boundary is generated.
   * @param parts
   *          The set of parts that will be included in the body of the request.
   * @return The {@link AsyncContentProvider} instance.
   */
  public static MultiPartAsyncContentProvider create(Optional<String> boundary,
      Part... parts) {

    String finalBoundary;
    if (boundary.isPresent()) {
      finalBoundary = boundary.get();
    } else {
      finalBoundary = MultiPartAsyncContentProvider.makeBoundary();
    }

    AsyncContentProvider[] contentProviders =
        MultiPartAsyncContentProvider.resolveContentProviders(parts, finalBoundary);

    return new MultiPartAsyncContentProvider(finalBoundary, contentProviders);
  }

  private static String makeBoundary() {
    Random random = new Random();
    StringBuilder builder = new StringBuilder("EveritHttpClientBoundary");
    int length = builder.length();
    final int lengthOfStringRepresentationOfLong = 16;
    while (builder.length() < length + lengthOfStringRepresentationOfLong) {
      long rnd = random.nextLong();
      final int radix = 36;
      builder.append(Long.toString(rnd < 0 ? -rnd : rnd, radix));
    }
    builder.setLength(length + lengthOfStringRepresentationOfLong);
    return builder.toString();
  }

  private static AsyncContentProvider[] resolveContentProviders(Part[] parts,
      String boundary) {
    Charset charset = StandardCharsets.US_ASCII;
    String onlyBoundaryLine = "--" + boundary + "--\r\n";

    if (parts.length == 0) {
      return new AsyncContentProvider[] {
          new ByteArrayAsyncContentProvider(onlyBoundaryLine.getBytes(charset), Optional.empty()) };
    }

    List<AsyncContentProvider> result = new ArrayList<>();

    String firstBoundaryLine = "--" + boundary + "\r\n";
    result.add(
        new ByteArrayAsyncContentProvider(firstBoundaryLine.getBytes(charset), Optional.empty()));

    for (int i = 0, n = parts.length; i < n; i++) {
      Part part = parts[i];
      result.add(
          new ByteArrayAsyncContentProvider(part.getHeaders().getBytes(StandardCharsets.UTF_8),
              Optional.empty()));

      result.add(part.getContent());

      if (i < n - 1) {
        String middleBoundaryLine = "\r\n" + firstBoundaryLine;
        result.add(
            new ByteArrayAsyncContentProvider(middleBoundaryLine.getBytes(charset),
                Optional.empty()));
      }
    }

    String lastBoundaryLine = "\r\n" + onlyBoundaryLine;
    result.add(
        new ByteArrayAsyncContentProvider(lastBoundaryLine.getBytes(charset),
            Optional.empty()));

    return result.toArray(new AsyncContentProvider[result.size()]);
  }

  private MultiPartAsyncContentProvider(String boundary, AsyncContentProvider... contentProviders) {
    super(Optional.of(MediaType.parse("multipart/form-data; boundary="
        + boundary)), contentProviders);

  }

}
