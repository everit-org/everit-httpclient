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
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * An <a href="http://tools.ietf.org/html/rfc2045">RFC 2045</a> Media Type, appropriate to describe
 * the content type of an HTTP request or response body.
 */
public final class MediaType {
  private static final Pattern PARAMETER;

  private static final int PARAMETER_MATCHER_GROUP_CHARSET = 3;

  private static final String QUOTED = "\"([^\"]*)\"";

  private static final String TOKEN = "([a-zA-Z0-9-!#$%&'*+.^_`{|}~]+)";

  private static final Pattern TYPE_SUBTYPE =
      Pattern.compile(MediaType.TOKEN + "/" + MediaType.TOKEN);

  static {
    PARAMETER = Pattern.compile(
        ";\\s*(?:" + MediaType.TOKEN + "=(?:" + MediaType.TOKEN + "|" + MediaType.QUOTED + "))?");
  }

  private static void checkOnlyOneCharsetDefined(String string, Optional<String> charset,
      String charsetParameter) {
    if (charset.isPresent() && !charsetParameter.equalsIgnoreCase(charset.get())) {
      throw new IllegalArgumentException("Multiple charsets defined: \""
          + charset
          + "\" and: \""
          + charsetParameter
          + "\" for: \""
          + string
          + '"');
    }
  }

  /**
   * Returns a media type for {@code string}.
   *
   * @throws IllegalArgumentException
   *           if {@code string} is not a well-formed media type.
   */
  public static MediaType parse(String string) {
    Matcher typeSubtype = MediaType.TYPE_SUBTYPE.matcher(string);
    if (!typeSubtype.lookingAt()) {
      throw new IllegalArgumentException("No subtype found for: \"" + string + '"');
    }
    String type = typeSubtype.group(1).toLowerCase(Locale.US);
    String subtype = typeSubtype.group(2).toLowerCase(Locale.US);

    Optional<String> charset = Optional.empty();
    Matcher parameter = MediaType.PARAMETER.matcher(string);
    for (int s = typeSubtype.end(); s < string.length(); s = parameter.end()) {
      parameter.region(s, string.length());
      if (!parameter.lookingAt()) {
        throw new IllegalArgumentException("Parameter is not formatted correctly: \""
            + string.substring(s)
            + "\" for: \""
            + string
            + '"');
      }

      String name = parameter.group(1);
      if (name == null || !"charset".equalsIgnoreCase(name)) {
        continue;
      }
      String charsetParameter;
      String token = parameter.group(2);
      if (token != null) {
        // If the token is 'single-quoted' it's invalid! But we're lenient and strip the quotes.
        charsetParameter = token.startsWith("'") && token.endsWith("'") && token.length() > 2
            ? token.substring(1, token.length() - 1)
            : token;
      } else {
        // Value is "double-quoted". That's valid and our regex group already strips the quotes.
        charsetParameter = parameter.group(MediaType.PARAMETER_MATCHER_GROUP_CHARSET);
      }
      MediaType.checkOnlyOneCharsetDefined(string, charset, charsetParameter);
      charset = Optional.ofNullable(charsetParameter);
    }

    return new MediaType(string, type, subtype, charset);
  }

  private final Optional<String> charset;

  private final String mediaType;

  private final String subtype;

  private final String type;

  private MediaType(String mediaType, String type, String subtype, Optional<String> charset) {
    this.mediaType = mediaType;
    this.type = type;
    this.subtype = subtype;
    this.charset = charset;
  }

  /**
   * Returns the charset of this media type, or null if this media type doesn't specify a charset.
   */
  public Optional<Charset> charset() {
    return charset(null);
  }

  /**
   * Returns the charset of this media type, or {@code defaultValue} if either this media type
   * doesn't specify a charset, of it its charset is unsupported by the current runtime.
   */
  public Optional<Charset> charset(Optional<Charset> defaultValue) {
    return this.charset.isPresent() ? Optional.of(Charset.forName(this.charset.get()))
        : defaultValue;
  }

  @Override
  public boolean equals(Object other) {
    return other instanceof MediaType && ((MediaType) other).mediaType.equals(this.mediaType);
  }

  @Override
  public int hashCode() {
    return this.mediaType.hashCode();
  }

  /**
   * Returns a specific media subtype, such as "plain" or "png", "mpeg", "mp4" or "xml".
   */
  public String subtype() {
    return this.subtype;
  }

  /**
   * Returns the encoded media type, like "text/plain; charset=utf-8", appropriate for use in a
   * Content-Type header.
   */
  @Override
  public String toString() {
    return this.mediaType;
  }

  /**
   * Returns the high-level media type, such as "text", "image", "audio", "video", or "application".
   */
  public String type() {
    return this.type;
  }
}
