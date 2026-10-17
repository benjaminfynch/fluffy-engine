package com.interview.assignment.lambda;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification.S3BucketEntity;
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification.S3Entity;
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification.S3EventNotificationRecord;
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification.S3ObjectEntity;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.interview.assignment.config.LambdaModule;
import com.interview.assignment.service.TripProcessingService;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

class TripFileHandlerTest {
  private final Context context = mock(Context.class);

  @BeforeEach
  void setUp() {
    when(context.getAwsRequestId()).thenReturn("request-123");
  }

  @AfterEach
  void clearCorrelationId() {
    MDC.remove("correlationId");
  }

  @Test
  void delegatesProcessingUsingTheDecodedKey() throws IOException {
    var service = mock(TripProcessingService.class);
    var handler = new TripFileHandler(service);
    doAnswer(
            invocation -> {
              assertEquals("request-123", MDC.get("correlationId"));
              return null;
            })
        .when(service)
        .process("tap-input", "incoming/tap data.csv", "version-1");

    assertNull(handler.handleRequest(event(), context));

    verify(service).process("tap-input", "incoming/tap data.csv", "version-1");
  }

  @Test
  void propagatesProcessingFailures() throws IOException {
    var service = mock(TripProcessingService.class);
    var failure = new IOException("Invalid CSV");
    doThrow(failure).when(service).process("tap-input", "incoming/tap data.csv", "version-1");
    var handler = new TripFileHandler(service);

    var exception =
        assertThrows(UncheckedIOException.class, () -> handler.handleRequest(event(), context));

    assertSame(failure, exception.getCause());
  }

  private S3Event event() {
    var bucket = new S3BucketEntity("tap-input", null, "arn:aws:s3:::tap-input");
    var object = new S3ObjectEntity("incoming%2Ftap+data.csv", 13L, null, "version-1", null);
    var s3 = new S3Entity("test", bucket, object, "1.0");
    var record =
        new S3EventNotificationRecord(
            "ap-southeast-2",
            "ObjectCreated:Put",
            "aws:s3",
            "2023-01-22T13:00:00Z",
            "2.1",
            null,
            null,
            s3,
            null);
    return new S3Event(List.of(record));
  }

  @Test
  void constructsTheHandlerUsingGuice() {
    var injector = mock(Injector.class);
    when(injector.getInstance(TripProcessingService.class))
        .thenReturn(mock(TripProcessingService.class));
    try (var guice = mockStatic(Guice.class)) {
      guice.when(() -> Guice.createInjector(any(LambdaModule.class))).thenReturn(injector);
      assertNotNull(new TripFileHandler());
    }
  }
}
