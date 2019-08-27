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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Optional;

import javax.servlet.MultipartConfigElement;

import org.apache.commons.io.IOUtils;
import org.eclipse.jetty.http.MultiPartFormInputStream;
import org.everit.http.client.async.AsyncContentUtil;
import org.everit.http.client.async.ByteArrayAsyncContentProvider;
import org.junit.Assert;
import org.junit.Test;

public class MultipartTest {

  @Test
  public void testMultipart() throws IOException {
    MultiPartAsyncContentProvider contentProvider =
        MultiPartAsyncContentProvider.create(Optional.empty(),
            new Part("a1", Optional.of("x.txt"), Optional.of("text/plain"),
                new ByteArrayAsyncContentProvider("1".getBytes(), Optional.empty()),
                Collections.emptyMap()),
            new Part("a2", Optional.of("x.json"), Optional.of("application/json"),
                new ByteArrayAsyncContentProvider("2".getBytes(), Optional.empty()),
                Collections.emptyMap()));

    byte[] multipartBytes = AsyncContentUtil.readAllBytes(contentProvider).blockingGet();
    ByteArrayInputStream is = new ByteArrayInputStream(multipartBytes);

    MultiPartFormInputStream multipartIS = new MultiPartFormInputStream(is,
        contentProvider.getContentType().get().toString(), new MultipartConfigElement(""), null);

    Collection<javax.servlet.http.Part> parts = multipartIS.getParts();
    Iterator<javax.servlet.http.Part> partIterator = parts.iterator();

    javax.servlet.http.Part part = partIterator.next();
    Assert.assertEquals("a1", part.getName());
    Assert.assertEquals("x.txt", part.getSubmittedFileName());
    Assert.assertEquals("text/plain", part.getContentType());
    Assert.assertEquals("1", IOUtils.toString(part.getInputStream(), StandardCharsets.UTF_8));

    part = partIterator.next();
    Assert.assertEquals("a2", part.getName());
    Assert.assertEquals("x.json", part.getSubmittedFileName());
    Assert.assertEquals("application/json", part.getContentType());
    Assert.assertEquals("2", IOUtils.toString(part.getInputStream(), StandardCharsets.UTF_8));
  }
}
