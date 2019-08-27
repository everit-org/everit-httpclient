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

import java.net.HttpCookie;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import javax.annotation.Generated;

import org.everit.http.client.async.AsyncContentProvider;

/**
 * An HTTP request. Instances of this class are immutable if their {@link #body} is null or itself
 * immutable.
 */
public final class HttpRequest {

  /**
   * Builder to build {@link HttpRequest}.
   */
  @Generated("SparkTools")
  public static final class Builder {
    private Optional<AsyncContentProvider> body = Optional.empty();

    private Collection<HttpCookie> cookies = Collections.emptyList();

    private Map<String, String> headers = Collections.emptyMap();

    private HttpMethod method;

    private String url;

    private Builder() {
    }

    private Builder(HttpRequest httpRequest) {
      this.url = httpRequest.url;
      this.method = httpRequest.method;
      this.headers = httpRequest.headers;
      this.cookies = httpRequest.cookies;
      this.body = httpRequest.body;
    }

    /**
     * Optional body of the http request.
     */
    public Builder body(Optional<AsyncContentProvider> body) {
      this.body = Objects.requireNonNull(body);
      return this;
    }

    /**
     * Builder method of the builder.
     *
     * @return built class
     */
    public HttpRequest build() {
      return new HttpRequest(this);
    }

    /**
     * Cookies that are sent with the HTTP request.
     */
    public Builder cookies(Collection<HttpCookie> cookies) {
      this.cookies = Objects.requireNonNull(cookies);
      return this;
    }

    /**
     * Headers that are sent with the HTTP request.
     */
    public Builder headers(Map<String, String> headers) {
      this.headers = Objects.requireNonNull(headers);
      return this;
    }

    /**
     * Method of the HTTP request.
     */
    public Builder method(HttpMethod method) {
      this.method = Objects.requireNonNull(method);
      return this;
    }

    /**
     * URL with query params of the HTTP request.
     */
    public Builder url(String url) {
      this.url = Objects.requireNonNull(url);
      return this;
    }
  }

  /**
   * Creates builder to build {@link HttpRequest}.
   *
   * @return created builder
   */
  @Generated("SparkTools")
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Creates a builder to build {@link HttpRequest} and initialize it with the given object.
   *
   * @param httpRequest
   *          to initialize the builder with
   * @return created builder
   */
  @Generated("SparkTools")
  public static Builder builderFrom(HttpRequest httpRequest) {
    return new Builder(httpRequest);
  }

  private final Optional<AsyncContentProvider> body;

  private final Collection<HttpCookie> cookies;

  private final Map<String, String> headers;

  private final HttpMethod method;

  private final String url;

  @Generated("SparkTools")
  private HttpRequest(Builder builder) {
    this.url = Objects.requireNonNull(builder.url);
    this.method = builder.method;
    this.headers = Collections.unmodifiableMap(new HashMap<>(builder.headers));
    this.cookies = Collections.unmodifiableList(new ArrayList<>(builder.cookies));
    this.body = builder.body;
  }

  /**
   * Optional body of the http request.
   */
  public Optional<AsyncContentProvider> getBody() {
    return this.body;
  }

  /**
   * Cookies that are sent with the HTTP request.
   */
  public Collection<HttpCookie> getCookies() {
    return this.cookies;
  }

  /**
   * Headers that are sent with the HTTP request.
   */
  public Map<String, String> getHeaders() {
    return this.headers;
  }

  /**
   * Method of the HTTP request.
   */
  public HttpMethod getMethod() {
    return this.method;
  }

  /**
   * URL with query params of the HTTP request.
   */
  public String getUrl() {
    return this.url;
  }
}
