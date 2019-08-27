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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.StandardOpenOption;
import java.util.Optional;

import org.apache.commons.io.IOUtils;
import org.everit.http.client.MediaType;
import org.everit.http.client.MultipartTest;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import io.reactivex.Single;

public class AsyncContentProviderImplementationsTest {

  private static File testFile;

  @AfterClass
  public static void afterClass() {
    AsyncContentProviderImplementationsTest.testFile.delete();
  }

  @BeforeClass
  public static void beforeClass() {
    URL testFileResource = MultipartTest.class.getClassLoader().getResource("META-INF/test.txt");
    try {
      AsyncContentProviderImplementationsTest.testFile =
          File.createTempFile("everit-test-async", "");
      try (InputStream in = testFileResource.openStream();
          OutputStream out =
              new FileOutputStream(AsyncContentProviderImplementationsTest.testFile)) {

        IOUtils.copy(in, out);
      }
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private byte[] readTestFile() {
    try (InputStream in = new FileInputStream(AsyncContentProviderImplementationsTest.testFile)) {
      return IOUtils.toByteArray(in);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  @Test
  public void testConcatenatedAsyncContentProvider() {
    ConcatenatedAsyncContentProvider provider = new ConcatenatedAsyncContentProvider(
        Optional.of(MediaType.parse("text/plain")),
        new ByteArrayAsyncContentProvider("abc".getBytes(StandardCharsets.UTF_8), Optional.empty()),
        new ByteArrayAsyncContentProvider("def".getBytes(StandardCharsets.UTF_8), Optional.empty()),
        new ByteArrayAsyncContentProvider("ghi".getBytes(StandardCharsets.UTF_8),
            Optional.empty()));

    Single<byte[]> single = AsyncContentUtil.readAllBytes(provider);
    byte[] result = single.blockingGet();
    String resultString = new String(result, StandardCharsets.UTF_8);
    Assert.assertEquals("abcdefghi", resultString);
  }

  @Test
  public void testInputStreamAsyncContentProvider() {
    try (AsyncContentProvider provider = new InputStreamAsyncContentProvider(
        new FileInputStream(AsyncContentProviderImplementationsTest.testFile), Optional.empty(),
        Optional.empty(), 8096)) {

      byte[] bytes = AsyncContentUtil.readAllBytes(provider).blockingGet();
      Assert.assertArrayEquals(readTestFile(), bytes);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  @Test
  public void testReadableByteChannelAsyncContentProvider() {
    try (AsyncContentProvider provider = new ReadableByteChannelAsyncContentProvider(
        FileChannel.open(AsyncContentProviderImplementationsTest.testFile.toPath(),
            StandardOpenOption.READ),
        Optional.empty(), Optional.empty(), 8096)) {

      byte[] bytes = AsyncContentUtil.readAllBytes(provider).blockingGet();
      Assert.assertArrayEquals(readTestFile(), bytes);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
