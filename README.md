everit-async-httpclient
=======================

This is a library that can be used as a wrapper around well known java based HTTP clients.

## Motivation

There are several HTTP client implementations in Java with their pros and cons. This API is a
wrapper that can be used around those libraries, so the no source code has to be changed in the
project by replacing the implementation below.

The API of this library is designed to be completely asynchronous, similar to the async API of
Jetty client.

## Features

 - All HTTP methods are supported
 - multipart/form-data requests
 - form-urlencoded requests
 - Support [rxjava][0] to ease asynchronous programming

## Implementations

[Jetty Client based implementation][1]

## Usage

### A simple GET request 

    // Instantiate one of the implementations. Do not forget to close the
    // http client when everything is done.
    HttpClient httpClient = new MyHttpClientImplementation();
    
    // Craete the request with a builder
    HttpRequest request = HttpRequest.builder().url("https://mypage.com");
    
    // Send the request asynchronously
    Single<HttpResponse> responseSingle = httpClient.send(request);
    
    responseSingle.subscribe(httpResponse -> {
      System.out.println("Status: " + httpResponse.getStatus());
      
      // Get the body. It is low level to read the body with the
      // AsyncContentProvider interface, so it is better to use the Util
      // class to read the content as a Single.
      Single<String> bodySingle = AsyncContentUtil.readString(
          httpResponse.getBody(), StandardCharsets.UTF8);
      
      bodySingle.subscribe(content -> {
        httpResponse.close();
        System.out.println(content);
      }, error -> httpResponse.close());
    });

### Creating a POST request

    HttpRequest.builder()
      .url("https://mypage.com")
      .method(HttpMethod.POST)
      .body(Optional.of(
          new ByteArrayAsyncContentProvider(
              "hello world".getBytes(),
              Optional.of(MediaType.parse("text/plain")))));

### Sending a multipart/form-data request

To create a multipart/form-data request, the easiest to pass an instance of
MultipartAsyncContentProvider as the body of the HttpRequest:

    HttpRequest.builder()
      .url("https://mypage.com")
      .method(HttpMethod.POST)
      .body(Optional.of(
          new MultipartAsyncContentProvider(
            Optional.empty(),
            new Part(...),
            new Part(...),
            new Part(...),
            ...
          ))));

**An example of creating a simple text field Part:**

    Part.createFieldPart("fieldName", "fieldValue");

**Advanced Part creation**

You may use the constructor of the _Part_ class to create more advanced parts like content read
from a file.

**Closing the Part content**

You may have realized that the Part also needs an AsyncContentProvider that should be closed
somewhen. This is done automatically once the request is sent, failed or canceled.


### AsyncContentProvider implementations

There are several implementations for AsyncContentProvider that can be used as the body of a
HttpRequest or that is provided as the body of a HttpResponse. In this library, the following
implementations are available:

 - ByteArrayAsyncContentProvider
 - InputStreamAsyncContentProvider
 - ReadableByteChannelAsyncContentProvider
 - MultipartAsyncContentProvider
 - FormUrlencodedAsyncContentProvider

### More examples

For more examples, see the unit tests of the project.

[0]: https://github.com/ReactiveX/RxJava
[1]: https://github.com/everit-org/everit-async-httpclient-jettyclient
