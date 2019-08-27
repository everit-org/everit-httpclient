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
package org.everit.http.client.testbase;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.NetworkConnector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.everit.http.client.FormUrlencodedAsyncContentProvider;
import org.everit.http.client.HttpClient;
import org.everit.http.client.HttpMethod;
import org.everit.http.client.HttpRequest;
import org.everit.http.client.HttpResponse;
import org.everit.http.client.async.AsyncContentUtil;
import org.json.JSONObject;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import io.reactivex.Single;

/**
 * JUnit tst class that contains generic tests for the HTTP client library.
 */
public abstract class HttpClientTest {

  private static final int BUFFER_SIZE = 8096;

  public static final String CONTEXT_PATH = "/test";

  private static final Logger LOGGER = Logger.getLogger(HttpClientTest.class.getName());

  private static int port;

  private static Server server;

  private static final HttpClientTestServlet TEST_SERVLET = new HttpClientTestServlet();

  /**
   * Stops the HTTP server that the tests communicate with.
   */
  @AfterClass
  public static void afterClass() {
    if (HttpClientTest.server != null) {
      try {
        HttpClientTest.server.stop();
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
  }

  /**
   * Starts up an HTTP server and registers {@link HttpClientTestServlet} on it to let the tests
   * communicate with the servlet.
   */
  @BeforeClass
  public static void beforeClass() {
    HttpClientTest.server = new Server();
    ServletContextHandler servletContextHandler =
        new ServletContextHandler(HttpClientTest.server, HttpClientTest.CONTEXT_PATH);
    servletContextHandler.addServlet(new ServletHolder(HttpClientTest.TEST_SERVLET), "/*");
    HttpClientTest.server.setHandler(servletContextHandler);
    ServerConnector serverConnector = new ServerConnector(HttpClientTest.server);
    final int thirtySecondsInMillisecs = 30000;
    serverConnector.setIdleTimeout(thirtySecondsInMillisecs);
    HttpClientTest.server.addConnector(serverConnector);
    try {
      HttpClientTest.server.start();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    Connector[] connectors = HttpClientTest.server.getConnectors();
    for (Connector connector : connectors) {
      if (connector instanceof NetworkConnector) {
        HttpClientTest.port = ((NetworkConnector) connector).getLocalPort();
      }
    }
  }

  private static String uriForPath(String path) {
    return "http://localhost:" + HttpClientTest.port + HttpClientTest.CONTEXT_PATH + path;
  }

  private HttpClient httpClient;

  /**
   * Closes the created {@link HttpClient} instance after running the test.
   */
  @After
  public void after() {
    if (this.httpClient != null) {
      this.httpClient.close();
    }
  }

  @Before
  public void before() {
    this.httpClient = createHttpClient();
  }

  protected abstract HttpClient createHttpClient();

  private byte[] createTestByteArray() {
    final int oneMegabyte = 1024 * 1024;
    byte[] result = new byte[oneMegabyte];
    for (int i = 0, n = result.length; i < n; i++) {
      final int byteValueRestartPoint = 128;
      result[i] = (byte) (i % byteValueRestartPoint);
    }
    return result;
  }

  private byte[][] createTestChunks(byte[] byteArray, int chunkSize) {
    List<byte[]> result = new ArrayList<>();
    int n = byteArray.length;
    int position = 0;
    while (position < n) {
      int currentChunkSize = n - position;
      if (currentChunkSize > chunkSize) {
        currentChunkSize = chunkSize;
      }
      byte[] chunk = new byte[currentChunkSize];
      System.arraycopy(byteArray, position, chunk, 0, currentChunkSize);
      result.add(chunk);
      position += currentChunkSize;
    }
    return result.toArray(new byte[0][]);
  }

  @Test
  public void testBody() {

    byte[] testByteArray = createTestByteArray();
    byte[][] chunks = createTestChunks(testByteArray, HttpClientTest.BUFFER_SIZE);

    Single<HttpResponse> single = this.httpClient.send(HttpRequest.builder().method(HttpMethod.GET)
        .url(HttpClientTest.uriForPath(HttpClientTestServlet.PATH_TEST_WITH_BODY))
        .body(Optional
            .of(new ChunkedAsyncContentProvider(chunks, Optional.empty(), false)))
        .build());

    try (HttpResponse response = single.blockingGet()) {
      Assert.assertEquals(Optional.of((long) testByteArray.length),
          response.getBody().getContentLength());

      Single<byte[]> bodySingle = AsyncContentUtil.readAllBytes(response.getBody());
      byte[] body = bodySingle.blockingGet();
      Assert.assertArrayEquals(testByteArray, body);
    }
  }

  @Test
  public void testBodyReceiveFailViaCallback() {

    byte[] testByteArray = createTestByteArray();
    byte[][] chunks = createTestChunks(testByteArray, HttpClientTest.BUFFER_SIZE);

    Single<HttpResponse> single = this.httpClient.send(HttpRequest.builder().method(HttpMethod.GET)
        .url(HttpClientTest.uriForPath(HttpClientTestServlet.PATH_TEST_WITH_BODY))
        .body(Optional
            .of(new ChunkedAsyncContentProvider(chunks, Optional.empty(), false)))
        .build());

    String errorMessage = "Error via callback";

    try (HttpResponse httpResponse = single.blockingGet()) {
      Single<byte[]> bodySingle = Single.create((emitter) -> {
        httpResponse.getBody().onSuccess(() -> emitter.onSuccess(new byte[0]))
            .onContent((content, callback) -> {

              callback.failed(new RuntimeException(errorMessage));
            })
            .onError((error) -> emitter.onError(error));
      });

      try {
        byte[] body = bodySingle.blockingGet();
        Assert.fail(
            "Exception should have been thrown, instead body arrived: " + Arrays.toString(body));
      } catch (RuntimeException e) {
        Assert.assertEquals(errorMessage, e.getMessage());
      }
    }
  }

  @Test
  public void testBodyReceiveFailViaErrorInOnContent() {

    byte[] testByteArray = createTestByteArray();
    byte[][] chunks = createTestChunks(testByteArray, HttpClientTest.BUFFER_SIZE);

    Single<HttpResponse> single = this.httpClient.send(HttpRequest.builder().method(HttpMethod.GET)
        .url(HttpClientTest.uriForPath(HttpClientTestServlet.PATH_TEST_WITH_BODY))
        .body(Optional.of(new ChunkedAsyncContentProvider(chunks, Optional.empty(), false)))
        .build());

    String errorMessage = "Error with throw exception";

    try (HttpResponse httpResponse = single.blockingGet()) {
      Single<byte[]> bodySingle = Single.create((emitter) -> {
        httpResponse.getBody().onSuccess(() -> emitter.onSuccess(new byte[0]))
            .onContent((content, callback) -> {
              throw new RuntimeException(errorMessage);
            })
            .onError((error) -> emitter.onError(error));
      });

      try {
        byte[] body = bodySingle.blockingGet();
        Assert.fail(
            "Exception should have been thrown, instead body arrived: " + Arrays.toString(body));
      } catch (RuntimeException e) {
        Assert.assertEquals(errorMessage, e.getMessage());
      }

    }
  }

  @Test
  public void testBodySendFail() {

    byte[] testByteArray = createTestByteArray();
    byte[][] chunks = createTestChunks(testByteArray, HttpClientTest.BUFFER_SIZE);

    Single<HttpResponse> single = this.httpClient.send(HttpRequest.builder().method(HttpMethod.GET)
        .url(HttpClientTest.uriForPath(HttpClientTestServlet.PATH_TEST_WITH_BODY))
        .body(Optional
            .of(new ChunkedAsyncContentProvider(chunks, Optional.empty(), true)))
        .build());

    try (HttpResponse response = single.blockingGet()) {
      Assert.fail("Exception should have been thrown");
    } catch (BodySendException e) {
      // Successful test
    }

  }

  @Test
  public void testConnectionClosedAfterHeaders() {
    Single<HttpResponse> single =
        this.httpClient.send(HttpRequest.builder().method(HttpMethod.GET)
            .url(
                HttpClientTest
                    .uriForPath(
                        HttpClientTestServlet.PATH_TEST_CONNECTION_ERROR_DURING_RESPONSE_BODY))
            .build());

    try (HttpResponse httpResponse = single.blockingGet()) {
      Single<byte[]> bodySingle = AsyncContentUtil.readAllBytes(httpResponse.getBody());
      try {
        byte[] body = bodySingle.blockingGet();
        Assert
            .fail(
                "Exception should have been thrown, instead body with length came: "
                    + body.length);
      } catch (RuntimeException e) {
        Assert.assertNotNull(e);
      }

    }
  }

  @Test
  public void testConnectionClosedBeforeHeaders() {

    Single<HttpResponse> single = this.httpClient.send(HttpRequest.builder().method(HttpMethod.GET)
        .url(HttpClientTest.uriForPath(HttpClientTestServlet.PATH_TEST_CONNECTION_ERROR_ON_ARRIVE))
        .build());

    try (HttpResponse httpResponse = single.blockingGet()) {
      Assert.fail("Exception should have been thrown");
    } catch (RuntimeException e) {
      Assert.assertNotNull(e);
    }
  }

  @Test
  public void testFailureOnResponseCallback() {
    byte[] testByteArray = createTestByteArray();
    byte[][] chunks = createTestChunks(testByteArray, HttpClientTest.BUFFER_SIZE);

    Single<HttpResponse> single = this.httpClient.send(HttpRequest.builder().method(HttpMethod.GET)
        .url(HttpClientTest.uriForPath(HttpClientTestServlet.PATH_TEST_WITH_BODY))
        .body(Optional
            .of(new ChunkedAsyncContentProvider(chunks, Optional.empty(), false)))
        .build());

    try (HttpResponse response = single.blockingGet()) {
      Assert.assertEquals(Optional.of((long) testByteArray.length),
          response.getBody().getContentLength());

      Single<Object> singleBody = Single.create((emitter) -> {
        response.getBody().onError(emitter::onError)
            .onSuccess(() -> emitter.onSuccess(new Object()))
            .onContent((content, callback) -> callback.failed(new RuntimeException("hehe")));
      });

      try {
        Object result = singleBody.blockingGet();
        Assert.fail("Exception should have been thrown, but arrived " + result);
      } catch (RuntimeException e) {
        Assert.assertNotNull(e);
      }
    }
  }

  @Test
  public void testFormUrlencoded() {
    String testUnicodeString = "d \u0171\"";

    Map<String, Collection<String>> parameters = new HashMap<>();
    parameters.put("d", Arrays.asList(testUnicodeString));

    Map<String, String> headers = new HashMap<>();
    headers.put("Accept-Charset", "utf-8");

    Single<HttpResponse> single = this.httpClient.send(HttpRequest.builder().method(HttpMethod.POST)
        .url(HttpClientTest.uriForPath(HttpClientTestServlet.PATH_TEST_FORM_URL_ENCODED)
            + "?a=1&b=2&c=3&c=4")
        .headers(headers)
        .body(Optional.of(new FormUrlencodedAsyncContentProvider(parameters)))
        .build());

    try (HttpResponse response = single.blockingGet()) {
      Single<byte[]> body = AsyncContentUtil.readAllBytes(response.getBody());
      String jsonString = new String(body.blockingGet(), StandardCharsets.UTF_8);
      JSONObject jsonObject = new JSONObject(jsonString);

      Assert.assertEquals(testUnicodeString, jsonObject.getJSONArray("d").get(0));
    }
  }

  @Test
  public void testNoBody() {

    Single<HttpResponse> single = this.httpClient.send(HttpRequest.builder().method(HttpMethod.GET)
        .url(HttpClientTest.uriForPath(HttpClientTestServlet.PATH_TEST_WITH_NO_BODY)
            + "?a=1&b=2&c=3&c=4")
        .build());

    try (HttpResponse response = single.blockingGet()) {
      Assert.assertEquals(Optional.empty(), response.getBody().getContentLength());
      Assert.assertEquals(Optional.empty(), response.getBody().getContentType());
      Assert.assertArrayEquals(new byte[0],
          AsyncContentUtil.readAllBytes(response.getBody()).blockingGet());

      Assert.assertEquals("1", response.getHeaders().get("a"));
      Assert.assertEquals("2", response.getHeaders().get("b"));
      Assert.assertEquals("3", response.getHeaders().get("c_0"));
      Assert.assertEquals("4", response.getHeaders().get("c_1"));
    }
  }

  @Test
  @Ignore
  public void testUnderStress() {
    Runnable[] tests = new Runnable[] {
        this::testBody, this::testBodyReceiveFailViaCallback,
        this::testBodyReceiveFailViaErrorInOnContent, this::testBodySendFail,
        this::testFormUrlencoded, this::testNoBody, this::testConnectionClosedBeforeHeaders,
        this::testConnectionClosedAfterHeaders
    };

    final int threadNum = 10;
    ExecutorService executor = Executors.newFixedThreadPool(threadNum);

    Random r = new Random();

    final int testRunCount = 100000;
    AtomicInteger remainingTestCountToFinish = new AtomicInteger(testRunCount);

    for (int i = 0; i < testRunCount; i++) {
      executor.execute(() -> {
        try {
          tests[r.nextInt(tests.length)].run();
        } catch (Throwable e) {
          HttpClientTest.LOGGER.log(Level.SEVERE, "Error during executing test", e);
        } finally {
          int remaining = remainingTestCountToFinish.decrementAndGet();
          final int loggingInterval = 1000;
          if (remaining % loggingInterval == 0) {
            HttpClientTest.LOGGER.log(Level.INFO, "Remaining test count to finish: " + remaining);
          }
        }
      });
    }

    try {
      executor.shutdown();
      final int timeoutOfLoadTest = 15;
      boolean terminated = executor.awaitTermination(timeoutOfLoadTest, TimeUnit.MINUTES);
      Assert.assertTrue("Tests did not finish in timeout", terminated);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
    HttpClientTest.LOGGER.info("Deadlock test done");
  }
}
