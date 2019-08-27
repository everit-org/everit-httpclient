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

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import org.everit.http.client.async.AsyncContentProvider;
import org.everit.http.client.async.ByteArrayAsyncContentProvider;

/**
 * One part from a multipart request.
 */
public class Part {

  private static final String HEADER_CONTENT_TYPE = "Content-Type";

  /**
   * Creates a simple text/plain field that can be part of the multipart request.
   *
   * @param name
   *          The name of the field.
   * @param value
   *          The value of the field.
   * @return The new part.
   */
  public static Part createFieldPart(String name, AsyncContentProvider value) {
    return new Part(name, Optional.empty(), Optional.empty(), value, Collections.emptyMap());
  }

  /**
   * Creates a simple text/plain field that can be part of the multipart request.
   *
   * @param name
   *          The name of the field.
   * @param value
   *          The value of the field.
   * @return The new part.
   */
  public static Part createFieldPart(String name, String value) {
    return Part.createFieldPart(name, new ByteArrayAsyncContentProvider(
        value.getBytes(StandardCharsets.UTF_8), Optional.empty()));
  }

  private final AsyncContentProvider content;

  private final Optional<String> contentType;

  private final Map<String, String> fields;

  private final Optional<String> fileName;

  private final String headers;

  private final String name;

  /**
   * Constructor.
   *
   * @param name
   *          Name of the field.
   * @param fileName
   *          Optional name of the file that is sent up. In case a simple field is sent, this may be
   *          {@link Optional#empty()}. This is the same as specifying the <i>filename</i> key-value
   *          pair in the <i>fields</i> parameter.
   * @param contentType
   *          Optional content type of the field/file. The final content type of the field is
   *          derived with the following algorythm:
   *          <ul>
   *          <li>if this parameter is specified, it is used, othewise</li>
   *          <li>if the <i>Content-Type</i> field is specified in the <i>fields</i> parameter, that
   *          is used. Otherwise</li>
   *          <li>if the {@link AsyncContentProvider#getContentType()} function of the content
   *          parameter returns a value, that is used. Otherwise</li>
   *          <li><i>text/plain</i> is used.</li>
   *          </ul>
   * @param content
   *          The content of this multipart value.
   * @param fields
   *          Additional fields of this multipart value. It is recommended to use only ISO8859-1
   *          characters in field names and values.
   */
  public Part(String name, Optional<String> fileName, Optional<String> contentType,
      AsyncContentProvider content,
      Map<String, String> fields) {
    this.name = name;
    this.fileName = fileName;
    this.contentType = contentType;
    this.content = content;
    this.fields = fields;
    this.headers = headers();
  }

  public AsyncContentProvider getContent() {
    return this.content;
  }

  public String getHeaders() {
    return this.headers;
  }

  private String headers() {
    // Compute the Content-Disposition.
    String contentDisposition = "Content-Disposition: form-data; name=\"" + this.name + "\"";
    if (this.fileName.isPresent()) {
      contentDisposition += "; filename=\"" + this.fileName.get() + "\"";
    }
    contentDisposition += "\r\n";

    String contentType = resolveContentTypeHeader();

    if (this.fields.isEmpty()) {
      String headers = contentDisposition;
      headers += contentType;
      headers += "\r\n";
      return headers;
    }

    StringBuilder sb =
        new StringBuilder();
    sb.append(contentDisposition);
    sb.append(contentType);
    for (Entry<String, String> field : this.fields.entrySet()) {
      if (Part.HEADER_CONTENT_TYPE.equals(field.getKey())) {
        continue;
      }
      sb.append(field.getKey());
      sb.append(": ");
      String value = field.getValue();
      if (value != null) {
        sb.append(value);
      }
      sb.append("\r\n");
    }
    sb.append("\r\n");
    return sb.toString();

  }

  private String resolveContentTypeHeader() {
    // Compute the Content-Type.
    String contentType = null;
    if (this.contentType.isPresent()) {
      contentType = this.contentType.get();
    }
    if (contentType == null) {
      contentType = this.fields.get(Part.HEADER_CONTENT_TYPE);
    }
    if (contentType == null && this.content.getContentType().isPresent()) {
      contentType = this.content.getContentType().get().toString();
    }
    if (contentType == null) {
      contentType = "text/plain";
    }

    contentType = "Content-Type: " + contentType + "\r\n";
    return contentType;
  }

  @Override
  public String toString() {
    return String.format("%s@%x[name=%s,fileName=%s,length=%d,headers=%s]",
        getClass().getSimpleName(),
        hashCode(),
        this.name,
        this.fileName,
        this.content.getContentLength(),
        this.fields);
  }
}
