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
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import javax.annotation.Generated;

import org.everit.http.client.async.AsyncContentProvider;

/**
 * An HTTP response that is returned as the result of a {@link HttpRequest}.
 */
public final class HttpResponse implements Closeable {

  /**
   * Builder to build {@link HttpResponse}.
   */
  @Generated("SparkTools")
  public static final class Builder {
    private AsyncContentProvider body;

    private Map<String, String> headers = Collections.emptyMap();

    private int status;

    private Builder() {
    }

    private Builder(HttpResponse httpResponse) {
      this.status = httpResponse.status;
      this.headers = Objects.requireNonNull(httpResponse.headers);
      this.body = Objects.requireNonNull(httpResponse.body);
    }

    /**
     * The body of the HTTP response.
     */
    public Builder body(AsyncContentProvider body) {
      this.body = Objects.requireNonNull(body);
      return this;
    }

    /**
     * The body of the HTTP response.
     */
    public HttpResponse build() {
      return new HttpResponse(this);
    }

    /**
     * The headers of the HTTP response.
     */
    public Builder headers(Map<String, String> headers) {
      this.headers = Objects.requireNonNull(headers);
      return this;
    }

    /**
     * The status of the HTTP response.
     */
    public Builder status(int status) {
      this.status = status;
      return this;
    }
  }

  /**
   * Creates builder to build {@link HttpResponse}.
   *
   * @return created builder
   */
  @Generated("SparkTools")
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Creates a builder to build {@link HttpResponse} and initialize it with the given object.
   *
   * @param httpResponse
   *          to initialize the builder with
   * @return created builder
   */
  @Generated("SparkTools")
  public static Builder builderFrom(HttpResponse httpResponse) {
    return new Builder(httpResponse);
  }

  private final AsyncContentProvider body;

  private boolean closed = false;

  private final Map<String, String> headers;

  private final int status;

  @Generated("SparkTools")
  private HttpResponse(Builder builder) {
    this.status = builder.status;
    this.headers = Collections.unmodifiableMap(new HashMap<>(builder.headers));
    this.body = Objects.requireNonNull(builder.body);
  }

  /**
   * Closes the body of this response. If the body content of the response is not read fully, an
   * {@link HttpResponseAbortException} is passed to the listener registered via
   * {@link AsyncContentProvider#onError(java.util.function.Consumer)}.
   */
  @Override
  public void close() {
    this.closed = true;
    this.body.close();
  }

  @Override
  protected void finalize() throws Throwable {
    if (!this.closed) {
      System.err.println(
          "HEY DEVELOPER! YOU FORGOT TO CLOSE YOUR " + this.getClass().getName() + " INSTANCE");
    }
  }

  /**
   * The body of the HTTP response.
   */
  public AsyncContentProvider getBody() {
    return this.body;
  }

  /**
   * The headers of the HTTP response.
   */
  public Map<String, String> getHeaders() {
    return this.headers;
  }

  /**
   * The status of the HTTP response.
   */
  public int getStatus() {
    return this.status;
  }
}
