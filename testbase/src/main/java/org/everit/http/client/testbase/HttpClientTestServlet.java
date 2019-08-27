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

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.eclipse.jetty.server.QuietServletException;
import org.eclipse.jetty.server.Response;
import org.everit.web.servlet.HttpServlet;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Helper servlet class that accepts the HTTP requests of the unit tests.
 */
public class HttpClientTestServlet extends HttpServlet {

  private static final int BUFFER_SIZE = 8096;

  public static final String PATH_TEST_CONNECTION_ERROR_DURING_RESPONSE_BODY =
      "/connection-error-during-response-body";

  public static final String PATH_TEST_CONNECTION_ERROR_ON_ARRIVE = "/connection-error-on-arrive";

  public static final String PATH_TEST_FORM_URL_ENCODED = "/formurl";

  public static final String PATH_TEST_WITH_BODY = "/body";

  public static final String PATH_TEST_WITH_NO_BODY = "/nobody";

  private void copyParamsToResponseHeaders(HttpServletRequest req, HttpServletResponse resp) {
    resp.setStatus(HttpServletResponse.SC_NO_CONTENT);
    Map<String, String[]> parameterMap = req.getParameterMap();
    for (Entry<String, String[]> parameterEntry : parameterMap.entrySet()) {
      String key = parameterEntry.getKey();
      String[] values = parameterEntry.getValue();
      if (values.length == 1) {
        resp.setHeader(key, values[0]);
      } else {
        for (int i = 0, n = values.length; i < n; i++) {
          resp.setHeader(key + "_" + i, values[i]);
        }
      }
    }
  }

  private void makeConnectionError(HttpServletResponse resp)
      throws IOException {

    ((Response) resp).getHttpChannel().abort(new QuietServletException("x"));
  }

  private void makeConnectionErrorDuringBodySend(HttpServletResponse resp)
      throws IOException {

    ServletOutputStream outputStream = resp.getOutputStream();
    byte[] buffer = new byte[HttpClientTestServlet.BUFFER_SIZE];
    Arrays.fill(buffer, (byte) 2);
    final int fileSizeAsBufferMultiplier = 300;
    for (int i = 0; i < fileSizeAsBufferMultiplier; i++) {
      outputStream.write(buffer);
    }
    makeConnectionError(resp);
  }

  @Override
  protected void service(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {

    resp.setCharacterEncoding("UTF-8");

    String pathInfo = req.getPathInfo();
    switch (pathInfo) {
      case PATH_TEST_WITH_NO_BODY:
        copyParamsToResponseHeaders(req, resp);
        break;
      case PATH_TEST_WITH_BODY:
        writeRequestBodyToResponse(req, resp);
        break;
      case PATH_TEST_FORM_URL_ENCODED:
        writeRequestParametersToResponseBodyAsJSON(req, resp);
        break;
      case PATH_TEST_CONNECTION_ERROR_ON_ARRIVE:
        makeConnectionError(resp);
        break;
      case PATH_TEST_CONNECTION_ERROR_DURING_RESPONSE_BODY:
        makeConnectionErrorDuringBodySend(resp);
        break;
      default:
        break;
    }
  }

  private void writeRequestBodyToResponse(HttpServletRequest req, HttpServletResponse resp) {
    try {
      byte[] body = IOUtils.toByteArray(req.getInputStream());
      resp.setContentLength(body.length);
      resp.getOutputStream().write(body);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private void writeRequestParametersToResponseBodyAsJSON(HttpServletRequest req,
      HttpServletResponse resp) throws IOException {

    JSONObject jsonObject = new JSONObject();
    Map<String, String[]> parameterMap = req.getParameterMap();
    for (Entry<String, String[]> entry : parameterMap.entrySet()) {
      jsonObject.putOnce(entry.getKey(), new JSONArray(entry.getValue()));
    }
    resp.getWriter().write(jsonObject.toString());
  }

}
